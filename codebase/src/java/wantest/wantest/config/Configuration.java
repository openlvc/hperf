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
package wantest.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;

public class Configuration
{
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private File configurationFile;
	private String loglevel;

	// federate settings
	private String federationName;
	private String federateName;
	
	// execution properties
	private int loopCount;
	private int loopWait;
	private int objectCount; // number of objects we'll create
	private int packetSize;  // the minimum size of each update in kb
	private boolean validateData;
	private List<String> peers;
	private boolean latencySender;  // is this the sender for the latency federate?

	private boolean runThroughputTest;
	private boolean runLatencyTest;
	private boolean isImmediateCallbackMode;
	private boolean isTimestepped;

	private boolean printEventLog;
	private String csvFile;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Configuration()
	{
		this.configurationFile = new File( "wantest.config" );
		this.loglevel = "INFO";
		
		// federate settings
		this.federationName = "WAN Test Federation";
		this.federateName = "wantest1";
		
		// execution properties
		this.loopCount = 20;
		this.loopWait = 10;
		this.objectCount = 20;
		this.packetSize = 1000;
		this.validateData = false;
		this.peers = new ArrayList<String>();
		this.latencySender = false;

		// default to run neither test unless instructed
		this.runThroughputTest = false;
		this.runLatencyTest = false;
		
		// what is our callback mode? Immediate or Evoked?
		this.isImmediateCallbackMode = true;
		this.isTimestepped = false;
		
		this.printEventLog = false;
		this.csvFile = null;
	}
	
	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	
	////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////// Accessor and Mutator Methods ///////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	// General
	public String getLogLevel()
	{
		return this.loglevel;
	}

	public File getConfigurationFile()
	{
		return this.configurationFile;
	}
	
	// Basic Federation Properties
	public String getFederationName()
	{
		return this.federationName;
	}
	
	public String getFederateName()
	{
		return this.federateName;
	}
	

	// Execution Properties
	/**
	 * The number of times we'll loop before leaving the federation
	 */
	public int getLoopCount()
	{
		return this.loopCount;
	}

	/**
	 * The period of time (ms) to stall while processing callbacks at the end of each loop.
	 * Defaults to 100ms.
	 */
	public int getLoopWait()
	{
		return this.loopWait;
	}
	
	/**
	 * The number of object that this federate should create and publish.
	 * This should be the same across all federates in a given test run,
	 * as this value is also used to set any expectations about what the
	 * test federate will receive from its peers.
	 */
	public int getObjectCount()
	{
		return this.objectCount;
	}

	/**
	 * Gets the minimum size for each packet in KB. This is achieved by using
	 * a single attribute/parameter in each message that we stuff with random
	 * data equal to this size. The actual size of packets is slighlty larger
	 * as we send additional information (federate name and timestamp).
	 */
	public int getPacketSize()
	{
		return this.packetSize;
	}

	/**
	 * If this is set to true, for each message received, we should validate
	 * the contents of the data to ensure it is as expected. 
	 */
	public boolean getValidateData()
	{
		return this.validateData;
	}
	
	/**
	 * A list of all the peer federates that will be part of this test run.
	 * The returned list contains the name of the federates.
	 */
	public List<String> getPeers()
	{
		return this.peers;
	}

	// Execution and Misc Options
	/** Is this federate the one that sends the ping requests for the latency test? */
	public boolean isLatencySender()
	{
		return this.latencySender;
	}
	
	/** Should the throughput test be run? */
	public boolean isThroughputTestEnabled()
	{
		return this.runThroughputTest;
	}
	
	/** Should the latency test be run? */
	public boolean isLatencyTestEnabled()
	{
		return this.runLatencyTest;
	}

	/** Are we using the immediate callback mode? (default to true) */
	public boolean isImmediateCallback()
	{
		return this.isImmediateCallbackMode;
	}

	/** Are we using the evoked callback mode? (default to false) */
	public boolean isEvokedCallback()
	{
		return this.isImmediateCallbackMode == false;
	}

	/** Should the federate use timestepping? Latency federate always will */
	public boolean isTimestepped()
	{
		return this.isTimestepped;
	}
	
	public boolean getPrintEventLog()
	{
		return this.printEventLog;
	}

	public boolean getExportCSV()
	{
		return this.csvFile != null;
	}
	
	public String getCSVFile()
	{
		return this.csvFile;
	}

	////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// Configuration File Loading Methods ////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////

