#!/bin/bash

RELEASE_OPTS="-DskipTests"
#RELEASE_OPTS=""

# Rollback previous release, if need
if [[ -f "pom.xml.releaseBackup" ]]; then
    echo "**********************************"
    echo "* Rollback previous release..."
    echo "**********************************"
    result=`mvn release:rollback`
    failure=`echo "$result" | grep -m1 -P "\[INFO\] BUILD FAILURE"  | grep -oP "BUILD \w+"`
    # rollback failed
    if [[ ! "_$failure" = "_" ]]; then
        echo "$result" | grep -P "\[ERROR\] "
        exit 1
    fi
    echo "Rollback previous release [OK]"
fi


echo "**********************************"
echo "* Preparing release..."
echo "**********************************"
mvn release:prepare --quiet -Darguments="${RELEASE_OPTS}"
if [[ $? -ne 0 ]]; then
    exit 1
fi
echo "Prepare release [OK]"

echo "**********************************"
echo "* Compiling sources..."
echo "**********************************"
mvn clean install -DskipTests --quiet
echo "Compiling sources [OK]"