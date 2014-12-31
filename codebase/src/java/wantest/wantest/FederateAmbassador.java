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

import org.apache.log4j.Logger;

import static wantest.Handles.*;
import wantest.config.Configuration;
import wantest.events.DiscoverEvent;
import wantest.events.LatencyEvent;
import wantest.events.ReflectEvent;
import wantest.events.ThroughputInteractionEvent;
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

public class FederateAmbassador extends NullFederateAmbassador
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Configuration configuration;
	private Storage storage;
	
	// sync point information
	public boolean startThroughputTest;
	public boolean finishedThroughputTest;
	public boolean startLatencyTest;
	public boolean finishedLatencyTest;
	
	// latency test specific values
	public boolean timeConstrained;
	public boolean timeRegulating;
	public long currentTime;
	public LatencyEvent currentLatencyEvent;
	public volatile int sendingSerial; // serial of event we are currently sending
	public volatile int requestedSerial; // serial of event we need to response to

	
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public FederateAmbassador( Configuration configuration, Storage storage )
	{
		this.logger = Logger.getLogger( "wantest" );
		this.configuration = configuration;
		this.storage = storage;
		
		// sync points
		this.startThroughputTest = false;
		this.finishedThroughputTest = false;
		this.startLatencyTest = false;
		this.finishedLatencyTest = false;
		
		// latency test specific values
		this.timeConstrained = false;
		this.timeRegulating = false;
		this.currentTime = 0;
		this.sendingSerial = -1;
		this.requestedSerial = -1;
		this.currentLatencyEvent = null;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	///////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Object Handling Methods ///////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////

	public void discoverObjectInstance( ObjectInstanceHandle theObject,
	                                    ObjectClassHandle theObjectClass,
	                                    String objectName )
		throws FederateInternalError
	{
		// record the time that we received the event
		long receivedTimestamp = System.currentTimeMillis();

		if( theObjectClass.equals(Handles.OC_TEST_FEDERATE) )
		{
			//
			// Class: HLAobjectRoot.TestFederate
			//
			storage.addPeer( new TestFederate(objectName) );
		}
		else if( theObjectClass.equals(Handles.OC_TEST_OBJECT) )
		{
			//
			// Class: HLAobjectRoot.TestObject
			//
			TestObject testObject = new TestObject( theObject, objectName );
			
			// attach an event record to both the global time list and the
			// object-specific list inside each object
			DiscoverEvent event = new DiscoverEvent( testObject, receivedTimestamp );
			storage.addThroughputEvent( event );
			storage.addObject( theObject, testObject );
			testObject.addEvent( event );
		}
		
		logger.debug( "   discoverObjectInstance(): class="+theObjectClass+
		              ", handle="+theObject+
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
		// find the object this is in reference to
		TestObject testObject = storage.getObject( theObject );
		if( testObject == null )
			return; // not something we want to bother with
		
		// if the update came with a creator name, then it is the initial update
		if( theAttributes.containsKey(AC_CREATOR) )
		{
			////////////////////
			// Initial Update //
			////////////////////
			String senderName = new String( theAttributes.getValueReference(AC_CREATOR).array() );
			TestFederate sender = storage.getPeer( senderName );
			if( sender == null )
				logger.error( "Received initial update from an undiscovered federate" );
			else
				testObject.setCreator( sender );
		}
		else
		{
			/////////////////////
			// Regular Reflect //
			/////////////////////
			// Update does not contain a creator, which means it is a regular update.
			// Record the details of the event
			long receivedTimestamp = System.currentTimeMillis();
			long sentTimestamp = Utils.bytesToLong( theAttributes.getValueReference(AC_LAST_UPDATED).array() );
			byte[] payload = theAttributes.getValueReference(AC_PAYLOAD).array();

			// validate the data blob received
			if( configuration.getValidateData() )
				Utils.verifyPayload( payload, configuration.getPacketSize(), logger );

			// create an event and link it into the master list and test object's list
			ReflectEvent event = new ReflectEvent( testObject.getCreator(),
			                                       testObject,
			                                       sentTimestamp,
			                                       receivedTimestamp,
			                                       payload.length );

			storage.addThroughputEvent( event );
			testObject.addEvent( event );
		}
	}

	///////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// Interaction Receiving Methods ////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////
	public void receiveInteraction( InteractionClassHandle interactionClass,
	                                ParameterHandleValueMap theParameters,
	                                byte[] userSuppliedTag,
	                                OrderType sentOrdering,
	                                TransportationTypeHandle theTransport,
	                                SupplementalReceiveInfo receiveInfo )
	    throws FederateInternalError
	{
		if( interactionClass.equals(IC_LATENCY) )
			handleLatencyInteraction( theParameters );
		else if( interactionClass.equals(IC_THROUGHPUT) )
			handleThroughputInteraction( theParameters );
	}

	/**
	 * Handle received throughput events:
	 * 
	 *  (class ThroughputInteraction reliable timestamp
	 *    (parameter sender)
	 *    (parameter payload)
	 *  )
	 * 
	 * Creates and stores the event information.
	 */
	private void handleThroughputInteraction( ParameterHandleValueMap parameters )
	{
		// get the timestamp
		long receivedTimestamp = System.currentTimeMillis();

		// validate the data blob received
		byte[] payload = parameters.getValueReference(PC_THROUGHPUT_PAYLOAD).array();
		if( configuration.getValidateData() )
			Utils.verifyPayload( payload, configuration.getPacketSize(), logger );

		// find the sending federate in our list
		byte[] temp = parameters.getValueReference(PC_THROUGHPUT_SENDER).array();
		String senderName = new String( temp );
		TestFederate sender = storage.getPeer( senderName );

		// store the event in the master list
		ThroughputInteractionEvent event = new ThroughputInteractionEvent( sender,
		                                                                   payload.length,
		                                                                   receivedTimestamp );
		storage.addThroughputEvent( event );
	}

	/**
	 * Handle a received latency event:
	 * 
	 *   (class LatencyInteraction reliable timestamp
	 *     (parameter serial)
	 *     (parameter sender)
	 *     (parameter payload)
	 *   )
	 * 
	 */
	private void handleLatencyInteraction( ParameterHandleValueMap parameters )
	{
		// stop the clock!
		long receivedTimestamp = System.nanoTime();
		
		// If this is in response to one of our requests, the serial will match
		// the one we have stored. In this case, record the received timestamp
		// against the test federate with the event
		int serial = Utils.bytesToInt( parameters.getValueReference(PC_LATENCY_SERIAL).array() );
		if( serial == this.sendingSerial )
		{
			//////////////////////////
			// Response to our Ping //
			//////////////////////////
			// find the TestFederate for the sender & record the timestamp
			String sender = new String( parameters.getValueReference(PC_LATENCY_SENDER).array() );
			TestFederate federate = storage.getPeer( sender );
			this.currentLatencyEvent.addResponse( federate, receivedTimestamp );

			// validate the payload data if we've been asked to
			if( configuration.getValidateData() )
			{
				// validate the data only if we're told to - will hurt latency!
				Utils.verifyPayload( parameters.getValueReference(PC_LATENCY_PAYLOAD).array(),
				                     configuration.getPacketSize(),
				                     logger );
			}

		}
		else
		{
			///////////////////////////
			// Request for Ping Back //
			///////////////////////////
			// this is an initial ping from someone else - record it so we can respond
			this.requestedSerial = serial;
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
		// no-op
	}

	public void federationSynchronized( String label, FederateHandleSet failedSet )
		throws FederateInternalError
	{
		if( label.equals("START_THROUGHPUT_TEST") )
			this.startThroughputTest = true;
		else if( label.equals("FINISH_THROUGHPUT_TEST") )
			this.finishedThroughputTest = true;
		else if( label.equals("START_LATENCY_TEST") )
			this.startLatencyTest = true;
		else if( label.equals("FINISH_LATENCY_TEST") )
			this.finishedLatencyTest = true;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}