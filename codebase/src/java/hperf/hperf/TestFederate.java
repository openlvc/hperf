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

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectInstanceHandle;

public class TestFederate
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private String federateName;
	private boolean isLocal;
	private ObjectInstanceHandle myHandle;
	private HashMap<ObjectInstanceHandle,AtomicInteger> objects;
	private int discoverEvents;
	private int reflectEvents;
	private int interactionEvents;

	private long firstMessage; // time that we received the first reflect/interaction
	private long lastMessage;  // time of the last reflect/interaction we received

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public TestFederate( String federateName, boolean isLocal )
	{
		this.federateName = federateName;
		this.isLocal = isLocal;
		this.myHandle = null;
		this.objects = new HashMap<ObjectInstanceHandle,AtomicInteger>();
		this.discoverEvents = 0;
		this.reflectEvents = 0;
		this.interactionEvents = 0;
		
		this.firstMessage = 0;
		this.lastMessage = 0;
	}
	
	public TestFederate( String federateName, boolean isLocal, ObjectInstanceHandle myHandle )
	{
		this( federateName, isLocal );
		this.myHandle = myHandle;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	public void recordDiscover( ObjectInstanceHandle objectHandle )
	{
		++discoverEvents;
		this.objects.put( objectHandle, new AtomicInteger(0) );
	}
	
	public void recordReflect( ObjectInstanceHandle objectHandle )
	{
		++reflectEvents;

		// we already know about the object, so this isn't the discover call.
		// increment our counter and record the timestamp
		this.objects.get(objectHandle).incrementAndGet();

		// record timestamp of the first true throughput test event, or it is the most recent
		if( this.firstMessage == 0 )
			this.firstMessage = System.currentTimeMillis();
		else
			this.lastMessage = System.currentTimeMillis();
	}
	
	public void recordInteraction( InteractionClassHandle interactionClass )
	{
		this.interactionEvents++;

		// record timestamp of the first true throughput test event, or it is the most recent
		if( this.firstMessage == 0 )
			this.firstMessage = System.currentTimeMillis();
		else
			this.lastMessage = System.currentTimeMillis();
	}
	
	public boolean containsObject( ObjectInstanceHandle handle )
	{
		return this.objects.containsKey( handle );
	}
	
	public int compareTo( TestFederate other )
	{
		if( other == null )
			return 1;
		
		int ourHash = federateName.hashCode();
		int theirHash = other.federateName.hashCode();
		if( ourHash > theirHash )
			return 1;
		else if( ourHash < theirHash )
			return -1;
		else
			return 0;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	
	public String getFederateName()
	{
		return this.federateName;
	}
	
	public boolean isLocalFederate()
	{
		return this.isLocal;
	}

	public int getObjectCount()
	{
		return this.objects.size();
	}

	public String toString()
	{
		return this.federateName;
	}

	public int getEventCount()
	{
		return discoverEvents + reflectEvents + interactionEvents;
	}
	
	public int getDiscoverEventCount()
	{
		return this.discoverEvents;
	}
	
	public int getReflectEventCount()
	{
		return this.reflectEvents;
	}
	
	public int getInteractionEventCount()
	{
		return this.interactionEvents;
	}

	/** Time at which we received the first reflect or interaction from the federate */
	public long getFirstMessageTimestamp()
	{
		return this.firstMessage;
	}

	/** Time at which we received the most recent reflect or interaction from the federate */
	public long getLastMessageTimestamp()
	{
		return this.lastMessage;
	}

	/** Duration in millis between the first reflect/interaction from federate and
	    the most recent one */ 
	public long getReceiveWindow()
	{
		if( this.firstMessage == 0 || this.lastMessage == 0 )
			return 0;
		else
			return this.lastMessage - this.firstMessage;
	}
	
	public ObjectInstanceHandle getFederateObjectHandle()
	{
		return this.myHandle;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
