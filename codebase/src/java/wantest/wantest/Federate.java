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
package wantest;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.*;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import static wantest.Handles.*;
import wantest.config.Configuration;
import wantest.latency.LatencyDriver;
import wantest.latency.LatencyReportGenerator;
import wantest.throughput.ThroughputDriver;
import wantest.throughput.ThroughputReportGenerator;

public class Federate
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
	
	private ThroughputDriver throughputDriver;
	private LatencyDriver latencyDriver;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Federate( String[] args )
	{
		// logging and configuration
		initializeLogging();
		this.configuration = new Configuration();
		this.configuration.loadCommandLine( args );

		this.rtiamb = null; // created during createAndJoinFederate()
		this.fedamb = null; // created during createAndJoinFederate()
		this.storage = new Storage();
		this.throughputDriver = new ThroughputDriver();
		this.latencyDriver = new LatencyDriver();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	// main test federate execution process
	public void execute() throws Exception
	{
		// Create and Join the federation
		this.createAndJoinFederation();

		// Publish and Subscribe
		this.publishAndSubscribe();
		
		// Register object representing federate
		this.registerFederateObject();

		// Wait until everyone else turns up
		this.waitForPeers();

		// Announce all the sync points up front -- life is just easier this way
		this.announceSyncPoints();
		
		/////////////////////////
		// Main test execution //
		/////////////////////////
		// Hand off execution to the specific test executors
		if( configuration.getRunThroughputTest() )
			throughputDriver.execute( configuration, rtiamb, fedamb, storage );
		
		if( configuration.getRunLatencyTest() )
			latencyDriver.execute( configuration, rtiamb, fedamb, storage );

		// Print the reports -- we do this after both tests have run
		// so that all the output is delivered together at the end of testing
		if( configuration.getRunThroughputTest() )
			new ThroughputReportGenerator(configuration,storage).printReport();
		
		if( configuration.getRunLatencyTest() )
			new LatencyReportGenerator(storage).printReport();		


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

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// Federate Lifecycle Methods ////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Create and join the federation. Note that someone else may have gotten in first, so we
	 * should expect that the federation creation process will fail.
	 */
	private void createAndJoinFederation() throws RTIexception
	{
		/////////////////////////////////////////////////
		// 1 & 2. create the RTIambassador and Connect //
		/////////////////////////////////////////////////
		logger.info( "Creating RTIambassador" );
		this.rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		
		// connect
		logger.info( "Connecting..." );
		fedamb = new FederateAmbassador( configuration, storage );
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
		rtiamb.enableAsynchronousDelivery();
	}

	/**
	 * Cache all handles and perform publication and subscription for all object and
	 * interaction types we require to complete throughput and latency tests.
	 */
	private void publishAndSubscribe() throws RTIexception
	{
		// Cache up all the handles
		OC_TEST_FEDERATE  = rtiamb.getObjectClassHandle( "TestFederate" );
		AC_FEDERATE_NAME = rtiamb.getAttributeHandle( OC_TEST_FEDERATE, "federateName" );

		OC_TEST_OBJECT  = rtiamb.getObjectClassHandle( "TestObject" );
		AC_LAST_UPDATED = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "lastUpdated" );
		AC_CREATOR      = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "creator" );
		AC_PAYLOAD      = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "payload" );
		
		IC_THROUGHPUT         = rtiamb.getInteractionClassHandle( "ThroughputInteraction" );
		PC_THROUGHPUT_SENDER  = rtiamb.getParameterHandle( IC_THROUGHPUT, "sender" );
		PC_THROUGHPUT_PAYLOAD = rtiamb.getParameterHandle( IC_THROUGHPUT, "payload" );

		IC_PING         = rtiamb.getInteractionClassHandle( "Ping" );
		PC_PING_SERIAL  = rtiamb.getParameterHandle( IC_PING, "serial" );
		PC_PING_SENDER  = rtiamb.getParameterHandle( IC_PING, "sender" );
		PC_PING_PAYLOAD = rtiamb.getParameterHandle( IC_PING, "payload" );

		IC_PING_ACK         = rtiamb.getInteractionClassHandle( "PingAck" );
		PC_PING_ACK_SERIAL  = rtiamb.getParameterHandle( IC_PING_ACK, "serial" );
		PC_PING_ACK_SENDER  = rtiamb.getParameterHandle( IC_PING_ACK, "sender" );
		PC_PING_ACK_PAYLOAD = rtiamb.getParameterHandle( IC_PING_ACK, "payload" );
		
		///////////////////////////
		// Publish and Subscribe //
		///////////////////////////
		// Class: TestFederate
		AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
		attributes.add( AC_FEDERATE_NAME );
		rtiamb.publishObjectClassAttributes( OC_TEST_FEDERATE, attributes );
		rtiamb.subscribeObjectClassAttributes( OC_TEST_FEDERATE, attributes );
		
		// Class: TestObject
		attributes.clear();
		attributes.add( AC_CREATOR );
		attributes.add( AC_LAST_UPDATED );
		attributes.add( AC_PAYLOAD );
		rtiamb.publishObjectClassAttributes( OC_TEST_OBJECT, attributes );
		rtiamb.subscribeObjectClassAttributes( OC_TEST_OBJECT, attributes );

		// Class: ThroughputInteraction
		rtiamb.publishInteractionClass( IC_THROUGHPUT );
		rtiamb.subscribeInteractionClass( IC_THROUGHPUT );

		// Class: LatencyInteraction
		rtiamb.publishInteractionClass( IC_PING );
		rtiamb.subscribeInteractionClass( IC_PING );
		rtiamb.publishInteractionClass( IC_PING_ACK );
		rtiamb.subscribeInteractionClass( IC_PING_ACK );
		
		logger.info( "Publish and Subscribe complete" );
	}

	/**
	 * Register and update an object representing this federate so that others may discover us.
	 * This currently covers up for the lack of MOM support in the Portico 1516e interface.
	 */
	private void registerFederateObject() throws RTIexception
	{
		logger.info( "Registering HLAobjectRoot.TestFederate object for local federate" );
		rtiamb.registerObjectInstance( OC_TEST_FEDERATE, configuration.getFederateName() );
	}
	
	/**
	 * This method ticks the RTI until we've received word that all federates listed
	 * as peers are present. 
	 */
	private void waitForPeers() throws RTIexception
	{
		///////////////////////////////////////////
		// wait for all the federates to connect //
		///////////////////////////////////////////
		logger.info( "Waiting for peers: "+configuration.getPeers() );
		List<String> absentPeers = new ArrayList<String>( configuration.getPeers() );

		while( absentPeers.isEmpty() == false )
		{
			// let the RTI work for a bit while we wait to discover the
			// objects registered by the remote federates
			rtiamb.evokeMultipleCallbacks( 1.0, 1.0 );

			// check to see who turned up
			for( TestFederate federate : storage.getPeers() )
			{
				String federateName = federate.getFederateName();
				if( absentPeers.contains(federateName) )
				{
					absentPeers.remove( federateName );
					logger.debug( "  ... found "+federateName );
				}
			}
		}

		logger.info( "All federates present - let's do this thing!" );
	}

	/**
	 * Announce all our sync points up front so that they're all ready for when we
	 * need them later.
	 */
	private void announceSyncPoints()
	{
		try
		{
			rtiamb.registerFederationSynchronizationPoint( "START_THROUGHPUT_TEST", new byte[]{} );
			rtiamb.registerFederationSynchronizationPoint( "FINISH_THROUGHPUT_TEST", new byte[]{} );
			rtiamb.registerFederationSynchronizationPoint( "START_LATENCY_TEST", new byte[]{} );
			rtiamb.registerFederationSynchronizationPoint( "FINISH_LATENCY_TEST", new byte[]{} );
		}
		catch( Exception e )
		{
			logger.warn( "Exception while registering exit sync point: "+e.getMessage() );
		}
	}
	
	/**
	 * Leave the federation. On the way out, try to destroy the federation. If we happen
	 * to be the last one out this is just a common cleanup courtesy
	 */
	private void resignAndDestroy() throws RTIexception
	{
		////////////////////////////////
		// resign from the federation //
		////////////////////////////////
		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		logger.info( "Resigned from Federation" );

		////////////////////////////////////
		// try and destroy the federation //
		////////////////////////////////////
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

	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
