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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandleSet;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import hperf.config.Configuration;
import hperf.latency.LatencyEvent;

import static hperf.Handles.*;

public class FederateAmbassador extends NullFederateAmbassador
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Storage storage;
	private Configuration configuration;

	// sync point information
	public Set<String> announcedSyncPoints;
	public Set<String> achievedSyncPoints;
	
	// time settings
	public boolean timeConstrained;
	public boolean timeRegulating;
	public long currentTime;

	// latency test settings
	public LatencyEvent currentLatencyEvent;
	public ConcurrentLinkedQueue<Integer> pingReceived;
	public Object pingSignal; // object we use to signal to other thread that ping has been received 


	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public FederateAmbassador( Configuration configuration, Storage storage )
	{
		this.logger = Logger.getLogger( "hp" );
		this.configuration = configuration;
		this.storage = storage;

		// sync points
		this.announcedSyncPoints = new HashSet<String>();
		this.achievedSyncPoints = new HashSet<String>();
		
		// time settings
		this.timeConstrained = false;
		this.timeRegulating = false;
		this.currentTime = 0;
		
		// latency test settings
		this.currentLatencyEvent = null;
		this.pingReceived = new ConcurrentLinkedQueue<Integer>();
		this.pingSignal = new Object();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Object Handling Methods ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	public void discoverObjectInstance( ObjectInstanceHandle objectHandle,
	                                    ObjectClassHandle theObjectClass,
	                                    String objectName )
		throws FederateInternalError
	{
		if( theObjectClass.equals(Handles.OC_TEST_FEDERATE) )
		{
			//
			// Class: HLAobjectRoot.TestFederate
			//
			storage.addPeer( new TestFederate(objectName,false/*isLocal*/,objectHandle) );
			logger.info( "Discovered federate "+objectName+" (handle:"+objectHandle+")" );
		}
		else if( theObjectClass.equals(Handles.OC_TEST_OBJECT) )
		{
			//
			// Class: HLAobjectRoot.TestObject
			//
			// No-op
		}
		
		logger.debug( "   discoverObjectInstance(): class="+theObjectClass+
		              ", handle="+objectHandle+
		              ", name="+objectName );
	}

	public void reflectAttributeValues( ObjectInstanceHandle theObject,
	                                    AttributeHandleValueMap theAttributes,
	                                    byte[] userSuppliedTag,
	                                    OrderType sentOrdering,
	                                    TransportationTypeHandle theTransport,
	                                    SupplementalReflectInfo reflectInfo )
	    throws FederateInternalError
	{
		// if the update came with a creator name, then it is the initial update
		if( theAttributes.containsKey(AC_CREATOR) )
		{
			//
			// Initial Update
			//
			String name = new String( theAttributes.getValueReference(AC_CREATOR).array() );
			TestFederate owner = storage.getPeer( name );
			if( owner == null )
				logger.error( "Received initial update from an undiscovered federate: "+name );
			else
				storage.recordDiscover( theObject, owner );
		}
		else
		{
			//
			// Regular Reflect
			//
			// validate the data blob received
			byte[] payload = theAttributes.getValueReference(AC_PAYLOAD).array();
			if( configuration.getValidateData() )
				Utils.verifyPayload( payload, configuration.getPacketSize(), logger );

			storage.recordReflect( theObject );
		}
	}

	public void removeObjectInstance( ObjectInstanceHandle objectHandle,
	                                  byte[] tag,
	                                  OrderType sent,
	                                  SupplementalRemoveInfo info )
		throws FederateInternalError
	{
		storage.recordDelete( objectHandle );
	}

	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// Interaction Receiving Methods ////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public void receiveInteraction( InteractionClassHandle interactionClass,
	                                ParameterHandleValueMap parameters,
	                                byte[] tag,
	                                OrderType sentOrdering,
	                                TransportationTypeHandle theTransport,
	                                SupplementalReceiveInfo receiveInfo )
	    throws FederateInternalError
	{
		if( interactionClass.equals(IC_PING) )
			handlePing( parameters );
		else if( interactionClass.equals(IC_PING_ACK) )
			handlePingAck( parameters );
		else if( interactionClass.equals(IC_THROUGHPUT) )
			handleThroughputInteraction( interactionClass, parameters );
	}

	///
	/// Throughput Interaction Handling Methods
	///
	private void handleThroughputInteraction( InteractionClassHandle interactionClass,
	                                          ParameterHandleValueMap parameters )
	{
		// validate the data blob received
		byte[] payload = parameters.getValueReference(PC_THROUGHPUT_PAYLOAD).array();
		if( configuration.getValidateData() )
			Utils.verifyPayload( payload, configuration.getPacketSize(), logger );

		// find the sending federate in our list
		byte[] temp = parameters.getValueReference(PC_THROUGHPUT_SENDER).array();
		String senderName = new String( temp );
		TestFederate sender = storage.getPeer( senderName );

		// record that we received the interaction
		storage.recordInteraction( interactionClass, sender );
	}
	
	///
	/// Latency Interaction Handling Methods
	///
	
	/**
	 * Handle a received Ping event that we need to respond to.
	 */
	private void handlePing( ParameterHandleValueMap parameters )
	{
		// get the serial out
		int serial = Utils.bytesToInt( parameters.getValueReference(PC_PING_SERIAL).array() );

		// validate the payload data if we've been asked to
		if( configuration.getValidateData() )
		{
			// validate the data only if we're told to - will hurt latency!
			Utils.verifyPayload( parameters.getValueReference(PC_PING_PAYLOAD).array(),
			                     configuration.getPacketSize(),
			                     logger );
		}

		// let the people waiting on a ping know that it is here
		synchronized( pingSignal )
		{
			this.pingReceived.add( serial );
			this.pingSignal.notifyAll();
		}
	}

	/**
	 * Have received a response to a Ping (perhaps it is ours!)
	 */
	private void handlePingAck( ParameterHandleValueMap parameters )
	{
		// stop the clock!
		long receivedTimestamp = System.nanoTime();

		// are we waiting for responses?
		if( this.currentLatencyEvent == null )
			return;

		// find the TestFederate for the sender & record the timestamp
		String sender = new String( parameters.getValueReference(PC_PING_ACK_SENDER).array() );
		TestFederate federate = storage.getPeer( sender );
		this.currentLatencyEvent.addResponse( federate, receivedTimestamp );

		// validate the payload data if we've been asked to
		if( configuration.getValidateData() )
		{
			// validate the data only if we're told to - will hurt latency!
			Utils.verifyPayload( parameters.getValueReference(PC_PING_ACK_PAYLOAD).array(),
			                     configuration.getPacketSize(),
			                     logger );
		}
		
		synchronized( pingSignal )
		{
			this.pingSignal.notifyAll();
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Time Management Methods ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	@SuppressWarnings("rawtypes")
	public void timeRegulationEnabled( LogicalTime time ) throws FederateInternalError
	{
		this.timeRegulating = true;
		this.currentTime = (long)((HLAfloat64Time)time).getValue();
		logger.info( "timeRegulationEnabled("+currentTime+")" );
	}

	@SuppressWarnings("rawtypes")
	public void timeConstrainedEnabled( LogicalTime time ) throws FederateInternalError
	{
		this.timeConstrained = true;
		this.currentTime = (long)((HLAfloat64Time)time).getValue();
		logger.info( "timeConstrainedEnabled("+currentTime+")" );
	}

	@SuppressWarnings("rawtypes")
	public void timeAdvanceGrant( LogicalTime time ) throws FederateInternalError
	{
		this.currentTime = (long)((HLAfloat64Time)time).getValue();
	}

	///////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// Exit Synchronization Point Handling /////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public void announceSynchronizationPoint( String label, byte[] tag )
		throws FederateInternalError
	{
		this.announcedSyncPoints.add( label );
	}

	public void federationSynchronized( String label, FederateHandleSet failedSet )
		throws FederateInternalError
	{
		this.achievedSyncPoints.add( label );
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
