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
package wantest.latency;

import org.apache.log4j.Logger;

import hla.rti1516e.RTIambassador;
import wantest.FederateAmbassador;
import wantest.Storage;
import wantest.config.Configuration;

public class LatencyDriver
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

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute( Configuration configuration,
	                     RTIambassador rtiamb,
	                     FederateAmbassador fedamb,
	                     Storage storage )
	{
		logger = Logger.getLogger( "wantest" );
		this.configuration = configuration;
		this.rtiamb = rtiamb;
		this.fedamb = fedamb;
		this.storage = storage;

		logger.info( " ================================" );
		logger.info( " =     Running Latency Test     =" );
		logger.info( " ================================" );

		logger.info( "Latency Test Finished" );
		logger.info( "" );
	}

	public void printReport()
	{
		
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
