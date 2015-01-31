/*
 *   Copyright 2015 Calytrix Technologies
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
package wantest.config;

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
	public static synchronized void initializeLogging()
	{
		if( CONFIGURED )
			return;

		// create the appender
		PatternLayout layout = new PatternLayout( "%-5p [%t] %c: %x%m%n" );
		ConsoleAppender appender = new ConsoleAppender( layout, ConsoleAppender.SYSTEM_OUT );
		appender.setThreshold( Level.TRACE ); // output restricted at logger level, not appender
		
		Logger logger = Logger.getLogger( "wantest" );
		logger.addAppender( appender );
		logger.setLevel( Level.INFO );
		
		CONFIGURED = true;
	}
	
	public static Logger getLogger( String federateName )
	{
		return Logger.getLogger( "wantest."+federateName );
	}
}
