@echo off

rem ################################
rem # check command line arguments #
rem ################################
:checkargs
if "%1" == "" goto printUsage
if "%1" == "--help" goto printUsage

rem #####################
rem # test for RTI_HOME #
rem #####################
:rtihometest
if "%RTI_HOME%" == "" goto nortihome
if not "%RTI_HOME%" == "" goto run

:nortihome
echo WARNING Your RTI_HOME environment variable is not set
goto run

rem ########################################
rem ### (target) run #######################
rem ########################################
:run
SHIFT
java -cp ".\lib\hperf.jar;.\lib\portico\portico.jar" hperf.Main --throughput-test %1 %2 %3 %4 %5 %6 %7 %8 %9
goto finish


rem ###############################################
rem ### (target) printUsage #######################
rem ###############################################
:printUsage
echo.
echo     Arguments:
echo. 
echo     --federate-name      [stirng]    ^(REQUIRED^) Name for this federate
echo     --federation-name    [string]    ^(optional^) Name of the federation we're joining, default hperf
echo     --loops              [number]    ^(optional^) Number of loops we should iterate for, default 20
echo     --peers              [list]      ^(REQUIRED^) Comma-separated list of other federate names
echo     --packet-size        [number]    ^(optional^) Min size of messages. e.g. 1B, 1K, 1M, default 1K
echo     --validate-data                  ^(optional^) Validate received contents and log any errors, default false
echo     --callback-immediate             ^(optional^) Use the immediate callback HLA mode ^(default^)
echo     --callback-evoked                ^(optional^) If specified, used the ticked HLA callback mode
echo     --loop-wait          [number]    ^(optional^) How long to tick ^(ms^) each loop ^(if in 'evoked' mode^), default 10
echo                                                 This argument has no effect in immediate callback mode
echo     --log-level          [string]    ^(optional^) TRACE ^| DEBUG ^| INFO ^| WARN ^| FATAL ^| OFF, default INFO
echo     --print-interval     [number]    ^(optional^) Print status update every X iterations, default 10% of loops
echo     --print-megabits                 ^(optional^) If specified, prints throughput in megabits-per-second
echo.
echo     example: ./throughput.bat --federate-name one --peers two,three --loops 10000
echo.

:finish

