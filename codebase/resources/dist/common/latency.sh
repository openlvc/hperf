#!/bin/bash

function printUsage()
{
	echo ""
	echo "The latency test is run with any number of federates. Only one federate is"
	echo "designated as the sender. The others are all receivers/responders. The    "
	echo "sender instigates each loops by sending a Ping interaction, to which all  "
	echo "responders reply with a PingAck"
	echo ""
	echo " NOTE: Bundling can have an adverse effect on latency performance. Turn it"
	echo "       off in the RTI.rid file before continuing."
	echo ""
	echo "usage: latency.sh [--arg <value>]"
	echo ""
	echo "    --federate-name      [stirng]    (REQUIRED) Name for this federate"
	echo "    --federation-name    [string]    (optional) Name of the federation we're joining, default hperf"
	echo "    --loops              [number]    (optional) Number of loops we should iterate for, default 20"
	echo "    --peers              [list]      (REQUIRED) Comma-separated list of other federate names"
	echo "    --packet-size        [number]    (optional) Min size of messages. e.g. 1B, 1K, 1M, default 1K"
	echo "    --sender                         (optional) Is this federate the one event sender, default false"
	echo "    --validate-data                  (optional) Validate received contents and log any errors, default false"
	echo "    --log-level          [string]    (optional) TRACE | DEBUG | INFO | WARN | FATAL | OFF, default INFO"
	echo ""
	echo "example: ./latency-sh --federate-name one --peers two,three --loops 10000 --sender"
	echo ""
	exit;
}

###########################
# check for usage request #
###########################
if [ $# = 0 ]
then
    printUsage;
    exit;
fi

if [ $1 = "--help" ]
then
    printUsage;
    exit;
fi

############################################
### (target) execute #######################
############################################
echo -e "Starting HPerf Test Federate (use --help to display usage)"
java -cp ./lib/hperf.jar:./lib/portico/2.1.0/portico.jar hperf.Main --latency-test $*

