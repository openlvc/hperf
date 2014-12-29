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

import wantest.TestFederate;
import wantest.TestObject;
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
	public enum Type{ Discovery, Reflection, Interaction };
	
	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	public Type type;
	public TestFederate sender;
	public ObjectInstanceHandle objectHandle;
	public long sentTimestamp;
	public long receivedTimestamp;
	public long datasize;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	private Event( Type type )
	{
		this.type = type;
		this.sender = null;
		this.objectHandle = null;
		this.sentTimestamp = 0;
		this.receivedTimestamp = 0;
		this.datasize = 0;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	public boolean isDiscover()
	{
		return this.type == Type.Discovery;
	}
	
	public boolean isReflection()
	{
		return this.type == Type.Reflection;
	}
	
	public boolean isInteraction()
	{
		return this.type == Type.Interaction;
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static Event createInteraction( TestFederate sender,
	                                       long sentTimestamp,
	                                       long receivedTimestamp,
	                                       long datasize )
	{
		Event event = new Event( Type.Interaction );
		event.sender = sender;
		event.sentTimestamp = sentTimestamp;
		event.receivedTimestamp = receivedTimestamp;
		event.datasize = datasize;
		return event;
	}
	
	public static Event createDiscover( ObjectInstanceHandle handle, long receivedTimestamp )
	{
		Event event = new Event( Type.Discovery );
		event.objectHandle = handle;
		event.receivedTimestamp = receivedTimestamp;
		return event;
	}
	
	public static Event createReflection( ObjectInstanceHandle handle,
	                                      TestFederate sender,
	                                      long sentTimestamp,
	                                      long receivedTimestamp,
	                                      long datasize )
	{
		Event event = new Event( Type.Reflection );
		event.objectHandle = handle;
		event.sender = sender;
		event.sentTimestamp = sentTimestamp;
		event.receivedTimestamp = receivedTimestamp;
		event.datasize = datasize;
		return event;
	}

}
