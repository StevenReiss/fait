#! /bin/csh -f

fgrep --binary-files=text :S: $1 | sed -e 's&FAIT\:\*\:S\:\ &&' | sort --field-separator=, --key=2nr >&! stats.csv
