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

import java.util.ArrayList;
import java.util.List;

import wantest.Federate;
import wantest.config.Configuration;

public class Main
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	private void executeJvmFederation( Configuration configuration )
	{
		System.setProperty( "portico.connection", "jvm" );
		
		String masterFederate = configuration.getFederateName();
		List<String> allFederates = new ArrayList<String>();
		allFederates.add( masterFederate );
		allFederates.addAll( configuration.getPeers() );
		
		for( String name : allFederates )
		{
			// create a new configuration object for each of the federates so
			// that they have the proper name and peer list
			List<String> peers = new ArrayList<String>( allFederates );
			peers.remove( name );
			Configuration local = configuration.copy( name, peers );
			
			// set us up as the master if that is the case
			if( masterFederate.equals(name) )
				local.setJvmMaster( true );
			
			// run the thing
			FederateRunner runner = new FederateRunner( local );
			Thread thread = new Thread( runner, name );
			thread.start();
		}
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static void main( String[] args ) throws Exception
	{
		// load the configuration so we can tell if this is a JVM federation or not
		Configuration configuration = new Configuration();
		configuration.loadCommandLine( args );

		if( configuration.isJvmFederation() )
			new Main().executeJvmFederation( configuration );
		else		
			new Federate(configuration).execute();
	}
	

	///////////////////////////////////////////////////////////////////////////////////
	////////////// Private Inner Class: FederateThread ////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	public class FederateRunner implements Runnable
	{
		private Configuration configuration;
		public FederateRunner( Configuration configuration )
		{
			this.configuration = configuration;
		}
		
		public void run()
		{
			try
			{
				new Federate(configuration).execute();
			}
			catch( Exception e )
			{
				e.printStackTrace();
			}
		}
	}
}
