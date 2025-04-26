/*
 *   Copyright 2015 The OpenLVC Group
 *
 *   This file is part of hperf.
 *
 *   NOTICE:  All information contained herein is, and remains
 *            the property of The OpenLVC Group.
 *            The intellectual and technical concepts contained
 *            herein are proprietary to The OpenLVC Group.
 *            Dissemination of this information or reproduction of
 *            this material is strictly forbidden unless prior written
 *            permission is obtained from The OpenLVC Group.
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package hperf.config;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class LoggingConfigurator
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	private static volatile boolean CONFIGURED = false;

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	private LoggingConfigurator() {}

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
	public static synchronized void initializeLogging( String level )
	{
		if( CONFIGURED )
			return;

		// create the appender
		PatternLayout layout = new PatternLayout( "%-5p [%c]: %x%m%n" );
		ConsoleAppender appender = new ConsoleAppender( layout, ConsoleAppender.SYSTEM_OUT );
		appender.setThreshold( Level.TRACE ); // output restricted at logger level, not appender
		
		Logger logger = Logger.getLogger( "hp" );
		logger.addAppender( appender );
		logger.setLevel( Level.toLevel(level) );
		
		CONFIGURED = true;
	}
	
	public static Logger getLogger( String federateName )
	{
		return Logger.getLogger( "hp."+federateName );
	}
}
