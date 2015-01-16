/*
 *   Copyright 2014 Calytrix Technologies
 *
 *   This file is part of wantest.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of Calytrix Technologies Pty Ltd.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to Calytrix Technologies Pty Ltd.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from Calytrix Technologies Pty Ltd.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package wantest.throughput;

import static wantest.Handles.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import wantest.FederateAmbassador;
import wantest.Storage;
import wantest.TestFederate;
import wantest.TestObject;
import wantest.Utils;
import wantest.config.Configuration;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;

public class ThroughputDriver
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Configuration configuration;
	private RTIambassador rtiamb;
	private FederateAmbassador fedamb;
	private Storage storage;
	
	private Map<ObjectInstanceHandle,TestObject> myObjects;
	private byte[] payload;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public ThroughputDriver()
	{
		this.myObjects = new HashMap<ObjectInstanceHandle,TestObject>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * Perform the core execution for the Throughput Test. This method will:
	 * 
	 *    - Register all the test objects for the local federate
	 *    - Wait for all other federates to register their objects
	 *    - Synchornize with the other federates
	 *    - Loop
	 *        -- Send an update for each test object
	 *        -- Send a set of interactions (number is the same as there are test objects)
	 *    - Wait for everyone to synchronize on the finishing sync point
	 * 
	 * Once complete, all results will be stored in the provided {@link Storage} object.
	 * 
	 * @param configuration
	 * @param rtiamb
	 * @param fedamb
	 * @param storage
	 * @throws RTIexception
	 */
	public void execute( Configuration configuration,
	                     RTIambassador rtiamb,
	                     FederateAmbassador fedamb, // TODO do we need this??
	                     Storage storage )
		throws RTIexception
	{
		logger = Logger.getLogger( "wantest" );
		this.configuration = configuration;
		this.rtiamb = rtiamb;
		this.fedamb = fedamb;
		this.storage = storage;
		this.payload = Utils.generatePayload( configuration.getPacketSize() );
		
		logger.info( " ===================================" );
		logger.info( " =     Running Throughput Test     =" );
		logger.info( " ===================================" );
		String sizeString = Utils.getSizeString( configuration.getPacketSize() );
		logger.info( "Minimum message size="+sizeString );

		// Register test objects
		this.registerObjects();
		
		// Confirm everyone else has registered their test objects and sync up for start
		this.waitForStart();
		
		//
		// Loop
		//
		// We log progress at each 10% mark for the text, so we need to know how
		// often that should be and somewhere to store the received count and
		// timestamp the last time we passed the threshold
		int batchSize = getBatchSize();
		int lastEventCount = 0;
		long lastTimestamp = System.nanoTime();

		this.logger.info( "Starting Throughput Test" );
		this.storage.startThroughputTestTimer();
		for( int i = 0; i < configuration.getLoopCount(); i++ )
		{
			// Do the actual work
			loop( i+1 );

			// Log some information!
			if( i != 0 && i % batchSize == 0 )
			{
				// duration
				long now = System.nanoTime();
				long duration = TimeUnit.NANOSECONDS.toMillis( now - lastTimestamp );
				// events total and per-second
				int eventCount = storage.getThroughputEvents().size();
				int eventsReceived = eventCount - lastEventCount;
				int eventsPerSecond = (int)(eventsReceived / (duration/1000.0));
				// throughput per second
				int totalbytes = eventsReceived*configuration.getPacketSize();
				String mbps = Utils.getSizeString( totalbytes / (duration/1000.0), 2 );

				String msg = "Finished loop %-7d -- %dms, %d events received (%d/s), %s";
				logger.info( String.format(msg, i, duration, eventsReceived, eventsPerSecond, mbps) );
				
				// reset the batch variables
				lastTimestamp = now;
				lastEventCount = eventCount;
			}
		}

		// Wait for everyone to finish their stuff
		this.waitForFinish();
		this.storage.stopThroughputTestTimer();

		logger.info( "Throughput test finished" );
		logger.info( "" );
	}

	/** We print out stats every so often during a run. This method determines how often.
	    For small sizes, this is every 10% of the total loop count. For larger, it's more often */
	private int getBatchSize()
	{
		if( configuration.getLoopCount() > 100000 )
			return 10000;
		else
			return (int)(configuration.getLoopCount() * 0.1);
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Register Objects /////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void registerObjects() throws RTIexception
	{
		// ------------------------------------------------------------------------------
		// 1. Create the TestObject instances that we'll use for metrics gathering.
		//    The initial attribute update happens only once we know all peers are present.
		// ------------------------------------------------------------------------------
		logger.info( "Registering ["+configuration.getObjectCount()+"] test objects" );
		for( int i = 0; i < configuration.getObjectCount(); i++ )
		{
			// name of the objects in the form "federate-X" where X is the
			// sequence number of the object for the local federate
			String objectName = configuration.getFederateName()+"-"+(i+1);
			ObjectInstanceHandle oHandle = rtiamb.registerObjectInstance( OC_TEST_OBJECT,
			                                                              objectName );

			// store our details about the object for later reference
			TestObject testObject = new TestObject( oHandle, objectName );
			this.myObjects.put( oHandle, testObject );
			logger.debug( "  [1]: "+oHandle+", name="+objectName );
		}
		
		// ------------------------------------------------------------------------------
		// 2. Send an initial update for all the objects so that other federates
		//    can associate them with us
		// ------------------------------------------------------------------------------
		AttributeHandleValueMap values = rtiamb.getAttributeHandleValueMapFactory().create( 2 );
		logger.info( "Send initial attribute updates" );
		for( TestObject testObject : myObjects.values() )
		{
			values.clear();
			values.put( AC_CREATOR, configuration.getFederateName().getBytes() );
			values.put( AC_LAST_UPDATED, Utils.longToBytes(System.currentTimeMillis()) );
			rtiamb.updateAttributeValues( testObject.getHandle(), values, null );
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////// Ready to Start //////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void waitForStart() throws RTIexception
	{
		// NOTE Sync-point messages are delivered OOB in Portico, which means if the point
		//      is blindly achieved after we register our objects, we may get the sync'd
		//      notice before processing all the discovery calls. This is why we check first.
		
		// wait to discover all objects from our peers
		waitForAllDiscoveries();
		
		// achieve the ready-to-start sync point
		rtiamb.synchronizationPointAchieved( "START_THROUGHPUT_TEST" );
		
		// wait for everyone to do the same
		while( fedamb.startThroughputTest == false )
			rtiamb.evokeMultipleCallbacks( 0.1, 1.0 );
	}

	/**
	 * We registered a certain number of objects based on our configuration. We expect all
	 * other federates to register the same number. Check each of the {@link TestFederate}
	 * objects to see if they have all registered that number. If not, tick until they have.
	 * 
	 * An object is only associated with a test federate after its initial update (which
	 * includes the name of the creating federate as a parameter so we can make the
	 * association). 
	 */
	private void waitForAllDiscoveries() throws RTIexception
	{
		///////////////////////////////////////////////////////////
		// wait until we have an initial update from all remotes //
		///////////////////////////////////////////////////////////
		logger.info( "Wait for all peers to update their test objects" );
		boolean allObjectsReady = true;
		do
		{
			for( TestFederate federate : storage.getPeers() )
			{
				if( federate.getObjectCount() > configuration.getObjectCount() )
				{
					allObjectsReady = false;
					break;
				}
				else
				{
					allObjectsReady = true;
				}		
			}

			rtiamb.evokeMultipleCallbacks( 1.0, 1.0 );

		} while( allObjectsReady == false );

	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////// Loop ///////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method performs the main portion of the test. For each loop, it will send out
	 * attribute value updates for each of the objects that it is managing
	 */
	private void loop( int loopNumber ) throws RTIexception
	{
		//////////////////////////////////////////////
		// send out an update for all local objects //
		//////////////////////////////////////////////
		byte[] senderNameBytes = configuration.getFederateName().getBytes();
		AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create( 2 );
		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create( 2 );
		for( TestObject testObject : myObjects.values() )
		{
			attributes.clear();
			attributes.put( AC_LAST_UPDATED, Utils.longToBytes(System.currentTimeMillis()) );
			attributes.put( AC_PAYLOAD, payload );
			rtiamb.updateAttributeValues( testObject.getHandle(), attributes, null );
			logger.debug( "  (update) Sent update for object "+testObject.getName() );
			
			parameters.clear();
			parameters.put( PC_THROUGHPUT_SENDER, senderNameBytes );
			parameters.put( PC_THROUGHPUT_PAYLOAD, payload );
			rtiamb.sendInteraction( IC_THROUGHPUT, parameters, null );
			logger.debug( "  (interaction) Sent interaction" );
		}


		////////////////////////////////////////////////////
		// give some time over to process incoming events //
		////////////////////////////////////////////////////
		// Tick for at least the loopWait time, but no longer four times
		// its value. This should give us enough time to process all the
		// events in the queue, while giving the caller control over the
		// block time when there is no work to do
		double looptime = ((double)configuration.getLoopWait()) / 1000;
		rtiamb.evokeMultipleCallbacks( looptime, looptime*4.0 );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Ready to Finish //////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void waitForFinish() throws RTIexception
	{
		logger.info( "Finished sending messages, waiting for everyone else" );
		
		rtiamb.synchronizationPointAchieved( "FINISH_THROUGHPUT_TEST" );
		while( fedamb.finishedThroughputTest == false )
		{
			rtiamb.evokeMultipleCallbacks( 0.5, 0.5 );
		}
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
