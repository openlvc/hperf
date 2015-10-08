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

import org.apache.log4j.Logger;

import hperf.Storage;
import hperf.TestFederate;
import hperf.Utils;
import hperf.config.Configuration;
import hperf.config.LoggingConfigurator;

public class ThroughputReportGenerator
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

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public ThroughputReportGenerator( Configuration configuration, Storage storage )
	{
		this.logger = LoggingConfigurator.getLogger( configuration.getFederateName() );
		this.configuration = configuration;
		this.storage = storage;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void printReport()
	{
		// print the overall summary
		String[] totalReport = getTotalEvents();
		String executionTime = getExecutionTime( storage.getThroughputTestDuration() );
		logger.info( " ==================================" );
		logger.info( " =     Throughput Test Report     =" );
		logger.info( " ==================================" );
		logger.info( "   Duration: "+ executionTime );
		logger.info( "   Events Sent / Received: " );
		logger.info( "      -Discover:    "+ totalReport[0] );
		logger.info( "      -Reflect:     "+ totalReport[1] );
		logger.info( "      -Interaction: "+ totalReport[2] );
		logger.info( "" );
		
		// print the listing for each peer
		for( TestFederate federate : storage.getPeers() )
		{
			String receiveWindow = getExecutionTime( federate.getReceiveWindow() );
			String[] federateReport = getFederateEvents( federate );
			String local = federate.isLocalFederate() ? "  ** LOCAL FEDERATE **" : "";
			logger.info( "   => Federate ["+federate.getFederateName()+"]"+local );
			logger.info( "        -Receive Window: " + receiveWindow );
			logger.info( "        -Discover:       " + federateReport[0] );
			logger.info( "        -Reflect:        " + federateReport[1] );
			logger.info( "        -Interaction:    " + federateReport[2] );
			logger.info( "" );
		}

		printThroughputTable();
	}

	/**
	 * Given a `runtime` in millieseconds, return a string representing it. String is in the
	 * format "1.23s (1234ms)" unless time is under 1s, in which case it is "999ms".
	 */
	private String getExecutionTime( long milliseconds )
	{
		if( milliseconds < 1000 )
		{
			return milliseconds+"ms";
		}
		else
		{
			return String.format( "%.2fs (%dms)", milliseconds/1000.0, milliseconds );
		}
	}
	
	/** Find the total number of events that we received */
	private String[] getTotalEvents()
	{
		int actualDiscovers    = storage.getDiscoverEventCount();
		int actualReflects     = storage.getReflectEventCount();
		int actualInteractions = storage.getInteractionEventCount();

		// build up the report string
		int peerCount = configuration.getPeers().size()+1;
		int objectCount = configuration.getObjectCount();
		int interactionCount = configuration.getInteractionCount();
		int loopCount = configuration.getLoopCount();

		int expectedDiscovers = objectCount * peerCount;
		int expectedReflects = objectCount * peerCount * loopCount;
		int expectedInteractions = interactionCount * peerCount * loopCount;
		String discoverProblem = (actualDiscovers != expectedDiscovers) ? "(!!)" : "";
		String reflectProblem = (actualReflects != expectedReflects) ? "(!!)" : "";
		String interactionProblem = (actualInteractions != expectedInteractions) ? "(!!)" : "";
		
		// prepare the final report
		String discoverString = actualDiscovers+"/"+expectedDiscovers+"   "+discoverProblem;
		String reflectString = actualReflects+"/"+expectedReflects+"   "+reflectProblem;
		String interactionString = actualInteractions+"/"+expectedInteractions+"   "+interactionProblem;
		return new String[]{ discoverString, reflectString, interactionString };
	}

	/** Counts the total event number for a particular federate */
	private String[] getFederateEvents( TestFederate federate )
	{
		int actualDiscovers    = federate.getDiscoverEventCount();
		int actualReflects     = federate.getReflectEventCount();
		int actualInteractions = federate.getInteractionEventCount();

		// build up the report string
		int expectedDiscovers = configuration.getObjectCount();
		int expectedReflects = configuration.getObjectCount() * configuration.getLoopCount();
		int expectedInteractions = configuration.getInteractionCount() * configuration.getLoopCount();
		String discoverProblem = (actualDiscovers != expectedDiscovers) ? "(!!)" : "";
		String reflectProblem = (actualReflects != expectedReflects) ? "(!!)" : "";
		String interactionProblem = (actualInteractions != expectedInteractions) ? "(!!)" : "";
		
		// prepare the final report
		String discoverString = actualDiscovers+"/"+expectedDiscovers+"   "+discoverProblem;
		String reflectString = actualReflects+"/"+expectedReflects+"   "+reflectProblem;
		String interactionString = actualInteractions+"/"+expectedInteractions+"   "+interactionProblem;
		return new String[]{ discoverString, reflectString, interactionString };
	}

	/**
	 * Print a table with the relative throughput for each federate
	 */
	private void printThroughputTable()
	{
		logger.info( "" );
		logger.info( " === Throughput Table ===" );
		logger.info( "" );		
		//logger.info( "      ----------------------------------------------------------|" );
		//logger.info( "      | Total                          | Throughput             |" );
		//logger.info( "      | Window |  Messages  | Data     |   Data/s   |   Msg/s   |" );
		//logger.info( "      |--------|------------|----------|------------|-----------|" );
		//logger.info( "  per | 12345s | 1000000000 | 1234.5MB |  123.4MB/s |  100000/s |" );
		//logger.info( "      -----------------------------------------------------------" );

		logger.info( "         |----------------------------------------------------------|" );
		logger.info( "         |        ---- Totals ----         |    -- Throughput --    |" );
		logger.info( "         | Window  |  Messages  |   Data   |   Data/s   |   Msg/s   |" );
		logger.info( "         |---------|------------|----------|------------|-----------|" );

		// display stats for the local federate
		logThroughputTableEntry( storage.getLocalFederate() );
		logger.info( "         |----------------------------------------------------------|" );
		
		// display stats for each of the peers
		for( TestFederate federate : storage.getPeers() )
		{
			// skip the local federate - we'll display that separately
			if( federate.isLocalFederate() == false )
				logThroughputTableEntry( federate );
		}

		logger.info( "         |----------------------------------------------------------|" );
		
		// log the overall totals
		long reflectEvents = storage.getReflectEventCount();
		long reflectBytes  = reflectEvents * configuration.getPacketSize();
		long interactionEvents = storage.getInteractionEventCount();
		long interactionBytes  = interactionEvents * configuration.getPacketSize();
		
		long receiveWindow = storage.getThroughputTestDuration();
		long totalBytes = reflectBytes + interactionBytes;   // total size of all data recv'd
		long eventCount = reflectEvents + interactionEvents; // no discover -- outside recv window
		double dataPerSecond = (totalBytes/(double)receiveWindow) * 1000.0; // revc window in ms
		double msgsPerSecond = (eventCount/(double)receiveWindow) * 1000.0;
		
		String line = String.format( "%8s | %7s | %10s | %8s | %10s | %7d/s |",
		                             "All",
		                             getTimeString(receiveWindow),
		                             eventCount,
		                             Utils.getSizeString( reflectBytes+interactionBytes, 1 ),
		                             Utils.getSizeStringPerSec( dataPerSecond, 1 ),
		                             (int)msgsPerSecond );
		logger.info( line );
		logger.info( "         |----------------------------------------------------------|" );
		logger.info( "" );
		
	}

	private void logThroughputTableEntry( TestFederate federate )
	{
		String federateName = federate.isLocalFederate() ? "--us--" : federate.getFederateName();
		long reflectEvents = federate.getReflectEventCount();
		long reflectBytes  = reflectEvents * configuration.getPacketSize();
		long interactionEvents = federate.getInteractionEventCount();
		long interactionBytes  = interactionEvents * configuration.getPacketSize();
		
		long receiveWindow = federate.getReceiveWindow();    // time from first to last msg
		long totalBytes = reflectBytes + interactionBytes;   // total size of all data recv'd
		long eventCount = reflectEvents + interactionEvents; // no discover -- outside recv window
		double dataPerSecond = (totalBytes/(double)receiveWindow) * 1000.0; // revc window in ms
		double msgsPerSecond = (eventCount/(double)receiveWindow) * 1000.0;
		
		String line = String.format( "%8s | %7s | %10s | %8s | %10s | %7d/s |",
		                             federateName,
		                             getTimeString(receiveWindow),
		                             eventCount,
		                             Utils.getSizeString( reflectBytes+interactionBytes, 1 ),
		                             Utils.getSizeStringPerSec( dataPerSecond, 1 ),
		                             (int)msgsPerSecond );
		logger.info( line );
	}

	private String getTimeString( long milliseconds )
	{
		if( milliseconds < 10000 )
		{
			return milliseconds+"ms";
		}
		else
		{
			long seconds = Math.floorDiv( milliseconds, 1000 );
			long minutes = Math.floorDiv( seconds, 60 );
			long remainder = seconds % 60;
			
			if( minutes == 0 )
				return seconds+"s";
			else
				return minutes+"m "+remainder+"s";
		}
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
