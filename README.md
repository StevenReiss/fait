Welcome to FAIT.

FAIT is a flow-analysis framework designed to be used by a programming
environment.  It is designed to run incrementally as the user edits,
providing continuing feedback on potential problems.  Currently the
system concentrates on security-related problems.

Building FAIT will require ivy.  Also you will need karma, faitbb,
and fredit.

FAIT is currently working with the Code Bubbles environment.  The source
for Code Bubbles is available from gitHub.

To build FAIT, first build IVY and Code Bubbles.  FAIT should be installed
in the same directory (i.e. ../ivy and ../bubbles should point to ivy and
code bubbles respectively).

Note that running fait requires karma.	Other associated projects (which
should be installed before trying to compile fait, in are faitbb (the
code bubbles user interface to fait) and fredit (a preliminary interface
for editing fait resource files).  All of these should be cloned from
github before attempting to compile.

Once all these are installed (within a common directory with ivy and
code bubbles), just run ant in the root directory to compile everything
and install the fait plugin in the bubbles environment.


===========================================================

Dependencies on Code Bubbles (for those wanting to port):

Note that all of these use the message bus (MINT).


* User interface (faitbb -- windows to show possible flow errors and how
	they come abount)

* Commands sent to Bedrock (back end)
   + STARTFILE (get contents of file as it currently exists)
   + OPENPROJECT (get class paths and list of classes in a project)
   + PROJECTS (get list of projects in the environment)

* Messages received from Bedrock (back end)
   + EDITERROR/FILEERROR (note errors or lack thereof in a file after edit)
   + EDIT (note changes to a file as they are made)
   + RESORUCE (note new files/removed files) -- currently not used
   + STOP (exit)

* Messages received from Code Bubbles (front end)
   + EXIT (exit: note this is superfluous as back end sends STOP)

* Commands sent from front end:
   + PING
   + EXIT
   + BEGIN : start flow analysis session
   + REMOVE : remove flow session
   + ADDFILE : add a file to be considered in source form
   + ANALYZE : start actual analysis
   + FLOWQUERY : issue a flow query (back slice)
   + CHANGEQUERY : issue a query to determine what variables have changed values
   + QUERY : issue a query on how a identified flow problem occurred
   + RESOURCES : list resource (fait.xml) files used
   + REFLECTION : list uses of reflection found in analysis
   + PERFORMANCE : return counts related to performance
   + CRITICAL : identify routines that are critical to detected flow errors
   + VARQUERY : determine where a variable could have been set
   + STACKSTART : find starting point for evaluation for BREPAIR

* Messages sent out (FAITEXEC TYPE=<cmd> ...)
   + ANALYSIS : start/stop analysis or analysis result
