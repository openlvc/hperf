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
package hperf;

import java.util.ArrayList;
import java.util.List;

import hperf.TestRunner;
import hperf.config.Configuration;

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

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static void main( String[] args ) throws Exception
	{
		// load the configuration so we can tell if this is a JVM federation or not
		Configuration configuration = new Configuration();
		configuration.loadCommandLine( args );

		if( configuration.isJvmFederation() )
			executeJvmFederation( configuration );
		else		
			new TestRunner(configuration).execute();
	}

	private static void executeJvmFederation( Configuration configuration )
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
			final Configuration local = configuration.copy( name, peers );
			
			// set us up as the master if that is the case
			if( masterFederate.equals(name) )
			{
				local.setJvmMaster( true );
				
				// if we're running the latency test, flag the master as a sender
				local.setSender( true );
			}
			
			// run the thing
			Runnable runner = new Runnable()
			{
				public void run()
				{
					try
					{
						new TestRunner(local).execute();
					}
					catch( Exception e ){ e.printStackTrace(); }
				}
			};
			
			//FederateRunner runner = new FederateRunner( local );
			Thread thread = new Thread( runner, name );
			thread.start();
		}
	}
	
}
