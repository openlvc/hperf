/*
 *   Copyright 2015 Calytrix Technologies
 *
 *   This file is part of hperf.
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
package hperf.throughput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateAmbassador;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import hperf.IDriver;
import hperf.Storage;
import hperf.TestFederate;
import hperf.Utils;
import hperf.config.Configuration;
import hperf.config.LoggingConfigurator;
import static hperf.Handles.*;

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
	private Storage storage;
	private RTIambassador rtiamb;
	private ThroughputFedAmb fedamb;

	private List<ObjectInstanceHandle> myObjects;
	private byte[] payload;

	// time factory for use if we are timestepped
	private HLAfloat64TimeFactory timeFactory;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public ThroughputDriver()
	{
		this.logger = null;         // set in configure()
		this.configuration = null;  // set in configure()
		this.storage = null;        // set in configure()
		this.rtiamb = null;         // set in execute()
		this.fedamb = null;         // set in configure()

		this.myObjects = new ArrayList<ObjectInstanceHandle>();
		this.payload = null;        // set in execute()
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
	 */
	public void execute( RTIambassador rtiamb ) throws RTIexception
	{
		this.rtiamb = rtiamb;
		this.payload = Utils.generatePayload( configuration.getPacketSize() );

		// Enable time policy if we use it
		if( configuration.isTimestepped() )
			this.enableTimePolicy();
		
		// Register test objects
		this.registerObjects();
		
		// Confirm everyone else has registered their test objects and sync up for start
		this.waitForStart();
		this.logger.info( "Starting Throughput Test" );
		this.storage.startThroughputTestTimer();
		
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
				int sentCount         = configuration.getObjectCount() + 
				                        configuration.getInteractionCount() *
				                        batchSize;
				int sentPerSecond     = (int)(sentCount / seconds);

				int eventCount        = storage.getThroughputEventCount() - sentCount;
				int receivedCount     = eventCount - lastEventCount;
				int receivedPerSecond = (int)(receivedCount / seconds);
				
				// throughput per second
				long totalbytes     = (long)receivedCount * (long)packetSize;
				String receivedmbps = Utils.getSizeStringPerSec( totalbytes / seconds, 2 );
				totalbytes          = (long)sentCount * (long)packetSize;
				String sentmbps     = Utils.getSizeStringPerSec( totalbytes / seconds, 2 );

				// log it all for the people
				String msg = "[%-6d] -- %5dms, send %10s (%5d/s) -- recv %10s (%5d/s)";
				logger.info( String.format(msg,i,duration,sentmbps,sentPerSecond,
				                           receivedmbps,receivedPerSecond) );
				
				// reset the batch variables so we can compare next time
				lastTimestamp = now;
				lastEventCount = eventCount;
			}
		}

		// Wait for everyone to finish their stuff
		this.waitForFinish();

		logger.info( "Throughput test finished" );
		logger.info( "" );
		
		// Print the report
		new ThroughputReportGenerator(configuration,storage).printReport();
	}

	/** We print out stats every so often during a run. This method determines how often.
	    For small sizes, this is every 10% of the total loop count. For larger, it's more often.
	    Users can manually specify a value from the command line if they want, in which case we
	    just return that. */
	private int getBatchSize()
	{
		if( configuration.getPrintInterval() == -1 )
		{
			// auto configure based on the loop count
    		if( configuration.getLoopCount() > 100000 )
    			return 10000;
    		else
    			return (int)Math.ceil( configuration.getLoopCount() * 0.1 );
		}
		else
		{
			// value has been specified - just use it
			return configuration.getPrintInterval();
		}
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
		AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);
		attributes.put( AC_PAYLOAD, payload );
		for( ObjectInstanceHandle testObject : myObjects )
		{
			rtiamb.updateAttributeValues( testObject, attributes, null );
			storage.recordReflect( testObject );
		}

		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(2);
		byte[] senderNameBytes = configuration.getFederateName().getBytes();
		parameters.put( PC_THROUGHPUT_SENDER, senderNameBytes );
		parameters.put( PC_THROUGHPUT_PAYLOAD, payload );
		for( int i = 0; i < configuration.getInteractionCount(); i++ )
		{
			rtiamb.sendInteraction( IC_THROUGHPUT, parameters, null );
			storage.recordInteraction( IC_THROUGHPUT, storage.getLocalFederate() );
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
		// 1. Create the test object instances that we'll use for metrics gathering.
		//    The initial attribute update happens only once we know all peers are present.
		// ------------------------------------------------------------------------------
		logger.info( "Registering ["+configuration.getObjectCount()+"] test objects" );
		for( int i = 0; i < configuration.getObjectCount(); i++ )
		{
			String objectName = configuration.getFederateName()+"-"+(i+1);
			ObjectInstanceHandle oHandle = rtiamb.registerObjectInstance( OC_TEST_OBJECT,
			                                                              objectName );

			// store our details about the object for later reference
			myObjects.add( oHandle );
			logger.debug( "  [1]: "+oHandle+", name="+objectName );
		}
		
		// ------------------------------------------------------------------------------
		// 2. Send an initial update for all the objects so that other federates
		//    can associate them with us
		// ------------------------------------------------------------------------------
		logger.info( "Send initial attribute updates" );
		AttributeHandleValueMap values = rtiamb.getAttributeHandleValueMapFactory().create(1);
		values.put( AC_CREATOR, configuration.getFederateName().getBytes() );
		for( ObjectInstanceHandle myObject : myObjects )
		{
			rtiamb.updateAttributeValues( myObject, values, null );
			storage.recordDiscover( myObject, storage.getLocalFederate() );
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
		while( fedamb.achievedSyncPoints.contains("START_THROUGHPUT_TEST") == false )
			tickOrSleep( 20 );
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
	///////////////////////////////////// Ready to Finish //////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * For each of our peers, loop through and see if we have received all of the messages we
	 * are expecting. When we've accounted for all expected messages in all peers we try to
	 * synchronize on the "finishing" point. We do this because in Portico, sync point messages
	 * are delivered OOB, so they effectively queue jump, meaning we might exit before all
	 * incoming reflections/interactions are processed and this would give inaccurate timings.
	 */
	private void waitForFinish() throws RTIexception
	{
		logger.info( "Finished sending messages, waiting for everyone else" );

		// number of events expected per federate
		int loopCount = configuration.getLoopCount();
		int expectedPerFederate = configuration.getObjectCount() +                  /* discovers */
                                 (configuration.getObjectCount() * loopCount) +     /* reflects */
                                 (configuration.getInteractionCount() * loopCount); /* interactions */
		
		// get a list of all peers - integer is how many messages we've received
		Collection<TestFederate> notfinished = new ConcurrentLinkedQueue<TestFederate>( storage.getPeers() );
		notfinished.remove( storage.getLocalFederate() );

		long nextScheduledReport = 0;  // earliest time we should print waiting summary
		while( notfinished.isEmpty() == false )
		{
			///////////////////////////////////////////////////////////////
			// check each federate to see if we have all expected events //
			///////////////////////////////////////////////////////////////
			for( TestFederate federate : notfinished )
			{
				int eventCount = federate.getEventCount();
				boolean finished = eventCount >= expectedPerFederate;
				if( finished )
				{
					logger.info( "Received all updates for ["+federate+"]: "+federate.getEventCount() );
					notfinished.remove( federate );
				}
			}

			////////////////////////////////////////////////////////////////
			// print a summary of who we're waiting for every two seconds //
			////////////////////////////////////////////////////////////////
			if( System.currentTimeMillis() > nextScheduledReport )
			{
				logger.info( "Waiting for "+notfinished.size()+" federates" );
				for( TestFederate federate : notfinished )
				{
					int remaining = expectedPerFederate - federate.getEventCount();
					logger.info( "  - "+federate.getFederateName()+": "+remaining+" events to go"+
					             " -- "+(int)(remaining/expectedPerFederate)+"%" );
				}

				nextScheduledReport = System.currentTimeMillis() + 5000;
			}

			// process a bit
			tickOrSleep( 100 );
		}
		
		storage.stopThroughputTestTimer();
		logger.info( "All finished - synchronizing" );
		rtiamb.synchronizationPointAchieved( "FINISH_THROUGHPUT_TEST" );
		while( fedamb.achievedSyncPoints.contains("FINISH_THROUGHPUT_TEST") == false )
			tickOrSleep( 500 );
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// IDriver Methods /////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void configure( Configuration configuration, Storage storage )
	{
		this.configuration = configuration;
		this.logger = LoggingConfigurator.getLogger( configuration.getFederateName() );
		this.storage = storage;
		this.fedamb = new ThroughputFedAmb( configuration, storage );
	}

	@Override
	public void printWelcomeMessage()
	{
		int loops = configuration.getLoopCount();
		int objects = configuration.getObjectCount();
		int interactions = configuration.getInteractionCount();
		int messages = objects+interactions;
		int packetSize = configuration.getPacketSize();
		long sendSize = (long)objects * (long)loops * (long)packetSize +
		                (long)interactions * (long)loops * (long)packetSize;
		long recvSize = sendSize * (long)configuration.getPeers().size();

		logger.info( " ===================================" );
		logger.info( " =     Running Throughput Test     =" );
		logger.info( " ===================================" );
		logger.info( "" );
		logger.info( "  Min Messsage Size = "+Utils.getSizeString(packetSize) );
		logger.info( "         Loop Count = "+configuration.getLoopCount() );
		logger.info( "       Object Count = "+configuration.getObjectCount() );
		logger.info( "  Interaction Count = "+configuration.getInteractionCount() );
		logger.info( "  Messages Per Loop = "+messages+" ("+objects+" updates, "+interactions+" interactions)" );
		logger.info( "     Total Messages = "+messages * loops );
		logger.info( "              Peers = "+configuration.getPeers().size() );
		logger.info( "    Total Send Size = "+Utils.getSizeString(sendSize,2).trim() );
		logger.info( "    Total Revc Size = "+Utils.getSizeString(recvSize,2).trim() );
		logger.info( "" );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String getName()
	{
		return "Throughput Test";
	}

	@Override
	public FederateAmbassador getFederateAmbassador()
	{
		return this.fedamb;
	}

	/**
	 * Depending on whether we are using the immediate callback processor, or are in a 
	 * ticking mode, either sleep or tick for at least the given number of millis
	 */
	private void tickOrSleep( long millis ) throws RTIexception
	{
		if( configuration.isImmediateCallback() )
			Utils.sleep( millis );
		else
			rtiamb.evokeMultipleCallbacks( (millis/1000.0), (millis/1000.0) );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
