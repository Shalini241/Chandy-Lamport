#!/bin/bash

# Change this to your netid
netid=sxg220033

# Root directory of your project
PROJDIR=/home/013/s/sx/sxg220033/OS-Asg1

# Directory where the config file is located on your local system
CONFIGLOCAL=/home/013/s/sx/sxg220033/OS-Asg1/configuration.text

# Directory your java classes are in
BINDIR=$PROJDIR

# Your main project class
PROG=NodeWrapper

n=0

# Read the first line of the configuration file to get the number of iterations
i=$(head -n 1 "$CONFIGLOCAL" | awk '{print $1}')

# Read the configuration lines starting from the second line
tail -n +2 $CONFIGLOCAL | while read line
do
    p=$(echo $line | awk '{print $1}')
    host=$(echo $line | awk '{print $2}')

    echo "ssh onto dc$n"
      # execute a remote command on the dc machine. Starts the jar and redirects the output to a log file.
    ssh $netid@$host \
      "cd ~/OS-Asg1; \
      java $PROG $n" &

    n=$((n + 1))

    if [[ $n -ge $i ]]
    then
        break
    fi
done
