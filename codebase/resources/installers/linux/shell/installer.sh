#!/bin/bash

#################################
# Set up the Installer Defaults #
#################################
# build variables -- can be modified by command line arguments
VERSION=5.1.0
USAGE="Usage: installer.sh [-nh] [-i INSTALL_PATH] [-l LAUNCHER_PATH]"
INTERACTIVE=1                      # interactive by default
INSTALL_PATH="/opt/cnr-$VERSION"   # install into optional directory by default
LAUNCHER_PATH="/usr/bin"           # install into /usr/bin by default

####################################
# Parse the command line arguments #
####################################
while getopts hni:l: OPT; do
    case "$OPT" in
        h)
            echo
            echo $USAGE
            echo 
            echo "  -h    Display this help message"
            echo "  -n    Non-interactive mode (user is not prompted for input)"
            echo "  -i    Path to install CNR (default: $INSTALL_PATH)" 
            echo "  -l    Path to install system-wide launchers into (default: $LAUNCHER_PATH)"
            echo
            echo "NOTE: You must be root to install CNR or launchers into system-wide"
            echo "      areas such as /usr/bin"
            exit 0
            ;;
        n)
            INTERACTIVE=0
            ;;
        i)
            INSTALL_PATH=$OPTARG
            ;;
        l)
            LAUNCHER_PATH=$OPTARG
            ;;
        \?)
            echo $USAGE >&2
            echo "Run $0 -h for further details on usage"
            exit -1
            ;;
    esac
done

#################################################################################################
# Let's run this install!                                                                       #
# Print out a welcome header, get license agreement, figure out where to dump stuff and DO EET! #
#################################################################################################
main()
{
    welcome

    if [ $INTERACTIVE -eq 1 ]; then
        eula
        getInstallPaths
    fi

	install
}

####################################################################################
#                           Core Installation Functions                            #
####################################################################################
install()
{
	echo Installing CNR...
	echo  Install path: $INSTALL_PATH
	echo Launcher path: $LAUNCHER_PATH
	echo

	# move the contents of the working directory over to the install directory
	if [ ! -d "$INSTALL_PATH" ]; then
  		echo "[install] creating install directory"
  		mkdir -p $INSTALL_PATH
	fi

	# copy the contents over to the install directory
	echo "[install] moving payload to install directory"
	mv * $INSTALL_PATH/

	# remove the unnecessary files in the install directory
    echo "[install] cleaning up temp files"
	rm $INSTALL_PATH/LICENSE.cnr    # plain-text license (non-editable is provided)
	rm $INSTALL_PATH/installer.sh   # this installation script

	# set up the sym-links if required
	if [ ! $BIN_PATH = "" ]; then
        echo "[install] creating launcher symlinks in $BIN_PATH"
		for file in $INSTALL_PATH/bin/*
		do
			temp=$(basename $file)
			ln -sf $INSTALL_PATH/bin/$temp $BIN_PATH/$temp
		done
	fi

    echo "[install] Installation complete. Installed into [$INSTALL_PATH]"
}

####################################################################################
#                                  Welcome Header                                  #
####################################################################################
welcome()
{
	# print a welcome message
	echo
	echo "=================================================================="
	echo "Calytrix Comm Net Radio v$VERSION Installer"
	echo "Copyright (c) Calytrix Technologies $(date +%Y)"
	echo "=================================================================="
	echo
}

####################################################################################
#                       End User License Agreement Functions                       #
####################################################################################
eula()
{
	echo "By installing this software you must agree the terms of the End User License Agreement"
    yesno "Do you wish to review the EULA?" "y"
    if [ "$YNRESULT" == "y" ]; then
        cat LICENSE.cnr | less
    fi

    echo "Do you accept the license agreement?"
    yesno "Do you wish to accept the license agreement?" "y"
    if [ "$YNRESULT" == "n" ]; then
    	echo ":( Maybe next time"
        exit 0;
    fi
}

####################################################################################
#                           Installation Path Functions                            #
####################################################################################
getInstallPaths()
{
	# figure out where they want to install
    readstring "Enter directory to install in:" $INSTALL_PATH
    INSTALL_PATH=$READRESULT
    echo ""

    # figure out whether they want to create system-wide symlinks
    yesno "Do you want to install CNR launchers for all users?" "y"
    if [ "$YNRESULT" == "y" ]
    then
	    readstring "Where do you want to install the system-wide launchers?" "/usr/bin"
    	BIN_PATH=$READRESULT
    else
    	BIN_PATH=""
    fi

    echo ""
}

####################################################################################
#                             General Helper Functions                             #
####################################################################################
#
# Params
# - $1 The prompt to display to the user
# - $2 The default value
#
# Return
# - Sets the variable $READRESULT to the entered value
#
readstring()
{
    echo -e "$1"
    printf "[$2] "
    read READRESULT

    if [ "$READRESULT" == "" ]; then
        READRESULT=$2
    fi
}

#
# Prompts the user for yes/no input
#
# Params
# - $1 The prompt to display to the user
# - $2 The default option (y or n)
#
# Return
# - Sets the variable $YNRESULT to either "y" or "n"
#
yesno()
{
    local VALIDINPUT=0
    local PROMPT=
    local RESULT=
    if [ "$2" == "y" ]; then
        PROMPT="[Yn] "
    else
        PROMPT="[yN] "
    fi

    while [ $VALIDINPUT -eq 0 ]; do 
        echo -e "$1"
        printf $PROMPT
        read RESULT

        if [ "$RESULT" == "y" ] || [ "$RESULT" == "Y" ]; then
            YNRESULT=y
            VALIDINPUT=1
        elif [ "$RESULT" == "n" ] || [ "$RESULT" == "N" ]; then
            YNRESULT=n
            VALIDINPUT=1
        elif [ "$RESULT" == "" ]; then
            YNRESULT=$2
            VALIDINPUT=1
        else
            error "Unexpected input: please enter either 'y' or 'n'"
            echo
        fi
    done
    printf "\n"
}

### Run things ###
main
exit 0
