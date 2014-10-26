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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import wantest.config.Configuration;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;

import org.apache.log4j.Logger;


public class WanTestFederate
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
	private WanTestFederateAmbassador fedamb;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void runFederate( String[] args ) throws Exception
	{
		this.logger = Logger.getLogger( "wantest" ); 
		
		this.configuration = new Configuration();
		this.configuration.loadCommandLine( args );
		this.configuration.loadConfigurationFile();
		
		// Create and Join
		this.createAndJoinFederation();
		
		// Publish and Subscribe
		this.publishAndSubscribe();
		
		// Register our objects
		this.registerObjects();
		
		// Wait until everyone else turns up
		this.waitForPeers();
		
		// Do our thing
		for( int i = 0; i < configuration.getLoopCount(); i++ )
		{
			step( i+1 );
		}
		
		// Log summary information
		this.logResultSummary();
		
		// Get out of here
		this.resignAndDestroy();
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
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
		fedamb = new WanTestFederateAmbassador( this );
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
			
			rtiamb.createFederationExecution( "WanTest", modules );
			logger.info( "Created Federation" );
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
	
	}
	
	private void publishAndSubscribe()
	{
		
	}
	
	private void registerObjects()
	{
		
	}
	
	private void waitForPeers()
	{
		
	}
	
	private void step( int stepNumber )
	{
		
	}
	
	private void logResultSummary()
	{
		
	}
	
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
