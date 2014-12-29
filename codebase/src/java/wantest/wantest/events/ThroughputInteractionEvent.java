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

import wantest.TestFederate;

public class ThroughputInteractionEvent implements Event
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private TestFederate sender;
	private int payloadSize;
	private long receivedTimestamp;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public ThroughputInteractionEvent( TestFederate sender, int payloadSize, long receivedTimestamp )
	{
		this.sender = sender;
		this.payloadSize = payloadSize;
		this.receivedTimestamp = receivedTimestamp;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public Type getType()
	{
		return Type.ThroughputInteraction;
	}

	public TestFederate getSender()
	{
		return this.sender;
	}
	
	public long getReceivedTimestamp()
	{
		return this.receivedTimestamp;
	}

	public int getPayloadSize()
	{
		return this.payloadSize;
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
