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

import java.util.HashMap;
import java.util.Map;

import wantest.TestFederate;

public class LatencyEvent
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private int serial;
	private long sentTimestamp;
	private int payloadSize;
	private int responseCount;
	private Map<TestFederate,Long> responses;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public LatencyEvent( int serial, int responseCount, int payloadSize )
	{
		this.serial = serial;
		this.sentTimestamp = 0;
		this.responseCount = responseCount;
		this.payloadSize = payloadSize;
		this.responses = new HashMap<TestFederate,Long>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public int getSerial()
	{
		return this.serial;
	}
	
	public long getSentTimestamp()
	{
		return this.sentTimestamp;
	}

	public void setSentTimestamp( long timestamp )
	{
		this.sentTimestamp = timestamp;
	}

	public int getPayloadSize()
	{
		return this.payloadSize;
	}
	
	public void addResponse( TestFederate sender, long timestamp )
	{
		this.responses.put( sender, timestamp );
	}
	
	public boolean hasReceivedAllResponses()
	{
		return responses.size() == responseCount;
	}
	
	public Map<TestFederate,Long> getResponses()
	{
		return this.responses;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
