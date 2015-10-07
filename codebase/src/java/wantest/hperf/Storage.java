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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectInstanceHandle;

/**
 * This class is used to store event information that is accumulated during throughput
 * and latency test federate runs. Instances of this class live are created by each
 * {@Federate} instance and handed to the {@link IDriver} for the tests so that they can
 * put information into them. At the conclusion of the tests, the stored results are passed
 * to the report generators for each test federate.
 */

public class Storage
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	// information about other federates
	private TestFederate localFederate;
	private Map<String,TestFederate> peers;

	// throughput test specific data
	private Map<ObjectInstanceHandle,TestFederate> objectOwners;
	private AtomicInteger discoverEvents;
	private AtomicInteger reflectEvents;
	private AtomicInteger interactionEvents;
//	private int discoverEvents;
//	private int reflectEvents;
//	private int interactionEvents;
	private long throughputTestStartTime;
	private long throughputTestStopTime;
	

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Storage() 
	{
		this.localFederate = null;
		this.peers = new HashMap<String,TestFederate>();

		// throughput test specific data
		this.objectOwners = new HashMap<ObjectInstanceHandle,TestFederate>();
		this.discoverEvents = new AtomicInteger(0);
		this.reflectEvents = new AtomicInteger(0);
		this.interactionEvents = new AtomicInteger(0);
//		this.discoverEvents = 0;
//		this.reflectEvents = 0;
//		this.interactionEvents = 0;
		this.throughputTestStartTime = 0;
		this.throughputTestStopTime = 0;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public TestFederate getOwner( ObjectInstanceHandle objectHandle )
	{
		return this.objectOwners.get( objectHandle );
	}
	
	public void recordDiscover( ObjectInstanceHandle objectHandle, TestFederate owner )
	{
		discoverEvents.incrementAndGet();
//		++discoverEvents;

		// tell the owning federate about the object
		owner.recordDiscover( objectHandle );
		
		// cache the owner against the handle locally for quick fetching
		objectOwners.put( objectHandle, owner );
	}
	
	public void recordReflect( ObjectInstanceHandle objectHandle )
	{
		reflectEvents.incrementAndGet();
//		++reflectEvents;
		objectOwners.get(objectHandle).recordReflect( objectHandle );
	}
	
	public void recordInteraction( InteractionClassHandle interactionClass, TestFederate sender )
	{
		interactionEvents.incrementAndGet();
//		++interactionEvents;
		sender.recordInteraction( interactionClass );
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public void addPeer( TestFederate federate )
	{
		this.peers.put( federate.getFederateName(), federate );
	}
	
	public Collection<TestFederate> getPeers()
	{
		return this.peers.values();
	}
	
	public TestFederate getPeer( String name )
	{
		return this.peers.get( name );
	}
	
	public void setLocalFederate( TestFederate federate )
	{
		this.localFederate = federate;
		this.peers.put( federate.getFederateName(), federate );
	}
	
	public TestFederate getLocalFederate()
	{
		return this.localFederate;
	}

	///////////////////
	// Timer Methods //
	///////////////////
	public void startThroughputTestTimer()
	{
		this.throughputTestStartTime = System.currentTimeMillis();
	}
	
	public void stopThroughputTestTimer()
	{
		this.throughputTestStopTime = System.currentTimeMillis();
	}
	
	public long getThroughputTestDuration()
	{
		return this.throughputTestStopTime - throughputTestStartTime;
	}
	
	public long getThroughputStartTime()
	{
		return this.throughputTestStartTime;
	}
	
	public int getThroughputEventCount()
	{
		return discoverEvents.get() + reflectEvents.get() + interactionEvents.get();
	}
	
	public int getDiscoverEventCount()
	{
		return this.discoverEvents.get();
	}
	
	public int getReflectEventCount()
	{
		return this.reflectEvents.get();
	}
	
	public int getInteractionEventCount()
	{
		return this.interactionEvents.get();
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
