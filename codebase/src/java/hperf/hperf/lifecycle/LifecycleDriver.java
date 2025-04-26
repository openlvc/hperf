/*
 *   Copyright 2015 The OpenLVC Group
 *
 *   This file is part of hperf.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of The OpenLVC Group.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to The OpenLVC Group.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from The OpenLVC Group.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package hperf.lifecycle;

import org.apache.log4j.Logger;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hperf.FederateAmbassador;
import hperf.IDriver;
import hperf.Storage;
import hperf.TestFederate;
import hperf.Utils;
import hperf.config.Configuration;
import hperf.config.LoggingConfigurator;

import static hperf.Handles.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LifecycleDriver implements IDriver
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
	private FederateAmbassador fedamb;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public boolean manageLifecycleManually()
	{
		return true;
	}

	public void printWelcomeMessage()
	{
		logger.info( " ==================================" );
		logger.info( " =     Running Lifecycle Test     =" );
		logger.info( " ==================================" );
		logger.info( "Loop Count = "+configuration.getLoopCount() );
		logger.info( "Sender/Receiver: "+(configuration.isSender() ? "SENDER":"RECIEVER") );	
	}

	public String getName()
	{
		return "Lifecycle Test";
	}

	public void configure( Configuration configuration, Storage storage )
	{
		this.configuration = configuration;
		this.storage = storage;
		this.logger = LoggingConfigurator.getLogger( configuration.getFederateName() );
	}

	public void execute( RTIambassador rtiamb, FederateAmbassador fedamb ) throws RTIexception
	{
		this.fedamb = fedamb;
		
		if( configuration.isSender() )
		{
			//
			// Sender -- keep trying to rejoin and receive peer notifications when present
			// 
			for( int i = 0; i < configuration.getLoopCount(); i++ )
			{
				doSetup();
				waitForPeers();
				doCleanup();
				logger.info( "Completed loop "+(i+1) );
			}
		}
		else
		{
			//
			// Receiver -- just sit in the federation and anwser calls
			// 
			doSetup();
			logger.info( "Federation created, waiting for others to commence" );

			for( int i = 0; i < configuration.getLoopCount(); i++ )
			{
				waitForPeers();
				storage.clearPeers();
				logger.info( "Completed loop "+(i+1) );
			}

			// sleep briefly to let any joining federates discover our object, then ditch
			Utils.sleep( 2000 );
			doCleanup();
		}
	}

	private void doSetup() throws RTIexception
	{
		// 1. create and join
		createAndJoinFederation();

		// 2. publish and subscribe the default set of stuff
		publishAndSubscribe();
		
		// 3. register federate object
		registerFederateObject();
	}

	private void createAndJoinFederation() throws RTIexception
	{
		// Create a new FederateAmbassador for this fresh run
		this.fedamb = new FederateAmbassador( configuration, storage );

		// Create the ambassador and connect to the federation
		/////////////////////////////////////////////////
		// 1 & 2. create the RTIambassador and Connect //
		/////////////////////////////////////////////////
		logger.debug( "Creating RTIambassador" );
		this.rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		
		// connect
		logger.debug( "Connecting..." );
		CallbackModel cbmodel = configuration.isImmediateCallback() ? CallbackModel.HLA_IMMEDIATE :
		                                                              CallbackModel.HLA_EVOKED;
		rtiamb.connect( fedamb, cbmodel );

		//////////////////////////////
		// 3. create the federation //
		//////////////////////////////
		logger.debug( "Creating Federation..." );
		// We attempt to create a new federation with the first three of the
		// restaurant FOM modules covering processes, food and drink
		try
		{
			URL[] modules = new URL[]{
			    (new File("config/testfom.fed")).toURI().toURL()
			};
			
			rtiamb.createFederationExecution( configuration.getFederationName(), modules );
			logger.debug( "Created Federation ["+configuration.getFederationName()+"]" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			logger.debug( "Didn't create federation, it already existed" );
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

		logger.debug( "Joined Federation as " + configuration.getFederateName() );

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
		AC_CREATOR      = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "creator" );
		AC_PAYLOAD      = rtiamb.getAttributeHandle( OC_TEST_OBJECT, "payload" );
		
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
		attributes.add( AC_PAYLOAD );
		rtiamb.publishObjectClassAttributes( OC_TEST_OBJECT, attributes );
		rtiamb.subscribeObjectClassAttributes( OC_TEST_OBJECT, attributes );

		logger.debug( "Publish and Subscribe complete" );
	}
	
	/**
	 * Register and update an object representing this federate so that others may discover us.
	 * This currently covers up for the lack of MOM support in the Portico 1516e interface.
	 */
	private void registerFederateObject() throws RTIexception
	{
		logger.debug( "Registering HLAobjectRoot.TestFederate object for local federate" );
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
		absentPeers.remove( storage.getLocalFederate() );

		while( absentPeers.isEmpty() == false )
		{
			// let the RTI work for a bit while we wait to discover the
			// objects registered by the remote federates
			if( configuration.isImmediateCallback() )
				Utils.sleep( 500 );
			else
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

		logger.debug( "All federates present - let's do this thing!" );
	}
	
	private void doCleanup() throws RTIexception
	{
		////////////////////////////////
		// resign from the federation //
		////////////////////////////////
		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		logger.debug( "Resigned from Federation" );

		////////////////////////////////////
		// try and destroy the federation //
		////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		//       remain. in that case we'll leave it for them to clean up
		try
		{
			rtiamb.destroyFederationExecution( configuration.getFederationName() );
			logger.debug( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			logger.debug( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			logger.debug( "Didn't destroy federation, federates still joined" );
		}

		/////////////////////////////
		// disconnect from the RTI //
		/////////////////////////////
		// This lets the LRC clean things up
		rtiamb.disconnect();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
