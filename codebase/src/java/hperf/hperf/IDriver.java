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

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;
import hperf.config.Configuration;

/**
 * Represents the main executor of a particular type of test. The framework will create
 * the federate and do all the basics (create, join, pub/sub, etc...) and then pass
 * execution onto the driver implementation.
 */
public interface IDriver
{
	public String getName();

	public void configure( Configuration configuration, Storage storage );

	public void printWelcomeMessage();

	/**
	 * Run the main federate loop. If this is a managed federate, the federation will have
	 * been created and joined; initial publication and subscription done, a federate object
	 * created and other expected peers will be present.
	 * 
	 * If this is an unmanaged federate, the RTIambassador param will be null as we will not
	 * have created one yet. The FederateAmbassador however is created and given to the driver
	 * by the TestRunner.
	 */
	public void execute( RTIambassador rtiamb, FederateAmbassador fedamb )
		throws RTIexception;
	
	/** This is true if the test wants to handle all the lifecycle management itself */
	public boolean manageLifecycleManually();
}

