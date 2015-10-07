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
package hperf.throughput;

import java.util.HashSet;
import java.util.Set;

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
import hperf.Handles;
import hperf.Storage;
import hperf.TestFederate;
import hperf.Utils;
import hperf.config.Configuration;

import static hperf.Handles.*;

public class ThroughputFedAmb extends NullFederateAmbassador
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

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public ThroughputFedAmb( Configuration configuration, Storage storage )
	{
		this.logger = Logger.getLogger( "wt" );
		this.configuration = configuration;
		this.storage = storage;

		// sync points
		this.announcedSyncPoints = new HashSet<String>();
		this.achievedSyncPoints = new HashSet<String>();
		
		// time settings
		this.timeConstrained = false;
		this.timeRegulating = false;
		this.currentTime = 0;
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
		if( theObjectClass.equals(Handles.OC_TEST_FEDERATE) )
		{
			//
			// Class: HLAobjectRoot.TestFederate
			//
			storage.addPeer( new TestFederate(objectName,false/*local*/) );
			logger.info( "Discovered federate "+objectName );
		}
		else if( theObjectClass.equals(Handles.OC_TEST_OBJECT) )
		{
			//
			// Class: HLAobjectRoot.TestObject
			//
			// No-op
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
