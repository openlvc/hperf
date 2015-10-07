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

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ParameterHandle;

public class Handles
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	// Object and Attributes
	// Class: TestFederate
	public static ObjectClassHandle OC_TEST_FEDERATE = null;
	public static AttributeHandle   AC_FEDERATE_NAME = null;

	// Class: TestObject
	public static ObjectClassHandle OC_TEST_OBJECT   = null;
	public static AttributeHandle   AC_CREATOR       = null; // string - federate name
	public static AttributeHandle   AC_PAYLOAD       = null; // byte[] stuffing

	// Interactions and Parameters
	// Class: ThroughputInteraction
	public static InteractionClassHandle IC_THROUGHPUT         = null;
	public static ParameterHandle        PC_THROUGHPUT_SENDER  = null; // string - federate name
	public static ParameterHandle        PC_THROUGHPUT_PAYLOAD = null; // byte[] stuffing

	// Class: Ping
	public static InteractionClassHandle IC_PING         = null;
	public static ParameterHandle        PC_PING_SERIAL  = null; // int - event id
	public static ParameterHandle        PC_PING_SENDER  = null; // string - federate name
	public static ParameterHandle        PC_PING_PAYLOAD = null; // byte[] stuffing

	// Class: PingAck
	public static InteractionClassHandle IC_PING_ACK         = null;
	public static ParameterHandle        PC_PING_ACK_SERIAL  = null; // int - event id
	public static ParameterHandle        PC_PING_ACK_SENDER  = null; // string - federate name
	public static ParameterHandle        PC_PING_ACK_PAYLOAD = null; // byte[] stuffing

}
