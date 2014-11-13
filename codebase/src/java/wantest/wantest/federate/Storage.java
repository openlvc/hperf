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

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	// exit synchronization
	public boolean readyToResign;

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
		
		// exit synchronization
		this.readyToResign = false;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

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
		logger.info( "      -Discover:    "+ totalReport[0] );
		logger.info( "      -Reflect:     "+ totalReport[1] );
		logger.info( "      -Interaction: "+ totalReport[2] );
		logger.info( "" );
		
		// print the listing for each peer
		for( TestFederate federate : peers.values() )
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

	/** Find the total number of events that we received */
	private String[] getTotalEvents()
	{
		int actualDiscovers    = 0;
		int actualReflects     = 0;
		int actualInteractions = 0;

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
				case Interaction:
					actualInteractions++;
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
		for( Event event : eventlist )
		{
			switch( event.type )
			{
				case Discovery:
					if( federate.containsObject(event.objectHandle) )
						actualDiscovers++;
					break;
				case Reflection:
					if( federate.containsObject(event.objectHandle) )
						actualReflects++;
					break;
				case Interaction:
					if( event.sender == federate )
						actualInteractions++;
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

	private void printEventLog()
	{
		if( configuration.getPrintEventLog() == false )
			return;

		logger.info( "" );
		logger.info( "  ======= Event Log ========" );
		int count = 0;
		for( Event event : eventlist )
		{
			if( event.type == Event.Type.Discovery )
			{
				String line = String.format( "[%d] (Discover) handle=%s, received=%d",
				                             ++count,
				                             event.objectHandle.toString(),
				                             event.receivedTimestamp );
				logger.info( line ); 
			}
			else if( event.type == Event.Type.Reflection || event.type == Event.Type.Interaction )
			{
				String type = event.type == Event.Type.Reflection ? "    (Reflect)" : "(Interaction)";
				long latency = event.receivedTimestamp - event.sentTimestamp;
				String pattern = "[%04d] %s sender=%s, size=%s, latency=%3dms   [received=%d, sent=%d]";
				String line = String.format( pattern,				                             
				                             ++count,
				                             type,
				                             event.sender,
				                             Utils.getSizeString(event.datasize),
				                             latency,
				                             event.receivedTimestamp,
				                             event.sentTimestamp );
				logger.info( line );
			}
			
		}
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
		long runtime = this.stopTime - this.startTime;
		int totalSeconds = (int)runtime/1000;
		// pre-populate all the lists, so we don't have to worry about checking for null
		for( int i = 0; i < totalSeconds; i++ )
			periods.add( new ArrayList<Event>() );
		
		//
		// loop through each event and store it in the appropriate periods list
		//
		for( Event event : eventlist )
		{
			if( event.type == Event.Type.Discovery )
				continue; // TODO skip for now - can't easily measure latency
			
			// figure out when the event happened so we know which period to store it in
			int timeOfEvent = (int)Math.floor( ((event.receivedTimestamp-this.startTime)/1000) );
			if( timeOfEvent < 0 )
				timeOfEvent = 0;

			// something has a habit of tipping over the edge to the next second
			// giving an index out of bounds by asking for index 19 e.g. in a 19 sized list
			while( timeOfEvent >= periods.size() )
				timeOfEvent--;
			List<Event> period = periods.get( timeOfEvent );
			period.add( event );
		}

		//
		// now we have everything broken down by period, calculate the stats for each
		//
		Period allEvents = analyzePeriod( eventlist );
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
		logger.info( " === Event Distribution ===" );
		logger.info( "" );		
		logger.info( "    ----------------------------------------------------------------------" );
		logger.info( "    |       | Latency                            | Throughput            |" );
		logger.info( "    | Evts  | Mean   | Med    | S.Dev   | 95%M   | Per-Second | Total    |" );
		logger.info( "    |-------|--------|--------|---------|--------|-----------------------|" );
	  //logger.info( " 12s| 1000  | 1234ms | 1234ms | 1234.56 | 1234ms |  123.4MB/s | 1234.5MB |" );
	  //logger.info( "    ----------------------------------------------------------------------" );
		for( int i = 0; i < distribution.length; i++ )
		{
			Period period = distribution[i];
			String line = String.format( " %2ds| %4d  | %4dms | %4dms | %7.2f | %4dms |  %.9s | %.8s |",
			                             (i+1),
			                             period.count,
			                             period.mean,
			                             period.median,
			                             period.stddev,
			                             period.mean95,
			                             period.getAvgThroughputString(1000),
			                             period.getTotalThroughputString() );

			logger.info( line );
		}

		logger.info( " -------------------------------------------------------------------------" );
		
		// log the information for the full dataset
		String line = String.format( " All| %4d  | %4dms | %4dms | %7.2f | %4dms |  %.9s | %.8s |",
		                             allEvents.count,
		                             allEvents.mean,
		                             allEvents.median,
		                             allEvents.stddev,
		                             allEvents.mean95,
		                             allEvents.getAvgThroughputString(runtime),
		                             allEvents.getTotalThroughputString() );
		logger.info( line );
		logger.info( " -------------------------------------------------------------------------" );

		
		// print a nice event distribution graph
		//   1. figure out the max events in any one period so we know how tall our graph must be
		//   2. loop through each event count level and draw a + if that period had at least that
		//      many events
		//
		//  25|         +
		//  20|         +
		//  15|+        +
		//  10|+  +     +
		//   5|+  +  +  +
		//    |--|--|--|--| 5|--|--|--|--|10|--|--|--|--|15|--|--|--|--|20|
		logger.info( "" );
		logger.info( "Distribution Graph   " );
		logger.info( "---------------------" );

		int maxEvents = 0;
		for( int i = 0; i < distribution.length; i++ )
		{
			if( distribution[i].count > maxEvents )
				maxEvents = distribution[i].count;
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
				if( distribution[j].count >= i )
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

	/**
	 * Analyze the given period to get some basic information about its values
	 */
	private Period analyzePeriod( List<Event> list )
	{
		// TODO add discovery support when we can figure out a simple way to measure latency
		list = trimDiscoverEvents( list );
		
		// fill the period with information about the:
		//   (1) number of evnets
		//   (2) mean of all events
		//   (3) median of all events
		//   (4) standard deviation
		//   (5) mean of all events in 95% range
		//   (6) total received data in bytes
		Period period = new Period();
		period.count = list.size(); // (1) count
		if( period.count == 0 )
			return period; // cya!!

		// get the mean and a list of just the latencies
		int[] latencies = new int[period.count];
		int cumulativeLatency = 0;
		for( int i = 0; i < list.size(); i++ )
		{
			Event event = list.get(i);
			int latency = (int)(event.receivedTimestamp - event.sentTimestamp);
			latencies[i] = latency;
			cumulativeLatency += latency;
			period.datasize += event.datasize; // (6) data size
		}

		// now we can figure out the mean
		period.mean = (int)Math.floor(cumulativeLatency / period.count); // (2) mean
		
		// sort the list so we can figure out the median
		Arrays.sort( latencies );
		
		// figure out standard deviation
		int medianIndex = (int)Math.ceil(period.count/2);
		long sum = 0;
		for( int i = 0; i < latencies.length; i++ )
		{
			sum += Math.pow(latencies[i]-period.mean, 2);

			// is this the median?
			if( i == medianIndex )
				period.median = latencies[i]; // (3) median
		}
		
		double variance = sum / period.count;
		period.stddev = Math.sqrt( variance ); // (4) stddev
		
		// get the mean of values within 2stddev of mean
		int low  = (int)(period.mean - (2*period.stddev));
		int high = (int)(period.mean + (2*period.stddev));
		int number = 0; // number of samples
		sum = 0; // re-purpose sum
		
		for( int i = 0; i < list.size(); i++ )
		{
			if( latencies[i] >= low && latencies[i] <= high )
			{
				sum += latencies[i];
				number++;
			}
		}
		
		period.mean95 = (int)Math.floor(sum / number); // (5) mean95
		
		return period;
	}

	/**
	 * For now we don't count discovery events in our stats -- too hard to measure latency.
	 * This method takes an event set and trims out the discovery events.
	 */
	private List<Event> trimDiscoverEvents( List<Event> source )
	{
		List<Event> trimmed = new ArrayList<Event>();
		for( Event event : source )
		{
			if( event.isDiscover() == false )
				trimmed.add( event );
		}

		return trimmed;
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
			writer.println( "ID, Type, Sender, Size, Latency, Sent Timestamp, Received Timestamp" );
			int count = 0;
			for( Event event : eventlist )
			{
				if( event.type == Event.Type.Discovery )
					continue;
				
				String type = event.type == Event.Type.Reflection ? "Reflection" : "Interaction";
				long latency = event.receivedTimestamp - event.sentTimestamp;
				String line = String.format( "%d, %s, %s, %s, %d, %d, %d",
				                             ++count,
				                             type,
				                             event.sender.toString(),
				                             event.datasize,
				                             latency,
				                             event.sentTimestamp,
				                             event.receivedTimestamp );
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
		public int mean          = 0;   // rounding
		public int median        = 0;
		public double stddev     = 0.0;
		public int mean95        = 0;   // rounding
		public long datasize     = 0;   // in bytes
		
		public String getAvgThroughputString( long periodmillis )
		{
			// datasize is stored in bytes
			// we want to return string like:
			//   123.45KB/s
			//   123.45MB/s
			
			// let's see how much we have so we can figure out the right qualifier
			double totalkb = datasize / 1024;
			double totalmb = totalkb / 1024;
			double totalgb = totalmb / 1024;
			double kbs = totalkb / (periodmillis/1000);
			double mbs = totalmb / (periodmillis/1000);
			double gbs = totalgb / (periodmillis/1000);
			if( gbs > 1 )
				return String.format( "%5.1fGB/s", mbs );
			else if( mbs > 1 )
				return String.format( "%5.1fMB/s", mbs );
			else
				return String.format( "%5.1fKB/s", kbs );
		}
		
		public String getTotalThroughputString()
		{
			// datasize is stored in bytes
			// we want to return string like:
			//   123.45KB
			//   123.45MB
			
			// let's see how much we have so we can figure out the right qualifier
			double kbs = datasize / 1024;
			double mbs = kbs / 1024;
			double gbs = mbs / 1024;
			if( gbs > 1 )
				return String.format( "%6.1fGB", gbs );
			else if( mbs > 1 )
				return String.format( "%6.1fMB", mbs );
			else
				return String.format( "%6.1fKB", kbs );
		}
	}

}
