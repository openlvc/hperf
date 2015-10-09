# HLA Performance Testing Federate
----
Welcome to HPerf - a simple HLA performance testing federate.

HPerf provides separate throughput and latency tests, allowing basic benchmarking of RTI
and network performance. HPerf is a Java federate using the IEEE 1516-2010 (HLA Evolved) interface.

**Table of Contents**

  1. How does it Work?
  2. Distribution Structure
  3. Running the Throughput Test
  4. Running the Latency Test
  5. Writing Compatible Federates
  6. Building from Source

----------------------------------------------------

## How does it work?

The test federate is a simple beast. It has its own simple FOM and operates in two modes:

  1. Throughput Testing
  2. Latency Testing

For either mode, you tell each instance what its federate name is, and the name of all the peers
it expects to have in the federation. It will not start an execution until all listed `peers` have
been disovered.

In the Throughput mode, it registers 20 objects and then updates them each once per iteration,
with a run lasting a default of 20 iterations. The federate will also sends 20 interactions each
iteration. Both the number of objects and interactions are configurable, as well as other basic
properties such as the number of loops, size of the packets sent and how information is reported.

In the Latency mode, you start one federate as the sender, while all others are receivers.
It will loop for a default of 20 iterations, sending a `Ping` interaction each loop.
The response times of all other federates are recorded to determine latency. Again, the number
of loops and packet sizes are controllable. Results presented represent two-way latency, that is,
the time it takes to send a message and get a response. To determine point-to-point latency,
halve the presented values.

## Distribution Structure

The following shows the basic structure of the distribution:

```
 `- hperf-1.0.0
   |- RTI.rid                 # RTI configuration file
   |- latency.sh              # Start latency test mode
   |- throughput.sh           # Start throughput test mode
   |- wanrouter.sh            # Start Portico WAN Router (if relevant)
   |- wanrouter.bat           # As above
   `- config
      |- testfom.fed          # Custom testing FOM
   `- lib
      |- hperf.jar            # Core hperf compiled code
      `- portico
         `- <version>
            |- portico.jar    # Portico dependency - built off master
```


## Running the Throughput Test

Shell scripts are provided to start the federate in each of its modes. There are a number of
required parameters to much specify, but the remainder are optional. To start a three federate
test you might have an command such as:

```
./throughput.sh --federate-name one --peers two,three --loops 1000 --packet-size 1K
```

This would start a federate, called `one` which expects two peers: `two`, and `three`.
This federate will complete 1000 iterations registering the default 20 objects and updating
them at each step (as well as sending an interaction). The size of the data stuffed into the
packets is controlled via the `--packet-size <size>` argument.

At the completion of a run you get a result table such as the following:

```
INFO  [hp.per]:  ==================================
INFO  [hp.per]:  =     Throughput Test Report     =
INFO  [hp.per]:  ==================================
INFO  [hp.per]:    Duration: 22.04s (22038ms)
INFO  [hp.per]:    Events Sent / Received:
INFO  [hp.per]:       -Discover:    2000/2000
INFO  [hp.per]:       -Reflect:     2000000/2000000
INFO  [hp.per]:       -Interaction: 0/0
INFO  [hp.per]:
INFO  [hp.per]:    => Federate [lon]
INFO  [hp.per]:         -Receive Window: 20.50s (20497ms)
INFO  [hp.per]:         -Discover:       1000/1000
INFO  [hp.per]:         -Reflect:        1000000/1000000
INFO  [hp.per]:         -Interaction:    0/0
INFO  [hp.per]:
INFO  [hp.per]:    => Federate [per]  ** LOCAL FEDERATE **
INFO  [hp.per]:         -Receive Window: 21.93s (21932ms)
INFO  [hp.per]:         -Discover:       1000/1000
INFO  [hp.per]:         -Reflect:        1000000/1000000
INFO  [hp.per]:         -Interaction:    0/0
INFO  [hp.per]:
INFO  [hp.per]:
INFO  [hp.per]:  === Throughput Table ===
INFO  [hp.per]:
INFO  [hp.per]:          |------------------------------------------------------------|
INFO  [hp.per]:          |        ---- Totals ----         |     -- Throughput --     |
INFO  [hp.per]:          | Window  |  Messages  |   Data   |    Data/s    |   Msg/s   |
INFO  [hp.per]:          |---------|------------|----------|--------------|-----------|
INFO  [hp.per]:   --us-- |     21s |    1000000 |    1.0GB |     45.6MB/s |   45595/s |
INFO  [hp.per]:          |------------------------------------------------------------|
INFO  [hp.per]:      lon |     20s |    1000000 |    1.0GB |     48.8MB/s |   48787/s |
INFO  [hp.per]:          |------------------------------------------------------------|
INFO  [hp.per]:      All |     22s |    2000000 |    2.0GB |     90.8MB/s |   90752/s |
INFO  [hp.per]:          |------------------------------------------------------------|
```

In this report you will see the number of events sent for the local federate, followed by the
number of events receveid for all other peers as well as an overall combined network utilization
value for the federation.

Below is a list of all available command line options to control various aspects of the federate:

```
usage: throughput.sh [--arg <value>]

    --federate-name      [stirng]    (REQUIRED) Name for this federate
    --federation-name    [string]    (optional) Name of the federation we're joining, default hperf
    --loops              [number]    (optional) Number of loops we should iterate for, default 20
    --peers              [list]      (REQUIRED) Comma-separated list of other federate names
    --packet-size        [number]    (optional) Min size of messages. e.g. 1B, 1K, 1M, default 1K
    --validate-data                  (optional) Validate received contents and log any errors, default false
    --callback-immediate             (optional) Use the immediate callback HLA mode (default)
    --callback-evoked                (optional) If specified, used the ticked HLA callback mode
    --loop-wait          [number]    (optional) How long to tick (ms) each loop (if in 'evoked' mode), default 10
                                                This argument has no effect in immediate callback mode
    --log-level          [string]    (optional) TRACE | DEBUG | INFO | WARN | FATAL | OFF, default INFO
    --print-interval     [number]    (optional) Print status update every X iterations, default 10% of loops
    --print-megabits                 (optional) If specified, prints throughput in megabits-per-second
