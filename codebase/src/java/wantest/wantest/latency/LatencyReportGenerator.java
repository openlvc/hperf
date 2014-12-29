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

import java.util.Map;

import org.apache.log4j.Logger;

import wantest.Storage;
import wantest.TestFederate;
import wantest.events.LatencyEvent;

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
		this.logger = Logger.getLogger( "wantest" );
		this.storage = storage;
	}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	public void printReport()
	{
		// print the overall summary
		logger.info( " =================================" );
		logger.info( " =      Latency Test Report      =" );
		logger.info( " =================================" );
		logger.info( "" );

		temp();
	}

	private void temp()
	{
		long cumulativeLatency = 0;
		long totalResponses = 0;
		for( LatencyEvent event : storage.getLatencyEvents() )
		{
			long sentTime = event.getSentTimestamp();
			Map<TestFederate,Long> responses = event.getResponses();
			for( Long receivedTime : responses.values() )
			{
				cumulativeLatency += (receivedTime-sentTime);
				totalResponses++;
			}
		}
		
		long avg = cumulativeLatency / totalResponses;
		
		logger.info( "Avg Latency: "+avg+"ms ("+totalResponses+" responses)" );
	}
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
