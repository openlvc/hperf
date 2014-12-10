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
package wantest.federate;

import static wantest.federate.Handles.*;
import wantest.config.Configuration;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Federate
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	protected Logger logger;
	private Configuration configuration;
	private RTIambassador rtiamb;
	private TestFederateAmbassador fedamb;

	// runtime object data
	//private ObjectInstanceHandle localFederate;
	private Map<ObjectInstanceHandle,TestObject> localObjects;
	private byte[] fatBuffer; // used to push sizes of messages up to a minimum size

	// report details
	private Storage storage;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Federate()
	{
		this.localObjects = new HashMap<ObjectInstanceHandle,TestObject>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void runFederate( String[] args ) throws Exception
	{
		initializeLogging();
		
		this.configuration = new Configuration();
		this.configuration.loadCommandLine( args );
		this.configuration.loadConfigurationFile();

		// Stuff a basic buffer full of some data we can verify on the other side
		// The size of this is set in configuration and we use it to ensure that
		// all messages are of at least a certain size. In reality, the size is a
		// little bigger as we send some additional attributes/parameters, but we
		// can't have it all with perfectly sized updates now can we!
		int bytes = configuration.getPacketSize();
		this.fatBuffer = new byte[bytes];
		for( int i = 0; i < bytes; i++ )
			this.fatBuffer[i] = (byte)(i % 10);
		
		// Set up our central data storage stuff - the fedamb will need it
		this.storage = new Storage( logger, configuration );
		String sizeString = Utils.getSizeString( configuration.getPacketSize() );
		logger.info( "Minimum message size="+sizeString );

		// Create and Join
		this.createAndJoinFederation();
		
		// Announce the exit sync-point so we can get it out of the way
		this.announceExitSyncPoint();

		// Publish and Subscribe
		this.publishAndSubscribe();
		
		// Register our objects
		this.registerObjects();
		
		// Wait until everyone else turns up
		this.waitForPeers();
		
		// Do our thing
		this.storage.startTimer();
		for( int i = 0; i < configuration.getLoopCount(); i++ )
		{
			loop( i+1 );
		}
		
		// wait for the exit sync point
		this.waitForExitSyncPoint();
		this.storage.stopTimer();
		
		// Log summary information
		this.storage.printReport();
		
		// print the queue size for the federate (damn well hope it is 0!) before we jump ship
		printLrcQueueSize();

		// Get out of here
		this.resignAndDestroy();
	}

	/**
	 * Set up a default logger with a basic logging pattern.
	 */
	private void initializeLogging()
	{
		this.logger = Logger.getLogger( "wantest" ); 
		this.logger.setLevel( Level.INFO );

		// create the appender
		PatternLayout layout = new PatternLayout( "%-5p [%t] %c: %x%m%n" );
		ConsoleAppender appender = new ConsoleAppender( layout, ConsoleAppender.SYSTEM_OUT );
		appender.setThreshold( Level.TRACE ); // output restricted at logger level, not appender

		// attach the appender		
		logger.addAppender( appender );
	}
	
	private void printLrcQueueSize()
	{
		int size = ((org.portico.impl.hla1516e.Rti1516eAmbassador)rtiamb).getHelper().getState().getQueue().getSize();
		logger.info( "LRC Queue has ["+size+"] unprocessed messages" );
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// Create and Join Federation ////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void createAndJoinFederation() throws RTIexception
	{
		/////////////////////////////////////////////////
		// 1 & 2. create the RTIambassador and Connect //
		/////////////////////////////////////////////////
		logger.info( "Creating RTIambassador" );
		this.rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		
		// connect
		logger.info( "Connecting..." );
		fedamb = new TestFederateAmbassador( this.storage, this.configuration );
		rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

		//////////////////////////////
		// 3. create the federation //
		//////////////////////////////
		logger.info( "Creating Federation..." );
		// We attempt to create a new federation with the first three of the
		// restaurant FOM modules covering processes, food and drink
		try
		{
			URL[] modules = new URL[]{
			    (new File("config/testfom.fed")).toURI().toURL()
			};
			
			rtiamb.createFederationExecution( configuration.getFederationName(), modules );
			logger.info( "Created Federation ["+configuration.getFederationName()+"]" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			logger.warn( "Didn't create federation, it already existed" );
		}
		catch( MalformedURLException urle )
		{
			logger.error( "Exception loading one of the FOM modules: " + urle.getMessage(), urle );
			throw new RuntimeException( urle );
		}

		////////////////////////////
		// 4. join the federation //
		////////////////////////////
		rtiamb.joinFederationExecution( configuration.getFederateName(),
		                                configuration.getFederateName(),
		                                configuration.getFederationName() );

		logger.info( "Joined Federation as " + configuration.getFederateName() );

		////////////////////////
		// 5. enable callback //
		////////////////////////
		// because REASONS, le sigh
		rtiamb.enableCallbacks();
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////// Publish and Subscribe ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void publishAndSubscribe() throws RTIexception
	{
		// Cache up all the handles
		OC_TEST_FEDERATE  = rtiamb.getObjectClassHandle( "HLAobjectRoot.TestFederate" );
		ATT_FEDERATE_NAME = rtiamb.getAttributeHandle( OC_TEST_FEDERATE, "federateName" );

		OC_TEST_OBJECT   = rtiamb.getObjectClassHandle( "HLAobjectRoot.TestObject" );
		ATT_LAST_UPDATED = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "lastUpdated" );
		ATT_CREATOR_NAME = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "creatingFederate" );
		ATT_BYTE_BUFFER  = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "byteBuffer" );
		
		IC_TEST_INTERACTION = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.TestInteraction" );
		PRM_SENDING_FED     = rtiamb.getParameterHandle( IC_TEST_INTERACTION, "sendingFederate" );
		PRM_SEND_TIME       = rtiamb.getParameterHandle( IC_TEST_INTERACTION, "sendTime" );
		PRM_BYTE_BUFFER     = rtiamb.getParameterHandle( IC_TEST_INTERACTION, "byteBuffer" );
		
		// Publish and Subscribe
		// Class: TestFederate
		AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
		attributes.add( ATT_FEDERATE_NAME );
		rtiamb.publishObjectClassAttributes( OC_TEST_FEDERATE, attributes );
		rtiamb.subscribeObjectClassAttributes( OC_TEST_FEDERATE, attributes );
		
		// Class: TestObject
		attributes.clear();
		attributes.add( ATT_CREATOR_NAME );
		attributes.add( ATT_LAST_UPDATED );
		attributes.add( ATT_BYTE_BUFFER );
		rtiamb.publishObjectClassAttributes( OC_TEST_OBJECT, attributes );
		rtiamb.subscribeObjectClassAttributes( OC_TEST_OBJECT, attributes );

		// Class: TestInteraction
		rtiamb.publishInteractionClass( IC_TEST_INTERACTION );
		rtiamb.subscribeInteractionClass( IC_TEST_INTERACTION );

		logger.info( "Publish and Subscribe complete" );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// Register Objects /////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void registerObjects() throws RTIexception
	{
		// 1. Register the object that represents this federate.
		//    Working around the lack of MOM support in the 1516e interface at the moment.
		//    We get the federate name from the object name
		logger.info( "Registering HLAobjectRoot.TestFederate object for local federate" );
		rtiamb.registerObjectInstance( OC_TEST_FEDERATE, configuration.getFederateName() );

		// 2. Create the TestObject instances that we'll use for metrics gathering.
		//    The initial attribute update happens only once we know all peers are present.
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
			this.localObjects.put( oHandle, testObject );
			logger.debug( "  [1]: "+oHandle+", name="+objectName );
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Test Execution Loop Methods ////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method ticks the RTI until we've received word that all federates listed
	 * as peers are present. At this point it then sends an initial update for each
	 * of its local test objects, and blocks again until it has received an initial
	 * update for all test objects in all peers. 
	 * 
	 * NOTE: An assumption is made about the number of test objects a remote federate
	 *       is going to publish - it is assumed that this is the same number as the
	 *       local federate is publishing.
	 */
	private void waitForPeers() throws RTIexception
	{
		///////////////////////////////////////////
		// wait for all the federates to connect //
		///////////////////////////////////////////
		logger.info( "Waiting for peers: "+configuration.getPeers() );
		List<String> peers = new ArrayList<String>( configuration.getPeers() );
		
		while( peers.isEmpty() == false )
		{
			for( String peer : storage.peers.keySet() )
			{
				if( peers.contains(peer) )
				{
					peers.remove( peer );
					logger.debug( "  ... found "+peer );
				}
			}
			
			// sleep for a bit and then hand our time over to the RTI
			//try{ Thread.sleep( 1000 ); } catch( InterruptedException ie ){ return; }
			rtiamb.evokeMultipleCallbacks( 1.0, 1.0 );
		}

		logger.info( "All federates present - let's do this thing!" );

		////////////////////////////////////////////////
		// send an initial update for all our objects //
		////////////////////////////////////////////////
		AttributeHandleValueMap values = rtiamb.getAttributeHandleValueMapFactory().create( 2 );
		logger.info( "Send initial attribute updates" );
		for( TestObject testObject : localObjects.values() )
		{
			values.clear();
			values.put( ATT_LAST_UPDATED, (""+System.currentTimeMillis()).getBytes() );
			values.put( ATT_CREATOR_NAME, configuration.getFederateName().getBytes() );
			rtiamb.updateAttributeValues( testObject.getHandle(), values, null );
		}

		///////////////////////////////////////////////////////////
		// wait until we have an initial update from all remotes //
		///////////////////////////////////////////////////////////
		logger.info( "Wait for all peers to update their test objects" );
		boolean allObjectsReady = true;
		do
		{
			for( TestFederate federate : storage.peers.values() )
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

	/**
	 * This method performs the main portion of the test. For each loop, it will send out
	 * attribute value updates for each of the objects that it is managing
	 */
	private void loop( int loopNumber ) throws RTIexception
	{
		logger.info( "Processing loop ["+loopNumber+"]" );
		
		//////////////////////////////////////////////
		// send out an update for all local objects //
		//////////////////////////////////////////////
		AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create( 1 );
		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create( 1 );
		for( TestObject testObject : localObjects.values() )
		{
			attributes.clear();
			attributes.put( ATT_CREATOR_NAME, configuration.getFederateName().getBytes() );
			attributes.put( ATT_BYTE_BUFFER, this.fatBuffer );
			attributes.put( ATT_LAST_UPDATED, (""+System.currentTimeMillis()).getBytes() );
			rtiamb.updateAttributeValues( testObject.getHandle(), attributes, null );
			logger.debug( "  (update) Sent update for object "+testObject.getName() );
			
			parameters.clear();
			parameters.put( PRM_SENDING_FED, configuration.getFederateName().getBytes() );
			parameters.put( PRM_BYTE_BUFFER, this.fatBuffer );
			parameters.put( PRM_SEND_TIME, (""+System.currentTimeMillis()).getBytes() );
			rtiamb.sendInteraction( IC_TEST_INTERACTION, parameters, new byte[]{} );
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

	/**
	 * This method will wait for the federation to synchronize on an "exit point" before
	 * shutting down. This is done to allow faster finishing federates the opportunity to
	 * receive the inbound messages from other slower federates. As it currently stands,
	 * if a fast federate finishes, it will complain that it is missing a large number of
	 * expected events. These events are those that it isn't receiving because it completed
	 * quickly and resigned before they could be sent!
	 * 
	 * Putting in a dedicated shutdown synchronization will allow all federates to ensure
	 * they have finished sending, and will mean that any reported loss of packets will be
	 * due to actual loss, not this false positive.
	 * 
	 * While we could use time-stepping, or sync points on each loop, this would have the
	 * effect of slowing down the whole federation to the pace of its slowest participant.
	 * This would not be a realistic simulation of what we are likely to see in the wild
	 * with "real simulations" (lol).
	 */
	private void waitForExitSyncPoint() throws RTIexception
	{
		rtiamb.synchronizationPointAchieved( "READY_TO_RESIGN" );
		
		logger.info( "Waiting for the other federates to finish up" );
		while( this.storage.readyToResign == false )
			rtiamb.evokeMultipleCallbacks( 0.1, 1.0 );
		
		logger.info( "Everybody is ready to resign" );
	}
	
	private void announceExitSyncPoint()
	{
		try
		{
			rtiamb.registerFederationSynchronizationPoint( "READY_TO_RESIGN", new byte[]{} );
		}
		catch( Exception e )
		{
			logger.warn( "Exception while registering exit sync point: "+e.getMessage() );
		}
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////// Resign and Destroy Federation ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void resignAndDestroy() throws RTIexception
	{
		////////////////////////////////////
		// 12. resign from the federation //
		////////////////////////////////////
		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		logger.info( "Resigned from Federation" );

		////////////////////////////////////////
		// 13. try and destroy the federation //
		////////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		//       remain. in that case we'll leave it for them to clean up
		try
		{
			rtiamb.destroyFederationExecution( "ExampleFederation" );
			logger.info( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			logger.info( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			logger.info( "Didn't destroy federation, federates still joined" );
		}

	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
