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
package hperf.config;

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
	private boolean jvmFederation;
	private boolean jvmMaster; // is this the "master" federate?
	
	// execution properties
	private int loopCount;
	private int loopWait;
	private int objectCount; // number of objects we'll create
	private int interactionCount; // number of interactions to send each iteration
	private int packetSize;  // the minimum size of each update in kb
	private boolean validateData;
	private List<String> peers;
	private boolean sender;  // is this the sender for the latency or lifecycle federates?
	private int printInterval;  // number of loops at which to print throughput federate status

	private boolean runThroughputTest;
	private boolean runLatencyTest;
	private boolean runLifecycleTest;
	private boolean isImmediateCallbackMode;
	private boolean isTimestepped;

	private boolean printEventLog;
	private boolean printMegabits;
	private String csvFile;

	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------
	public Configuration()
	{
		this.configurationFile = new File( "hperf.config" );
		this.loglevel = "INFO";
		
		// federate settings
		this.federationName = "test-fed";
		this.federateName = "hperf1";
		this.jvmFederation = false;
		this.jvmMaster = false;
		
		// execution properties
		this.loopCount = 20;
		this.loopWait = 10;
		this.objectCount = 20;
		this.interactionCount = -1; // if -1, will default to same as objectCount
		this.packetSize = 1000;
		this.validateData = false;
		this.peers = new ArrayList<String>();
		this.sender = false;
		this.printInterval = -1;

		// default to run neither test unless instructed
		this.runThroughputTest = false;
		this.runLatencyTest = false;
		this.runLifecycleTest = false;
		
		// what is our callback mode? Immediate or Evoked?
		this.isImmediateCallbackMode = true;
		this.isTimestepped = false;
		
		this.printEventLog = false;
		this.printMegabits = false;
		this.csvFile = null;
	}
	
	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * Make a copy of this configuration. We do this to support the execution of JVM
	 * federations, where we take a single configuration in, but need to generate a
	 * unique one for each federate.
	 */
	public Configuration copy( String federateName, List<String> peers )
	{
		Configuration temp = new Configuration();
		temp.configurationFile = this.configurationFile;
		temp.loglevel = this.loglevel;
		
		// federate settings
		temp.federationName = this.federationName;
		temp.federateName = federateName;
		temp.jvmFederation = this.jvmFederation;
		temp.jvmMaster = this.jvmMaster;
		
		// execution properties
		temp.loopCount = this.loopCount;
		temp.loopWait = this.loopWait;
		temp.objectCount = this.objectCount;
		temp.interactionCount = this.interactionCount;
		temp.packetSize = this.packetSize;
		temp.validateData = this.validateData;
		temp.peers = peers;

		// default to run neither test unless instructed
		temp.runThroughputTest = this.runThroughputTest;
		temp.runLatencyTest = this.runLatencyTest;
		
		// what is our callback mode? Immediate or Evoked?
		temp.isImmediateCallbackMode = this.isImmediateCallbackMode;
		
		temp.printEventLog = this.printEventLog;
		temp.printMegabits = this.printMegabits;
		temp.csvFile = this.csvFile;
		
		return temp;
	}
	
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
	
	public boolean isJvmFederation()
	{
		return this.jvmFederation;
	}

	/** Is this the "master" federate for a JVM federation. If so, this is
	    the only one that should do any logging */
	public boolean isJvmMaster()
	{
		return this.jvmMaster;
	}
	
	public void setJvmMaster( boolean master )
	{
		this.jvmMaster = master;
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
	 * The number of interactions that this federate should sent each iteraction.
	 * This should be the same across all federates in a test run, as other federates
	 * will base their expectations on this. Defaults to the same value as returned
	 * by {@link #getObjectCount()} unless specifically specified on the commadn line.
	 * @return
	 */
	public int getInteractionCount()
	{
		return this.interactionCount == -1 ? this.objectCount : this.interactionCount;
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
	/** Should this federate send the ping requests for the latency and lifecycle tests? */
	public boolean isSender()
	{
		return this.sender;
	}

	/** Set whether or not this federate is a latency/lifecycle sender. Only done to support
	    federates running using the JVM binding */
	public void setSender( boolean sender )
	{
		this.sender = sender;
	}

	/** Number of iterations that should happen before we print a status update for the
	    throughput test. Defaults to -1, which is "auto" based on number of iterations. */
	public int getPrintInterval()
	{
		return this.printInterval;
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

	/** Should the lifecycle test be run? */
	public boolean isLifecycleTestEnabled()
	{
		return this.runLifecycleTest;
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

	/** Print results in megabits-per-second, or megabytes-per-second */
	public boolean isPrintMegabits()
	{
		return this.printMegabits;
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
    		--federate-name hperf1
    		--federation-name hperf
    		--peers hperf2,hperf3
    		--loops 20
    		--packet-size 32B/KB/MB
    		--loop-wait 100
    		--print-event-log
    		--jvm
		*/

		int count = 0;
		while( count < args.length )
		{
			String argument = args[count];
			if( argument.startsWith("--") == false )
				throw new RuntimeException( "Unknown argument: ["+argument+"]" );
			
			if( argument.startsWith("--loglevel") || argument.startsWith("--log-level") )
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
			
			if( argument.startsWith("--jvm") )
			{
				this.jvmFederation = true;
				count++;
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
			
			if( argument.startsWith("--objects") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.objectCount = Integer.parseInt( args[count+1] );
				count += 2;
				continue;
			}
			
			if( argument.startsWith("--interactions") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.interactionCount = Integer.parseInt( args[count+1] );
				count += 2;
				continue;
			}

			if( argument.startsWith("--sender") )
			{
				this.sender = true;
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
			
			if( argument.startsWith("--lifecycle-test") )
			{
				this.runLifecycleTest = true;
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
			
			if( argument.startsWith("--print-megabits") )
			{
				this.printMegabits = true;
				count++;
				continue;
			}

			if( argument.startsWith("--print-interval") )
			{
				validateArgIsValue( argument, args[count+1] );
				this.printInterval = Integer.parseInt( args[count+1] );
				count += 2;
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