	// Configuration Load Methods
	public void loadCommandLine( String[] args )
	{
		/*
    		--federate-name wantest1
    		--federation-name wantest
    		--peers wantest2,wantest3
    		--loops 20
    		--packet-size 32B/KB/MB
    		--loop-wait 100
    		--print-event-log
		*/

		int count = 0;
		while( count < args.length )
		{
			String argument = args[count];
			if( argument.startsWith("--") == false )
				throw new RuntimeException( "Unknown argument: ["+argument+"]" );
			
			if( argument.startsWith("--loglevel") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.loglevel = args[count+1];
				count += 2; // skip over the next
				continue;
			}
			
			if( argument.startsWith("--config") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.configurationFile = new File( args[count+1] );
				if( this.configurationFile.exists() == false )
					throw new RuntimeException( "Configuration file doesn't exist: "+args[count+1] );
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--federate-name") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.federateName = args[count+1];
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--federation-name") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.federationName = args[count+1];
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--loops") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.loopCount = Integer.parseInt( args[count+1] );
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--packet-size") )
			{
				validateArgIsValue( argument, args[count+1] );
				String packetString = args[count+1];
				if( packetString.toUpperCase().endsWith("K") )
				{
					int size = Integer.parseInt( packetString.substring(0,packetString.length()-1) );
					this.packetSize = size * 1024;
				}
				else if( packetString.toUpperCase().endsWith("M") )
				{
					int size = Integer.parseInt( packetString.substring(0,packetString.length()-1) );
					this.packetSize = size * 1024 * 1024;
				}
				else if( packetString.toUpperCase().endsWith("B") )
				{
					int size = Integer.parseInt( packetString.substring(0,packetString.length()-1) );
					this.packetSize = size;
				}
				else
				{
					// assume bytes if there is not information
					this.packetSize = Integer.parseInt( packetString );
				}
				
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--validate-data") )
			{
				this.validateData = true;
				count++;
				continue;
			}

			if( argument.startsWith("--peers") )
			{
				validateArgIsValue( argument, args[count+1] );
				StringTokenizer tokenizer = new StringTokenizer( args[count+1], "," );
				while( tokenizer.hasMoreTokens() )
					peers.add( tokenizer.nextToken() );
				
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--sender") )
			{
				this.latencySender = true;
				count++;
				continue;
			}
			
			if( argument.startsWith("--throughput-test") )
			{
				this.runThroughputTest = true;
				count++;
				continue;
			}
			
			if( argument.startsWith("--latency-test") )
			{
				this.runLatencyTest = true;
				count++;
				continue;
			}
			
			if( argument.startsWith("--callback-immediate") )
			{
				this.isImmediateCallbackMode = true;
				count++;
				continue;
			}

			if( argument.startsWith("--callback-evoked") )
			{
				this.isImmediateCallbackMode = false;
				count++;
				continue;
			}
			
			if( argument.startsWith("--timestepped") )
			{
				this.isTimestepped = true;
				count++;
				continue;
			}

			if( argument.startsWith("--loop-wait") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.loopWait = Integer.parseInt( args[count+1] );
				count += 2;
				continue;
			}

			if( argument.startsWith("--print-event-log") )
			{
				this.printEventLog = true;
				count++;
				continue;
			}
			
			if( argument.startsWith("--export-csv") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.csvFile = args[count+1];
				count += 2;
				continue;
			}
			
			throw new RuntimeException( "Unknown argument: "+argument );
		}
		
	}

	/** Make sure no numpty left the actual argument companion value off a call by ensuring that
	 *  the given argument is not actually a new argument declaration (that is, doesn't start
	 *  with "--").
	 *  
	 * @param key The key we're already working while checking for a value
	 * @param arg The argument we expect to be a value, not a key
	 */
	private void validateArgIsValue( String key, String arg )
	{
		if( arg.startsWith("--") )
			throw new RuntimeException( "Was expecting a value for "+key+", instead found: "+arg );
	}
	
	public void loadConfigurationFile()
	{
		// load the file into a properties set
		Properties properties = new Properties();
		try
		{
			FileInputStream fis = new FileInputStream( configurationFile );
			properties.load( fis );
		}
		catch( FileNotFoundException fnfe )
		{
			System.err.println( "Couldn't find config file, falling back on defaults: "+
			                    configurationFile.getAbsolutePath() );
		}
		catch( IOException ioex )
		{
			throw new RuntimeException( "Error reading config file: "+configurationFile, ioex );
		}

		System.err.println( "No config file load yet :(" );
	}
	
	//----------------------------------------------------------
	//                     STATIC METHODS
	//----------------------------------------------------------
}
