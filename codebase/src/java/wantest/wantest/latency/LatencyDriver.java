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
package wantest.latency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import hla.rti1516e.RTIambassador;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import wantest.FederateAmbassador;
import wantest.Storage;
import wantest.TestFederate;
import wantest.config.Configuration;
import wantest.federate.Utils;

public class LatencyDriver
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Configuration configuration;
	private RTIambassador rtiamb;
	private FederateAmbassador fedamb;
	private Storage storage;
	
	// execution parameters
	private byte[] payload;
	private List<String> orderedPeers;
	private HLAfloat64TimeFactory timeFactory;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void execute( Configuration configuration,
	                     RTIambassador rtiamb,
	                     FederateAmbassador fedamb,
	                     Storage storage )
	    throws RTIexception
	{
		logger = Logger.getLogger( "wantest" );
		this.configuration = configuration;
		this.rtiamb = rtiamb;
		this.fedamb = fedamb;
		this.storage = storage;
		this.payload = Utils.generatePayload( configuration.getPacketSize() );
		this.orderedPeers = createPeerList();

		logger.info( " ================================" );
		logger.info( " =     Running Latency Test     =" );
		logger.info( " ================================" );
		String sizeString = Utils.getSizeString( configuration.getPacketSize() );
		logger.info( "Minimum message size="+sizeString );
		logger.info( "Federate order: "+this.orderedPeers );
		
		// enable our time policy
		this.enableTimePolicy();
		
		// Confirm that everyone is ready to proceed
		this.waitForStart();
		
		// Loop
		for( int i = 0; i < configuration.getLoopCount(); i++ )
		{
			loop( i+1 );
		}

		// Confirm that everyone is ready to complete
		this.waitForFinish();

		logger.info( "Latency Test Finished" );
		logger.info( "" );
	}

	/**
	 * Returns a list of all participating federates, sorted in a consistent manner across
	 * all federates.
	 */
	private List<String> createPeerList()
	{
		List<String> list = new ArrayList<String>();
		list.add( configuration.getFederateName() );
		for( TestFederate testFederate : storage.getPeers() )
			list.add( testFederate.getFederateName() );
		
		Collections.sort( list );
		return list;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Time Policy Methods ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void enableTimePolicy() throws RTIexception
	{
		this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

		// turn on the time policy
		rtiamb.enableTimeConstrained();
		rtiamb.enableTimeRegulation( timeFactory.makeInterval(1.0) );
		while( fedamb.timeConstrained == false || fedamb.timeRegulating == false )
			rtiamb.evokeMultipleCallbacks( 0.5, 1.0 );

	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////// Loop ///////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * This method performs the main portion of the test. For each loop, it will send out
	 * attribute value updates for each of the objects that it is managing
	 */
	private void loop( int loopNumber ) throws RTIexception
	{
		logger.info( "Processing loop ["+loopNumber+"]" );

		// cache a couple of things that we use multiple times
		int peerCount = orderedPeers.size();
		double looptime = ((double)configuration.getLoopWait()) / 1000;

		// if we do our test initially, then `currentTime % peerCount` will be 0.
		// As we use the mod for an index into the ordered sender list, we need
		// it to be this way, but it's also our sign that this loop is over. This
		// is why we use a do..while() - by the time it gets to the test, time
		// has moved forward one step, getting us past the problem
		do
		{
			// whose turn is it?
			int senderIndex = (int)fedamb.currentTime % peerCount;
			String senderName = orderedPeers.get( senderIndex );
			if( configuration.getFederateName().equals(senderName) )
			{
				logger.info( "  Sub-Loop "+senderIndex+" -- that's me!" );
			}
			else
			{
				logger.info( "  Sub-Loop "+senderIndex+" -- that's "+senderName );
			}
			
			long requestedTime = fedamb.currentTime+1;
			rtiamb.timeAdvanceRequest( timeFactory.makeTime(requestedTime) );
			while( fedamb.currentTime < requestedTime )
				rtiamb.evokeMultipleCallbacks( looptime, looptime*4.0 );
		}
		while( fedamb.currentTime % peerCount != 0 );
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Lifecycle Methods /////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void waitForStart() throws RTIexception
	{
		// advance to time 1.0 initially so that we're ready to start
		// we start from here because... reasons (0 modulo anything is 0 -- see loop())
//		rtiamb.timeAdvanceRequest( timeFactory.makeTime(1) );
//		while( fedamb.currentTime != 1 )
//			rtiamb.evokeMultipleCallbacks( 0.1, 1.0 );
		
		// achieve the ready-to-start sync point
		rtiamb.synchronizationPointAchieved( "START_LATENCY_TEST" );
		
		// wait for everyone to do the same
		while( fedamb.startLatencyTest == false )
			rtiamb.evokeMultipleCallbacks( 0.1, 1.0 );
	}

	private void waitForFinish() throws RTIexception
	{
		rtiamb.synchronizationPointAchieved( "FINISH_LATENCY_TEST" );

		// wait for everyone to do the same
		while( fedamb.finishedLatencyTest == false )
			rtiamb.evokeMultipleCallbacks( 0.1, 1.0 );
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
