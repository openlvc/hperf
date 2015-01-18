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
package wantest;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import wantest.events.Event;
import wantest.events.LatencyEvent;

/**
 * This class is used to store event information that is accumulated during the
 * throughput and latency test runs that execute as part of the test federate
 * runs. Instances of this class live inside the {@link FederateAmbassador} and are
 * passed into the various results printing methods/classes at the conclusion of
 * the testing runs.
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
	private Map<String,TestFederate> peers;
	
	// information about discovered objects
	private Map<ObjectInstanceHandle,TestObject> objects;
	
	// event information
	private ConcurrentLinkedQueue<Event> throughputEvents;
	private ConcurrentLinkedQueue<LatencyEvent> latencyEvents;
	
	// timers
	private long throughputTestStartTime;
	private long throughputTestStopTime;
	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Storage() 
	{
		this.peers = new HashMap<String,TestFederate>();
		this.objects = new HashMap<ObjectInstanceHandle,TestObject>();
		this.throughputEvents = new ConcurrentLinkedQueue<Event>();
		this.latencyEvents = new ConcurrentLinkedQueue<LatencyEvent>();
		
		// timers
		this.throughputTestStartTime = 0;
		this.throughputTestStopTime = 0;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

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
	
	public void addObject( ObjectInstanceHandle handle, TestObject object )
	{
		this.objects.put( handle, object );
	}
	
	public TestObject getObject( ObjectInstanceHandle handle )
	{
		return this.objects.get( handle );
	}

	///////////////////
	// Event Methods //
	///////////////////
	public void addThroughputEvent( Event event )
	{		
		this.throughputEvents.add( event );
	}
	
	public ConcurrentLinkedQueue<Event> getThroughputEvents()
	{
		return this.throughputEvents;
	}
	
	public void addLatencyEvent( LatencyEvent event )
	{
		this.latencyEvents.add( event );
	}
	
	public ConcurrentLinkedQueue<LatencyEvent> getLatencyEvents()
	{
		return this.latencyEvents;
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

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
