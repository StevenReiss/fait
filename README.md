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