```


## Running the Latency Test

The latency test is executed in much the same way as the throughput test. You specify the name of
the local federate, and the name of all expected peers. The major difference is that for the
latency test you must designate a single federate as the **Sender**. By default, all unspecified
are configured to be receiver/responders. They wait for the `Ping` interactions and then fire back
a response. Without a sender there isn't much to respond to!

An example command line, again with two federates, might look like this:

```
./latency.sh --federate-name one --peers two --loops 10000 --sender
```

After completing a test run you will see a result table similar to the following, with latencies broken down for each responding federate. The given values are two-way latency (round-trip).

```
INFO  [hp.per]:  =================================
INFO  [hp.per]:  =      Latency Test Report      =
INFO  [hp.per]:  =================================
INFO  [hp.per]: 
INFO  [hp.per]: Loops:   10000
INFO  [hp.per]: Payload: 1.00KB
INFO  [hp.per]: 
INFO  [hp.per]:     ---------------------------------------------------
INFO  [hp.per]:     |          | Latency                              |
INFO  [hp.per]:     | Federate | Mean   | Med    | S.Dev     | 95%M   |
INFO  [hp.per]:     |----------|--------|--------|-----------|--------|
INFO  [hp.per]:     |      two |  443μs |  316μs |  682.23μs |  374μs |
INFO  [hp.per]:     ---------------------------------------------------
INFO  [hp.per]: 
INFO  [hp.per]: Resigned from Federation
```

##### Disabling Bundling
***NOTE** Before running the latency test remember to turn bundling off. The test federate sends
          a single message and then waits for responses. As such, if bundling is on the ping
          request will get buffered for at least the bundling timeout time, giving highly
          consistent but slow latency values.*

```
# (4.4) JGroups Bundling Support
#         If you are sending lots of smaller messages, higher overall throughput can be obtained by
#         bundling them together into a fewer number of larger messages. However, doing so comes at
#         the cost of latency. Messages are buffered until either the timeout period (milliseconds)
#         is reached, or the total size of the messages exceeds the specified threshold. Bundling
#         is enabled by default. For latency-critical tasks, disable it or reduce the max-timeout.
#
portico.jgroups.bundling = false
# portico.jgroups.bundling.maxSize = 64K
# portico.jgroups.bundling.maxTime = 30
```

If you are using the Portico WAN facilities, you will have to turn bundling off for it as well:

```
# (5.2) Enable / Disable Bundling
#       Bundling enables higher throughput by grouping together a number of
#       smaller messages and sending them as one. This makes much more efficient
#       use of the network and provides considerable improvements to throughput
#       at a minor cost to latency.
#
#       If enabled, the subsequent options will control how it is applied.
#
#       Default: true
#
portico.wan.bundle.enabled = false
```

##### Warm-up Time
If you are executing the test on a single computer or LAN, you will want to specify a larger
iteration loop value. Given the speed of the interconnect, the JVM tends to be the limiting
factor in these cases as it warms up.

When executing over long-haul links, the latency of the connection is typically well beyond
anything that the JVM will introduce. For example, the Ping time alone between two test VMs
in NYC and Singapore is upwards of 200ms -- a few hundred microseconds isn't going to be
significant in this context.


## Writing Compatible Federates
----
The testing federate uses a simplified custom FOM. This just includes some basic structures
to give us what we need to handle the throughput and latency communications. It is not based
on any simulation. The basic structure is as follows:

```
`- ObjectRoot
   `- TestFederate             // Each federate in the sim registers one
      |- federateName         // String
   `- TestObject
      |- lastUpdated          // long
      |- creator              // String
      |- payload              // byte[]

`- InteractionRoot
   `- ThroughputInteraction
      |- sender               // String
      |- payload              // byte[]
   `- Ping
      |- serial               // int
      |- sender               // String
      |- payload              // byte[]
   `- PingAck
      |- serial               // int
      |- sender               // String
      |- payload              // byte[]

```

