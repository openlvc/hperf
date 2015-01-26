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
import wantest.IDriver;
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
import hla.rti1516e.time.HLAfloat64TimeFactory;

public class ThroughputDriver implements IDriver
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

	// time factory for use if we are timestepped
	private HLAfloat64TimeFactory timeFactory;

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
	                     FederateAmbassador fedamb,
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
		printHeader();

		// Enable time policy if we use it
		if( configuration.isTimestepped() )
			this.enableTimePolicy();
		
		// Register test objects
		this.registerObjects();
		
		// Confirm everyone else has registered their test objects and sync up for start
		this.waitForStart();
		
		//////////////////////////////////////////////////////////////////////////////
		//                                                                          //
		// Loop                                                                     //
		//                                                                          //
		// We log progress at each 10% mark for the text, so we need to know how    //
		// often that should be and somewhere to store the received count and       //
		// timestamp the last time we passed the threshold                          //
		//////////////////////////////////////////////////////////////////////////////
		int batchSize = getBatchSize();
		int packetSize = configuration.getPacketSize();
		int lastEventCount = 0;
		long lastTimestamp = System.nanoTime();

		this.logger.info( "Starting Throughput Test" );
		this.storage.startThroughputTestTimer();
		for( int i = 1; i <= configuration.getLoopCount(); i++ )
		{
			////////////////////////
			// Do the actual work //
			////////////////////////
			loop( i );

			/////////////////////////////////////////////////////
			// Log some summary information every now and then //
			/////////////////////////////////////////////////////
			// Well, that got out of hand. One line to loop, a bajillion to watch when we start/stop
			if( i % batchSize == 0 && i != 0 )
			{
				// duration
				long now = System.nanoTime();
				long duration = TimeUnit.NANOSECONDS.toMillis( now - lastTimestamp );
				double seconds = duration / 1000.0;
				
				// events total and per-second
				int eventListSize     = storage.getThroughputEvents().size();
				int receivedCount     = eventListSize - lastEventCount;
				int receivedPerSecond = (int)(receivedCount / seconds);
				int sentCount         = (configuration.getObjectCount()*2) * batchSize;
				int sentPerSecond     = (int)(sentCount / seconds);
				
				// throughput per second
				long totalbytes     = (long)receivedCount * (long)packetSize;
				String receivedmbps = Utils.getSizeStringPerSec( totalbytes / seconds, 2 );
				totalbytes          = (long)sentCount * (long)packetSize;
				String sentmbps     = Utils.getSizeStringPerSec( totalbytes / seconds, 2 );

				// log it all for the people
				String msg = "Finished loop %-7d -- %5dms, %6d received (%5d/s), %10s -- %6d sent (%5d/s), %10s";
				logger.info( String.format(msg, i, duration, receivedCount, receivedPerSecond,
				                           receivedmbps, sentCount, sentPerSecond, sentmbps) );
				
				// reset the batch variables so we can compare next time
				lastTimestamp = now;
				lastEventCount = eventListSize;
			}
		}

		// Wait for everyone to finish their stuff
		this.waitForFinish();
		this.storage.stopThroughputTestTimer();

		logger.info( "Throughput test finished" );
		logger.info( "" );
		
		// Print the report
		new ThroughputReportGenerator(configuration,storage).printReport();
	}

	/** We print out stats every so often during a run. This method determines how often.
	    For small sizes, this is every 10% of the total loop count. For larger, it's more often */
	private int getBatchSize()
	{
		if( configuration.getLoopCount() > 100000 )
			return 10000;
		else
			return (int)Math.ceil( configuration.getLoopCount() * 0.1 );
	}

	private void printHeader()
	{
		int loops = configuration.getLoopCount();
		int objects = configuration.getObjectCount();
		int packetSize = configuration.getPacketSize();
		long sendSize = (long)objects * (long)2 * (long)loops * (long)packetSize;
		long revcSize = sendSize * configuration.getPeers().size();
		logger.info( "" );
		logger.info( "  Min Messsage Size = "+Utils.getSizeString(packetSize) );
		logger.info( "         Loop Count = "+configuration.getLoopCount() );
		logger.info( "       Object Count = "+configuration.getObjectCount() );
		logger.info( "  Messages Per Loop = "+objects*2+" ("+objects+" updates, "+objects+" interactions)" );
		logger.info( "     Total Messages = "+objects*2 * loops );
		logger.info( "              Peers = "+configuration.getPeers().size() );
		logger.info( "    Total Send Size = "+Utils.getSizeString(sendSize,1) );
		logger.info( "    Total Revc Size = "+Utils.getSizeString(revcSize,1) );
		logger.info( "" );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Time Policy Methods ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void enableTimePolicy() throws RTIexception
	{
		this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

		// turn on the time policy
		rtiamb.enableTimeConstrained();
		rtiamb.enableTimeRegulation( timeFactory.makeInterval(1.0) );
		while( fedamb.timeConstrained == false || fedamb.timeRegulating == false )
			tickOrSleep( 500 );

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
			tickOrSleep( 500 );
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

			tickOrSleep( 500 );

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
		// If we use IMMEDIATE callback mode - do nothing, we'll get callbacks automatically
		// If we use EVOKED callback mode - tick away!
		// If we use TIMESTEPPING - request an advance and then wait until we get it
		if( configuration.isTimestepped() )
		{
			// Tick until we get the advance grant
			long requestedTime = fedamb.currentTime + 1;
			rtiamb.timeAdvanceRequest( timeFactory.makeTime(requestedTime) );
			while( fedamb.currentTime < requestedTime )
				tickOrSleep( configuration.getLoopWait() );
		}
		else if( configuration.isEvokedCallback() )
		{
			// Tick for at least the loopWait time, but no longer four times
			// its value. We'll only continue to be held if there are messages
			// pending that require attention, so if we go past mintime it is
			// with good cause.
			double looptime = ((double)configuration.getLoopWait()) / 1000;
			rtiamb.evokeMultipleCallbacks( looptime, looptime*4.0 );
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Ready to Finish //////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void waitForFinish() throws RTIexception
	{
		logger.info( "Finished sending messages, waiting for everyone else" );

		// Get out own collection of all our peers that we can modify
		// As we encounter one that is finished, we'll remove it. When
		// they're all gone - everyone has finished!
		//
		// Value of this map is an int array where [0] = recv count at last check, [1] = lives
		// If the recv count does not increase for a tick, a live is deducted. When a federate
		// is out of lives, we remove it from the list of federates we're waiting for
		Map<TestFederate,Integer[]> notfinished = new HashMap<TestFederate,Integer[]>();
		for( TestFederate federate : storage.getPeers() )
			notfinished.put( federate, new Integer[]{ 0,5 } );

		// For each object, we expect loopNumber events + the discover event
		int targetCount = configuration.getLoopCount() + 1;
		
		// let's see what the state of things is
		while( notfinished.isEmpty() == false )
		{
			// wait/tick for a little bit to let more events filter in
			tickOrSleep( 2000 );

			// loop through each federate to see if we have all the messages
			for( TestFederate federate : storage.getPeers() )
			{
				// assume they are finished - catch them out if they're not
				boolean federateIsFinished = true;
				for( TestObject object : federate.getObjects() )
				{
					if( object.getEventCount() != targetCount )
					{
						// their even count does not match what we expect - they're not done
						federateIsFinished = false;
						break;
					}
				}
				
				if( federateIsFinished )
				{
					logger.info( "Received all updates for ["+federate.getFederateName()+"]: "+
					             federate.getEventCount()+" (total: "+storage.getThroughputEvents().size()+")" );
					notfinished.remove( federate );
				}
				else
				{
					// check to see if we're out of lives
					Integer[] vitals = notfinished.get( federate );
					int eventCount = federate.getEventCount();
					int lastCount = notfinished.get(federate)[0];
					if( (eventCount > lastCount) == false )
						vitals[1] = vitals[1]-1;
					
					// give up on this one
					if( vitals[1] > 0 )
					{
						logger.info( "Waiting for ["+federate.getFederateName()+"]: "+
					                 federate.getEventCount()+
					                 " events ("+storage.getThroughputEvents().size()+" total events)" );
					}
					else
					{
						notfinished.remove( federate );
						logger.info( "Waited too long for ["+federate.getFederateName()+
						             "] - these events clearly aren't coming, dropped packets" );
					}
				}
			}
			
		}		
		
		logger.info( "All finished - synchronizing" );
		rtiamb.synchronizationPointAchieved( "FINISH_THROUGHPUT_TEST" );
		while( fedamb.finishedThroughputTest == false )
			tickOrSleep( 500 );
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//                                    Utility Methods                                    //
	///////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Depending on whether we are using the immediate callback processor, or are in a 
	 * ticking mode, either sleep or tick for at least the given number of millis
	 */
	private void tickOrSleep( long millis ) throws RTIexception
	{
		if( configuration.isImmediateCallback() )
			Utils.sleep( millis );
		else
			rtiamb.evokeMultipleCallbacks( (millis*1000), (millis*1000) );
	}

	public String getName()
	{
		return "Throughput Test";
	}
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
