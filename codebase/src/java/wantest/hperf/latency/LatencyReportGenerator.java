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
package hperf.latency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import hperf.Storage;
import hperf.TestFederate;
import hperf.Utils;

/**
 * This class takes the information that was gathered during the latency test, does a
 * bit of aggregation and prints a report on the activities.
 */
public class LatencyReportGenerator
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private Logger logger;
	private Storage storage;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public LatencyReportGenerator( Storage storage )
	{
		this.logger = Logger.getLogger( "wt" ); // TODO Fix me for JVM federations
		this.storage = storage;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void printReport()
	{
		// what was the payload size? -- haven't got access to config :(
		int payload = storage.getLatencyEvents().element().getPayloadSize();
		
		// print the overall summary
		logger.info( " =================================" );
		logger.info( " =      Latency Test Report      =" );
		logger.info( " =================================" );
		logger.info( "" );
		logger.info( "Loops:   "+storage.getLatencyEvents().size() );
		logger.info( "Payload: "+Utils.getSizeString(payload) );
		logger.info( "" );
		logger.info( "    ---------------------------------------------------" );
		logger.info( "    |          | Latency                              |" );
		logger.info( "    | Federate | Mean   | Med    | S.Dev     | 95%M   |" );
		logger.info( "    |----------|--------|--------|-----------|--------|" );
	  //logger.info( "    | fedname  | 1234ms | 1234ms | 1234.56   | 1234ms |" );
	  //logger.info( "    ---------------------------------------------------" );

		// analyze the data and get our results
		List<ResultSet> results = compute();
		for( ResultSet result : results )
		{
			String line = String.format( "    | %8s | %4s | %4s | %7s | %4s |",
			                             result.federate,
			                             result.getMeanString(),
			                             result.getMedianString(),
			                             result.getStandardDeviationString(),
			                             result.getMean95String() );

			logger.info( line );
		}
		
		logger.info( "    ---------------------------------------------------" );
		logger.info( "" );
	}

	/**
	 * Analyze the stored data and generate a list of ResultSet objects for each peer.
	 * These will have the latency results for each individual peer so we can see who
	 * has been naught and who has been nice.
	 * 
	 * The returned list will be ordered alphabetically by federate name
	 */
	private List<ResultSet> compute()
	{
		// Create a group of ResultSet objects (one for each federate) and
		// figure out the following for each:
		//     (1) mean
		//     (2) median
		//     (3) standard deviation
		//     (4) mean within 95% confidence interval
		
		//
		// 1. Create a ResultSet for each federate
		//
		int eventCount = storage.getLatencyEvents().size();
		Map<String,ResultSet> results = new HashMap<String,ResultSet>();
		for( TestFederate federate : storage.getPeers() )
		{
			if( federate.isLocalFederate() )
				continue;

			ResultSet result = new ResultSet( federate.getFederateName(), eventCount );
			results.put( result.federate, result );
		}

		//
		// 2. Populate each ResultSet with the latencies for
		//    the associated federate
		//
		// break the events down so we have a list for each federate
		// figure out the mean while we're at it
		int count = -1;
		for( LatencyEvent event : storage.getLatencyEvents() )
		{
			count++;
			long sendtime = event.getSentTimestamp();
			for( TestFederate federate : event.getResponses().keySet() )
			{
				ResultSet result = results.get( federate.getFederateName() );
				result.latencies[count] = (int)(event.getResponses().get(federate) - sendtime);
				result.cumulative += result.latencies[count];
				result.count++;
			}
		}

		//
		// 3. Analyze each ResultSet to determine the headline figures
		//
		// we've now got the list of all events, let's figure out the stats
		for( ResultSet result : results.values() )
		{
			// sort the latencies so we can do our std.dev calculations
			Arrays.sort( result.latencies );

			// figure out the mean
			result.mean = (int)(result.cumulative / result.count);  // (1) mean
			
			// figure out the median and standard deviation
			int medianIndex = (int)Math.floor(result.latencies.length/2);
			long sum = 0;
			for( int i = 0; i < result.latencies.length; i++ )
			{
				if( i == medianIndex )
					result.median = result.latencies[i]; // (2) median
				
				// sdev
				sum += Math.pow(result.latencies[i]-result.mean, 2);
			}
			
			double variance = sum / result.count;
			result.stddev = Math.sqrt( variance ); // (3) stddev

			// eliminate any that are outside 2 confidence intervals
			
			// get the mean of values within 2stddev of mean
			int low  = (int)(result.mean - (2*result.stddev));
			int high = (int)(result.mean + (2*result.stddev));
			int number = 0; // number of samples
			sum = 0; // re-purpose sum so we can calculate mean off only those samples included
			for( int i = 0; i < result.latencies.length; i++ )
			{
				if( result.latencies[i] >= low && result.latencies[i] <= high )
				{
					sum += result.latencies[i];
					number++;
				}
			}
			
			result.mean95 = (int)Math.floor(sum / number); // (4) mean95
		}
		
		// turn the map into a list alphabetically ordered by the name of the federate
		List<ResultSet> finalresults = new ArrayList<ResultSet>( results.values() );
		Collections.sort( finalresults );
		return finalresults;
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	
	private class ResultSet implements Comparable<ResultSet>
	{
		public String federate  = "unknown";
		public int[] latencies  = null; // set in constructor
		public int count        = 0;    // μs
		public long cumulative  = 0;    // μs - total latency for all events
		public int mean         = 0;    // μs
		public int median       = 0;    // μs
		public double stddev    = 0.0;  // μs
		public int mean95       = 0;    // μs
		
		public ResultSet( String federateName, int eventCount )
		{
			this.federate = federateName;
			this.latencies = new int[eventCount];
		}
		
		public int compareTo( ResultSet other )
		{
			return this.federate.compareTo(other.federate);
		}
		
		public String getMeanString(){ return getFormattedString(mean); }
		public String getMedianString(){ return getFormattedString(median); }
		public String getMean95String(){ return getFormattedString(mean95); }
		public String getStandardDeviationString(){ return getFormattedString(stddev); }
		private String getFormattedString( int value )
		{
			long microseconds = TimeUnit.NANOSECONDS.toMicros( value );
			long milliseconds = TimeUnit.NANOSECONDS.toMillis( value );
			if( microseconds > 9999 )
				return String.format( "%4dms", milliseconds );
			else
				return String.format( "%4dμs", microseconds );
		}

		public String getFormattedString( double value )
		{
			long microseconds = TimeUnit.NANOSECONDS.toMicros( (int)value );
			if( microseconds > 9999 )
				return String.format( "%7.2fms", value/(1000*1000) );
			else
				return String.format( "%7.2fμs", value/1000 );
		}

	}
	
}
