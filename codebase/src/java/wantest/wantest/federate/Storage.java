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
