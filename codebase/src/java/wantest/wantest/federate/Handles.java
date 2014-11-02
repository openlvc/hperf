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
	// Class: TestObject
	public static ObjectClassHandle OC_TEST_OBJECT    = null;
	public static AttributeHandle   ATT_LAST_UPDATED  = null;
	public static AttributeHandle   ATT_CREATOR_NAME  = null;
	public static AttributeHandle   ATT_BYTE_BUFFER   = null;

	// Class: TestFederate
	public static ObjectClassHandle OC_TEST_FEDERATE  = null;
	public static AttributeHandle   ATT_FEDERATE_NAME = null;

	// Interactions and Parameters
	// Class: TestInteraction
	public static InteractionClassHandle IC_TEST_INTERACTION = null;
	public static ParameterHandle        PRM_SENDING_FED     = null;
	public static ParameterHandle        PRM_SEND_TIME       = null;
	public static ParameterHandle        PRM_BYTE_BUFFER     = null;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}