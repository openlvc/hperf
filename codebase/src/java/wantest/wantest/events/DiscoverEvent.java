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
package wantest.events;

import hla.rti1516e.ObjectInstanceHandle;
import wantest.TestFederate;
import wantest.TestObject;

public class DiscoverEvent implements Event
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private TestObject testObject;
	private long receivedTimestamp;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public DiscoverEvent( TestObject testObject, long receivedTimestamp )
	{
		this.testObject = testObject;
		this.receivedTimestamp = receivedTimestamp;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public Type getType()
	{
		return Type.Discover;
	}

	public TestObject getObject()
	{
		return this.testObject;
	}

	public ObjectInstanceHandle getObjectHandle()
	{
		return this.testObject.getHandle();
	}

	public long getReceivedTimestamp()
	{
		return this.receivedTimestamp;
	}
	
	public TestFederate getSender()
	{
		return null;
	}
	
	public int getPayloadSize()
	{
		return 0;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
