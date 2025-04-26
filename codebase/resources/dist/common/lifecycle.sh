#!/bin/bash

function printUsage()
{
	echo ""
	echo "The lifecycle test is typically run with two federates. One that is the   "
	echo "sender and will constantly join and resign. The other is the responder.   "
	echo "They will stay present in the simulation continuously, responding to ping "
	echo "messages received from the sender".
	echo ""
	echo "The purpose of the test is to ensure that when joining a federation, the  "
	echo "group management features of the federate are maintained. This is not a   "
	echo "speed test, but rather a stability test to be run over time.              "
	echo ""
	echo " NOTE: Bundling can have an adverse effect on test performance. Turn it"
	echo "       off in the RTI.rid file if you'd like to speed things up a bit."
	echo ""
	echo "usage: lifecycle.sh [--arg <value>]"
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
	echo "example: ./lifecycle-sh --federate-name one --peers two --loops 10000 --sender"
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

############################################
### (target) execute #######################
############################################
echo -e "Starting HPerf Test Federate (use --help to display usage)"

# Change : to ; on classpath if Git Bash... because Windows
classpath="./lib/hperf.jar:./lib/portico/portico.jar"
if [ $(uname) != "Linux" ]; then
	classpath=$( echo "$classpath" | tr : \; )
fi

java -cp $classpath hperf.Main --lifecycle-test $*
