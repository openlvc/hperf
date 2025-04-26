#!/bin/bash

function printUsage()
{
	echo "usage: throughput.sh [--arg <value>]"
	echo ""
	echo "    --federate-name      [stirng]    (REQUIRED) Name for this federate"
	echo "    --federation-name    [string]    (optional) Name of the federation we're joining, default hperf"
	echo "    --loops              [number]    (optional) Number of loops we should iterate for, default 20"
	echo "    --peers              [list]      (REQUIRED) Comma-separated list of other federate names"
	echo "    --packet-size        [number]    (optional) Min size of messages. e.g. 1B, 1K, 1M, default 1K"
	echo "    --validate-data                  (optional) Validate received contents and log any errors, default false"
	echo "    --callback-immediate             (optional) Use the immediate callback HLA mode (default)"
	echo "    --callback-evoked                (optional) If specified, used the ticked HLA callback mode"
	echo "    --loop-wait          [number]    (optional) How long to tick (ms) each loop (if in 'evoked' mode), default 10"
	echo "                                                This argument has no effect in immediate callback mode"
	echo "    --log-level          [string]    (optional) TRACE | DEBUG | INFO | WARN | FATAL | OFF, default INFO"
	echo "    --print-interval     [number]    (optional) Print status update every X iterations, default 10% of loops"
	echo "    --print-megabits                 (optional) If specified, prints throughput in megabits-per-second"
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

# Change : to ; on classpath if Git Bash... because Windows
classpath="./lib/hperf.jar:./lib/portico/portico.jar"
if [ $(uname) != "Linux" ]; then
	classpath=$( echo "$classpath" | tr : \; )
fi

java -cp $classpath hperf.Main --throughput-test $*
