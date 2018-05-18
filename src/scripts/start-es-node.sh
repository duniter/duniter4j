#!/bin/bash


PID=`ps -efl | grep duniter4j-es | grep g1/lib | awk '/^([0-9]+) ([^\s]) ([a-zA-Z0-9]+) ([0-9]+).*/ {printf "%s", $4}'`

if [ "$PID" != "" ];
then
        echo "Error: ES node already started!"
        exit -1
else
        cd /opt/duniter4j-es-g1/bin
        ./elasticsearch -d
        echo "ES node started !"
        echo "...to follow log: tail -f /opt/duniter4j-es-g1/logs/g1-es-data.log"
fi

