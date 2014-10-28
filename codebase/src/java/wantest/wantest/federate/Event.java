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

import hla.rti1516e.ObjectInstanceHandle;

/**
 * This class is included in {@link TestObject}s as a way to track when remote reflections
 * are received, and to count the number of discrete events received for an object. The
 * timing information is used at the end of the simulation run to generate summary results
 * on the distribution of events .....
 * 
 *
 */
public class Event
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	public enum Type{ Discovery, Reflection };
	
	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	public Type type;
	public ObjectInstanceHandle objectHandle;
	public long sentTimestamp;
	public long receivedTimestamp;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Event( Type type, ObjectInstanceHandle objectHandle )
	{
		this.type = type;
		this.objectHandle = objectHandle;
		this.sentTimestamp = 0;
		this.receivedTimestamp = 0;
	}
	
	public Event( Type type,
	              ObjectInstanceHandle objectHandle,
	              long sentTimestamp,
	              long receivedTimestamp )
	{
		this( type, objectHandle );
		this.sentTimestamp = sentTimestamp;
		this.receivedTimestamp = receivedTimestamp;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
