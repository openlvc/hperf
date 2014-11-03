#!/bin/bash

function printUsage()
{
    echo "usage: wantest.sh [--arg <value>]"
    echo ""
    echo "    --federate-name      (REQUIRED) Name for this federate"
    echo "    --federation-name    (optional) Name of the federation we're joining, default wantest"
    echo "    --loops              (optional) Number of loops we should iterate for, default 20"
    echo "    --peers              (REQUIRED) Comma-separated list of other federate names"
	echo "    --packet-size        (optional) Minimum size of each message in KB, default 4"
	echo "    --loop-wait          (optional) How long to stall processing events each loop (ms), default 100"
    echo "    --log-level          (optional) TRACE | DEBUG | INFO | WARN | FATAL | OFF, default INFO"
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

