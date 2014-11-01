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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import wantest.config.Configuration;

public class Storage
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Configuration configuration;
	
	// data storage - public for code convenience (naughty!)
	public Map<String,TestFederate> peers;
	public Map<ObjectInstanceHandle,TestObject> objects;
	public List<Event> eventlist;
	
	// timers
	private long startTime;
	private long stopTime;
	private long loopStartTime;
	private List<Long> loopTimes;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Storage( Logger logger, Configuration configuration ) 
	{
		this.logger = logger;
		this.configuration = configuration;

		// data storage
		this.peers = new HashMap<String,TestFederate>();
		this.objects = new HashMap<ObjectInstanceHandle,TestObject>();
		this.eventlist = new ArrayList<Event>();
		
		// timers
		this.startTime = 0;
		this.stopTime = 0;
		this.loopStartTime = 0;
		this.loopTimes = new ArrayList<Long>();
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	/////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Report Printing //////////////////////////////////// 
	/////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Print a summary report of the activities to the logger
	 */
	public void printReport()
	{
		// print the overall summary
		String[] totalReport = getTotalEvents();
		String executionTime = (this.stopTime - this.startTime) / 1000 +"s";
		logger.info( " ========== Execution Report ==========" );
		logger.info( "   Duration: "+ executionTime );
		logger.info( "   Events Received: " );
		logger.info( "      -Discover:  "+ totalReport[0] );
		logger.info( "      -Reflect:   "+ totalReport[1] );
		logger.info( "" );
		
		// print the listing for each peer
		for( TestFederate federate : peers.values() )
		{
			String[] federateReport = getFederateEvents( federate );
			logger.info( "   => Federate ["+federate.getFederateName()+"]" );
			logger.info( "        -Discover: " + federateReport[0] );
			logger.info( "        -Reflect:  " + federateReport[1] );
			logger.info( "" );
		}
		
		printDistribution();
	}

	/** Find the total number of events that we received */
	private String[] getTotalEvents()
	{
		int actualDiscovers = 0;
		int actualReflects = 0;

		for( Event event : eventlist )
		{
			switch( event.type )
			{
				case Discovery:
					actualDiscovers++;
					break;
				case Reflection:
					actualReflects++;
					break;
			}
		}

		// build up the report string
		int peerCount = configuration.getPeers().size();
		int objectCount = configuration.getObjectCount();
		int loopCount = configuration.getLoopCount();

		int expectedDiscovers = objectCount * peerCount;
		int expectedReflects = objectCount * peerCount * loopCount;
		String discoverProblem = (actualDiscovers != expectedDiscovers) ? "(!!)" : "";
		String reflectProblem = (actualReflects != expectedReflects) ? "(!!)" : "";
		
		// prepare the final report
		String discoverString = actualDiscovers+"/"+expectedDiscovers+"   "+discoverProblem;
		String reflectString = actualReflects+"/"+expectedReflects+"   "+reflectProblem;
		return new String[]{ discoverString, reflectString };
	}

	/** Counts the total event number for a particular federate */
	private String[] getFederateEvents( TestFederate federate )
	{
		int actualDiscovers = 0;
		int actualReflects = 0;

		for( Event event : eventlist )
		{
			if( federate.containsObject(event.objectHandle) == false )
				continue;
			
			switch( event.type )
			{
				case Discovery:
					actualDiscovers++;
					break;
				case Reflection:
					actualReflects++;
					break;
			}
		}

		// build up the report string
		int expectedDiscovers = configuration.getObjectCount();
		int expectedReflects = configuration.getObjectCount() * configuration.getLoopCount();
		String discoverProblem = (actualDiscovers != expectedDiscovers) ? "(!!)" : "";
		String reflectProblem = (actualReflects != expectedReflects) ? "(!!)" : "";
		
		// prepare the final report
		String discoverString = actualDiscovers+"/"+expectedDiscovers+"   "+discoverProblem;
		String reflectString = actualReflects+"/"+expectedReflects+"   "+reflectProblem;
		return new String[]{ discoverString, reflectString };
	}

	/**
	 * This method prints two lots of information relating to the distribution of events as
	 * they were received:
	 * 
	 *   * The number of events received for each whole-second block of the test, including
	 *     the latency. Each reflection is encoded with the time at which it was sent. This
	 *     is compared on the receiver end to the time at which it arrives, generating the
	 *     latency.
	 *     
	 *   * A distribution graph that shows the number of events received each period.
	 *     This allows us to visualize the arrival of events in an ascii-graph form
	 */
	private void printDistribution()
	{
		int totalSeconds = (int)Math.ceil((this.stopTime-this.startTime)/1000);
		int[][] distribution = new int[totalSeconds][2]; // [0]: event count, [1]: total latency 
		for( Event event : eventlist )
		{
			if( event.type == Event.Type.Discovery )
				continue; // TODO implement this, but can't measure latency
			
			if( event.type == Event.Type.Reflection )
			{
				int spotInLine = (int)Math.ceil((event.receivedTimestamp - this.startTime)/1000);
				distribution[spotInLine][0]++;
				distribution[spotInLine][1] += (event.receivedTimestamp - event.sentTimestamp);
			}
		}

		logger.info( "" );
		logger.info( " === Event Distribution ===" );
		logger.info( "" );
		for( int i = 0; i < distribution.length; i++ )
		{
			// "[1s] 17 events, 17ms latency (avg)"
			
			int latency = 0;
			if( distribution[i][0] != 0 )
				latency = (int)distribution[i][1] / distribution[i][0];

			String line = String.format( "[%2ss] %3d events, %3sms latency (avg)", (i+1), distribution[i][0], latency );
			logger.info( line );
		}		
		
		// print a nice graph
		//   1. figure out the max events in any one period so we know how tall our graph must be
		//   2. loop through each event count level and draw a + if that period had at least that
		//      many events
		//
		//  25|          +
		//  20|          +
		//  15| +        +
		//  10| +  +     +
		//   5| +  +  +  +
		//    |--|--|--|--| 5|--|--|--|--|10|--|--|--|--|15|--|--|--|--|20|
		logger.info( "" );
		logger.info( "Distribution Graph   " );
		logger.info( "---------------------" );

		int maxEvents = 0;
		for( int  i = 0; i < distribution.length; i++ )
		{
			if( distribution[i][0] > maxEvents )
				maxEvents = distribution[i][0];
		}

		// now draw a nice graph
		for( int i = maxEvents; i > 0; i-- )
		{
			// only draw in groups of 5
			if( i % 5 != 0 )
				continue;
			
			StringBuffer buffer = new StringBuffer();
			buffer.append( String.format("%3d|",i) );
			for( int j = 0; j < distribution.length; j++ )
			{
				if( distribution[j][0] >= i )
					buffer.append( "++ " );
				else
					buffer.append( "   " );
			}
			
			// print the line
			logger.info( buffer );
		}

		// print the bottom line
		StringBuffer xaxis = new StringBuffer( "   |" ); // start with the lead in
		for( int i = 0; i < distribution.length; i++ )
		{
			int second = i+1;
			if( second % 5 == 0 )
				xaxis.append( String.format("%2d|",second) );
			else
				xaxis.append( "--|" );
		}
		logger.info( xaxis );		
	}
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	public void startTimer()
	{
		this.startTime = System.currentTimeMillis();
	}
	
	public void stopTimer()
	{
		this.stopTime = System.currentTimeMillis();
	}
	
	public void startLoopTimer() throws RuntimeException
	{
		if( this.loopStartTime != 0 )
			throw new RuntimeException( "Tried to start loop timer while it's already active :S" );
		
		this.loopStartTime = System.currentTimeMillis();
	}
	
	public void stopLoopTimer()
	{
		long looptime = System.currentTimeMillis() - this.loopStartTime;
		this.loopTimes.add( looptime );
		this.loopStartTime = 0;
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
