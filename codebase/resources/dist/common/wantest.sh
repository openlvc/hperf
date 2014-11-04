#!/bin/bash

function printUsage()
{
    echo "usage: wantest.sh [--arg <value>]"
    echo ""
    echo "    --federate-name    [stirng]    (REQUIRED) Name for this federate"
    echo "    --federation-name  [string]    (optional) Name of the federation we're joining, default wantest"
    echo "    --loops            [number]    (optional) Number of loops we should iterate for, default 20"
    echo "    --peers            [list]      (REQUIRED) Comma-separated list of other federate names"
	echo "    --packet-size      [number]    (optional) Minimum size of each message in KB, default 4"
	echo "    --loop-wait        [number]    (optional) How long to stall processing events each loop (ms), default 100"
    echo "    --log-level        [string]    (optional) TRACE | DEBUG | INFO | WARN | FATAL | OFF, default INFO"
	echo "    --print-event-log              (optional) If specified, prints a list of vital stats on all events"
    echo "    --export-cvs       [string]    (optional) File to dump event list to. No export if not provided"
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

######################
# test for JAVA_HOME #
######################
JAVA=java
if [ "$JAVA_HOME" = "" ]
then
	echo WARNING Your JAVA_HOME environment variable is not set! Fallback on system path.
	#exit;
else
        JAVA=$JAVA_HOME/bin/java
fi

############################################
### (target) execute #######################
############################################
echo -e "Starting WAN Test Federate (use --help to display usage)"
$JAVA -cp ./lib/wantest.jar:./lib/portico/2.0.1/portico.jar wantest.Main $*