The major classes are described below:

  * `TestFederate`: One of these is registered by each federate. This is how
                    we determine whether all peer federates are present or not.
  * `TestObject`: Each throughput federate registers a number of these. We store
                  the creator so we can link events back to a particular federate.
                  Last payload is kept so we can do payload verification if needed.
  * `ThroughputInteraction`: Each throughput federate sends a number of these per
                             loop (same number as registered objects). For these we
                             just care who sent it and what it came with.
  * `Ping`: Used by the latency federate to request responses. Includes a serial
            number that receivers package in their response, and a variable-size
            payload so we can measure changes at different packet fatness.
  * `PingAck`: Sent in response to the reception of a Ping. Basically the same
               except that the `serial` is copied from the request, and the `sender`
               is the local receiver/responder.

##### Throughput Test Lifecycle

Below is some psuedocode taken from the `ThroughputDriver.java` class that controls how
the federate behaves in this mode. If you are writing a compatible federate, you will have
to conform to the expectations (register a `TestFederate` for yourself and update it, use
the specified sync points, etc...)

```
public void execute()
{
    // Enable time policy if we use it
    if( configuration.isTimestepped() )
        this.enableTimePolicy();
    
    // Register test objects
    this.registerObjects();
    
    // Confirm everyone else has registered their test objects and sync up for start
    // Sync point is START_THROUGHPUT_TEST
    this.waitForStart();
    
    //////////////////////////////////////////////////////////////////////////////
    // Loop                                                                     //
    //////////////////////////////////////////////////////////////////////////////
    for( int i = 1; i <= configuration.getLoopCount(); i++ )
    {
        reflectAttributes();
        sendInteractions();
        if( configuration.isTimestepped() )
        {
            advanceTime();
            waitForTimeAdvanceGrant();
        }
    }

    // Wait for everyone to finish their stuff
    // We wait for all expected messages to arrive, and then for the
    // FINISH_THROUGHPUT_TEST sync point to be achieved
    this.waitForFinish();

}
```

##### Latency Test Lifecycle
Below is some psuedocode taken from the `LatencyDriver.java` class that controls how the
federate behaves in this mode. If you are writing a compatible federate, you will have to
conform to the expectations (register a `TestFederate` for yourself and update it, use the
specified sync points, etc...)


```
public void execute()
{
    // Confirm that everyone is ready to proceed
    this.waitForStart();
    
    // Loop
    for( int i = 0; i < configuration.getLoopCount(); i++ )
    {
        if( configuration.isLatencySender() )
            sendInteractionAndWait( i );
        else
            respondToInteractions( i );

        // print where we're up to every now and then
        if( (i+1) % ((int)configuration.getLoopCount()*0.1) == 0 )
            logger.info( "Finished loop ["+(i+1)+"]" );
    }

    // Confirm that everyone is ready to complete
    this.waitForFinish();

    // Print the report - but only if we were the sender
    if( configuration.isLatencySender() )
        new LatencyReportGenerator(storage).printReport();
    else
        logger.info( "Report has been generated by sender federate" );
}
```

## Building from Source
----
Building your own version of the federate from source is intended to be quick and easy.
To build, you will need:

  * Java Development Kit (JDK) 8
  * `JAVA_HOME` environment variable set to point at where JDK is installed

The build system uses Apache Ant, which is included with the source. There are shell scripts/batch
files to run Ant, so to get a build working you run:

```
$ cd hperf/codebase
$ ./ant sandbox
$ cd dist/hperf-1.0.0
$ ./throughput.sh ...
```

This will compile all the source code and assemble a complete build in the `dist/hperf-[version]`
directory. Just move into that directory and run!

