/*
 *   Copyright 2015 Calytrix Technologies
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

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;
import wantest.config.Configuration;

/**
 * Represents the main executor of a particular type of test. The framework will create
 * the federate and do all the basics (create, join, pub/sub, etc...) and then pass
 * execution onto the driver implementation.
 */
public interface IDriver
{
	public String getName();
	
	public void configure( Configuration configuration );
	
	public void printWelcomeMessage();

	public void execute( RTIambassador rtiamb, FederateAmbassador fedamb, Storage storage )
		throws RTIexception;
}
