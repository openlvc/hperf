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
package wantest.throughput;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import wantest.Storage;
import wantest.TestFederate;
import wantest.Utils;
import wantest.config.Configuration;
import wantest.events.DiscoverEvent;
import wantest.events.Event;
import wantest.events.Event.Type;
import wantest.events.ReflectEvent;
import wantest.events.ThroughputInteractionEvent;

/**
 * This class takes the information that was gathered during the throughput test, does a
 * bit of aggregation and prints a report on the activities.
 */
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
		this.logger = Logger.getLogger( "wantest" );
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
		logger.info( "   Events Received: " );
		logger.info( "      -Discover:    "+ totalReport[0] );
		logger.info( "      -Reflect:     "+ totalReport[1] );
		logger.info( "      -Interaction: "+ totalReport[2] );
		logger.info( "" );
		
		// print the listing for each peer
		for( TestFederate federate : storage.getPeers() )
		{
			String[] federateReport = getFederateEvents( federate );
			logger.info( "   => Federate ["+federate.getFederateName()+"]" );
			logger.info( "        -Discover:    " + federateReport[0] );
			logger.info( "        -Reflect:     " + federateReport[1] );
			logger.info( "        -Interaction: " + federateReport[2] );
			logger.info( "" );
		}
		
		printDistribution();
		printEventLog();
		if( configuration.getExportCSV() )
			exportEventsToFile();

	}
	
	private String getExecutionTime( long runtime )
	{
		long milliseconds = storage.getThroughputTestDuration();
		if( milliseconds < 1000 )
			return milliseconds+"ms";
		else
			return Math.ceil(milliseconds/1000)+"s";
	}
	
	/** Find the total number of events that we received */
	private String[] getTotalEvents()
	{
		int actualDiscovers    = 0;
		int actualReflects     = 0;
		int actualInteractions = 0;

		for( Event event : storage.getThroughputEvents() )
		{
			switch( event.getType() )
			{
				case Discover:
					actualDiscovers++;
					break;
				case Reflect:
					actualReflects++;
					break;
				case ThroughputInteraction:
					actualInteractions++;
					break;
				default:
					break;
			}
		}
		
		// build up the report string
		int peerCount = configuration.getPeers().size();
		int objectCount = configuration.getObjectCount();
		int loopCount = configuration.getLoopCount();

		int expectedDiscovers = objectCount * peerCount;
		int expectedReflects = objectCount * peerCount * loopCount;
		int expectedInteractions = expectedReflects; // we send with each obj update
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
		int actualDiscovers    = 0;
		int actualReflects     = 0;
		int actualInteractions = 0;

		// look at each event and figure out what we want to do with it
		for( Event event : storage.getThroughputEvents() )
		{
			switch( event.getType() )
			{
				case Discover:
					DiscoverEvent discover = (DiscoverEvent)event;
					if( federate.containsObject(discover.getObjectHandle()) )
						actualDiscovers++;
					break;
				case Reflect:
					ReflectEvent reflect = (ReflectEvent)event;
					if( federate.containsObject(reflect.getObjectHandle()) )
						actualReflects++;
					break;
				case ThroughputInteraction:
					ThroughputInteractionEvent interaction = (ThroughputInteractionEvent)event;
					if( interaction.getSender() == federate )
						actualInteractions++;
					break;
				default:
					break;
			}
		}

		// build up the report string
		int expectedDiscovers = configuration.getObjectCount();
		int expectedReflects = configuration.getObjectCount() * configuration.getLoopCount();
		int expectedInteractions = expectedReflects;
		String discoverProblem = (actualDiscovers != expectedDiscovers) ? "(!!)" : "";
		String reflectProblem = (actualReflects != expectedReflects) ? "(!!)" : "";
		String interactionProblem = (actualInteractions != expectedInteractions) ? "(!!)" : "";
		
		// prepare the final report
		String discoverString = actualDiscovers+"/"+expectedDiscovers+"   "+discoverProblem;
		String reflectString = actualReflects+"/"+expectedReflects+"   "+reflectProblem;
		String interactionString = actualInteractions+"/"+expectedInteractions+"   "+interactionProblem;
		return new String[]{ discoverString, reflectString, interactionString };
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Latency Distribution Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
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
		//
		// break the full event list down into smaller sets for each 1s period
		//
		List<List<Event>> periods = new ArrayList<List<Event>>();
		long runtime = storage.getThroughputTestDuration();
		int totalSeconds = (int)Math.ceil( runtime/1000 );
		if( totalSeconds == 0 )
			totalSeconds++;

		// pre-populate all the lists, so we don't have to worry about checking for null
		for( int i = 0; i < totalSeconds; i++ )
			periods.add( new ArrayList<Event>() );
		
		//
		// loop through each event and store it in the appropriate periods list
		//
		long startTime = storage.getThroughputStartTime();
		for( Event event : storage.getThroughputEvents() )
		{
			// only show those events that move data for us
			if( event.getType() != Event.Type.Reflect &&
				event.getType() != Event.Type.ThroughputInteraction )
			{
				continue;
			}
			
			// figure out when the event happened so we know which period to store it in
			int timeOfEvent = (int)Math.floor( (event.getReceivedTimestamp() - startTime)/1000 );

			// something has a habit of tipping over the edge to the next second
			// giving an index out of bounds by asking for index 19 e.g. in a 19 sized list
			while( timeOfEvent >= periods.size() )
				timeOfEvent--;

			// make sure that for some stupid reason (such as a really quick execution) we're
			// not below 0.
			if( timeOfEvent < 0 )
				timeOfEvent = 0;

			List<Event> period = periods.get( timeOfEvent );
			period.add( event );
		}

		//
		// now we have everything broken down by period, calculate the stats for each
		//
		Period allEvents = analyzePeriod( storage.getThroughputEvents() );
		Period[] distribution = new Period[totalSeconds];
		for( int i = 0; i < periods.size(); i++ )
		{
			distribution[i] = analyzePeriod( periods.get(i) );
		}
		
		//     -------------------------------------------
		//     |        | Latency                        |
		//     | Evts   | Mean   | Med   | S.Dev | 95%M  |
		//     -------------------------------------------
		//  5s |
		//  4s |
		//  3s |
		//
		logger.info( "" );
		logger.info( " === Test Distribution ===" );
		logger.info( "" );		
		logger.info( "     -----------------------------------------------|" );
		logger.info( "     |          | Throughput            |           |" );
		logger.info( "     | Events   | Per-Second | Total    |   Msg/s   |" );
		logger.info( "     |----------|------------|----------|-----------|" );
	  //logger.info( "  12s| 10000000 |  123.4MB/s | 1234.5MB |  100000/s |" );
	  //logger.info( "     ------------------------------------------------" );
		for( int i = 0; i < distribution.length; i++ )
		{
			Period period = distribution[i];
			String line = String.format( " %3ds| %8d |  %.9s | %.8s |  %6d/s |",
			                             (i+1),
			                             period.count,
			                             period.getAvgThroughputString(1000),
			                             period.getTotalThroughputString(),
			                             period.count );

			logger.info( line );
		}

		logger.info( "     ------------------------------------------------" );
		
		// log the information for the full dataset
		String line = String.format( "  All| %8d |  %.9s | %.8s |  %6d/s |",
		                             allEvents.count,
		                             allEvents.getAvgThroughputString(runtime),
		                             allEvents.getTotalThroughputString(),
		                             (int)(allEvents.count/(runtime/1000)) );
		logger.info( line );
		logger.info( "     ------------------------------------------------" );		
	}

	/**
	 * Create and populate a period from the given list of events
	 */
	private Period analyzePeriod( Collection<Event> list )
	{
		Period period = new Period();
		int eventCount = 0;
		for( Event event : list )
		{
			if( event.getType() == Type.Reflect || event.getType() == Type.ThroughputInteraction )
			{
				eventCount++;
				period.datasize += event.getPayloadSize();
			}
		}
		
		period.count = eventCount;
		return period;
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// Event Log Methods ////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	private void printEventLog()
	{
		if( configuration.getPrintEventLog() == false )
			return;

		logger.info( "" );
		logger.info( "  ======= Event Log ========" );
		int count = 0;
		for( Event event : storage.getThroughputEvents() )
		{
			if( event.getType() == Event.Type.Discover )
			{
				DiscoverEvent discover = (DiscoverEvent)event;
				String line = String.format( "[%d] (Discover) handle=%s, received=%d",
				                             ++count,
				                             discover.getObjectHandle(),
				                             discover.getReceivedTimestamp() );
				logger.info( line ); 
			}
			else if( event.getType() == Event.Type.Reflect )
			{
				ReflectEvent reflect = (ReflectEvent)event;
				long latency = reflect.getReceivedTimestamp() - reflect.getSentTimestamp();
				String pattern = "[%04d] %s sender=%s, size=%s, latency=%3dms   [received=%d, sent=%d]";
				String line = String.format( pattern,
				                             ++count,
				                             "    (Reflect)",
				                             reflect.getSender(),
				                             Utils.getSizeString(reflect.getPayloadSize()),
				                             latency,
				                             reflect.getReceivedTimestamp(),
				                             reflect.getSentTimestamp() );
				logger.info( line );
			}
			else if( event.getType() == Event.Type.ThroughputInteraction )
			{
				ThroughputInteractionEvent interaction = (ThroughputInteractionEvent)event;
				String pattern = "[%04d] %s sender=%s, size=%s [received=%d]";
				String line = String.format( pattern,
				                             ++count,
				                             "(Interaction)",
				                             interaction.getSender(),
				                             Utils.getSizeString(interaction.getPayloadSize()),
				                             event.getReceivedTimestamp() );
				logger.info( line );
			}
			
		}
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////// CSV File Dumping /////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private void exportEventsToFile()
	{
		File file = new File( configuration.getCSVFile() );
		if( file.exists() )
			logger.warn( "File ["+file.getAbsolutePath()+"] exists, about to overwrite" );
		
		logger.info( "Writing eventlog to file: "+file.getAbsolutePath() );
		
		try
		{
			PrintWriter writer = new PrintWriter( file );
			writer.println( "ID, Type, Sender, Size, Received Timestamp" );
			int count = 0;
			for( Event event : storage.getThroughputEvents() )
			{
				// only dump out reflect/interaction stuff for now
				if( event.getType() != Event.Type.Reflect &&
					event.getType() != Event.Type.ThroughputInteraction )
				{
					continue;
				}
				
				String type = event.getType() == Event.Type.Reflect ? "Reflection" : "Interaction";
				String line = String.format( "%d, %s, %s, %s, %d",
				                             ++count,
				                             type,
				                             event.getSender(),
				                             event.getPayloadSize(),
				                             event.getReceivedTimestamp() );
				writer.println( line );
			}
			
			writer.close();
			logger.info( "Write "+count+" records from eventlog to file" );
		}
		catch( Exception e )
		{
			logger.error( "Error writing event list to CSV file", e );
		}
	}

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------

	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////// Priave Class: Period ///////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////
	private class Period
	{
		public int count         = 0;
		public long datasize     = 0;   // in bytes
		
		public String getAvgThroughputString( long periodmillis )
		{
			// We already have conversion methods, but I want to define
			// specific widths and don't want to see "0b" as a string. We'll
			// stop at KB here and always put it in the decimal point format.
			double totalkb = datasize / 1000;
			double totalmb = totalkb / 1000;
			double totalgb = totalmb / 1000;
			double kbs = (totalkb / periodmillis) * 1000;
			double mbs = (totalmb / periodmillis) * 1000;
			double gbs = (totalgb / periodmillis) * 1000;
			if( gbs > 1 )
				return String.format( "%5.1fGB/s", gbs );
			else if( mbs > 1 )
				return String.format( "%5.1fMB/s", mbs );
			else
				return String.format( "%5.1fKB/s", kbs );
		}
		
		public String getTotalThroughputString()
		{
			// We already have conversion methods, but I want to define
			// specific widths and don't want to see "0b" as a string. We'll
			// stop at KB here and always put it in the decimal point format.
			double kbs = datasize / 1000;
			double mbs = kbs / 1000;
			double gbs = mbs / 1000;
			if( gbs > 1 )
				return String.format( "%6.1fGB", gbs );
			else if( mbs > 1 )
				return String.format( "%6.1fMB", mbs );
			else
				return String.format( "%6.1fKB", kbs );
		}
	}
}
