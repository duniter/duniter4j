#!/bin/bash

VERSION=$1
OLD_VERSION=$2

if [ "${VERSION}" == "" ]; then
        echo "ERROR: Missing version argument !"
        echo " "
        echo "usage: sudo ./update-es.sh <version> [<old_version>]"
        exit
fi
if [ "${OLD_VERSION}" == "" ]; then
        OLD_VERSION=`ps -efl | grep duniter4j-es | grep g1/lib | sed -r 's/.*duniter4j-es-([0-9.]+)-g1.*/\1/g'`
        if [ "${OLD_VERSION}" == "" ]; then
                echo "Error: unable to known previous version"
                exit
        fi
fi

READLINK=`which readlink`
if [ -z "$READLINK"  ]; then
  message "Required tool 'readlink' is missing. Please install before launch \"$0\" file."
  exit 1
fi

# ------------------------------------------------------------------
# Ensure BASEDIR points to the directory where the soft is installed.
# ------------------------------------------------------------------
SCRIPT_LOCATION=$0
if [ -x "$READLINK" ]; then
  while [ -L "$SCRIPT_LOCATION" ]; do
    SCRIPT_LOCATION=`"$READLINK" -e "$SCRIPT_LOCATION"`
  done
fi 

export BASEDIR=`dirname "$SCRIPT_LOCATION"`                                                                                                                                                                                                        
cd $BASEDIR 

echo "--- Downloading duniter4j-es-standalone v$VERSION... ----------------------"

if [ -f "downloads/duniter4j-es-${VERSION}-standalone.zip" ]; then
        echo "...removing file, as it already exists in ./downloads/duniter4j-es-${VERSION}-standalone.zip" 
        rm ./downloads/duniter4j-es-${VERSION}-standalone.zip
fi

if [ ! -e "downloads" ]; then
        mkdir downloads
fi

cd downloads
wget -kL https://github.com/duniter/duniter4j/releases/download/duniter4j-${VERSION}/duniter4j-es-${VERSION}-standalone.zip
cd ..

if [ -f "downloads/duniter4j-es-${VERSION}-standalone.zip" ]; then
        echo ""
else
        echo "Error: unable to dowlonad this version!"
        exit -1
fi

echo "--- Installating duniter4j-es v$VERSION... ---------------------"
if [ -d "/opt/duniter4j-es-${VERSION}-g1" ]; then
        echo "Error: Already installed in /opt/duniter4j-es-${VERSION}-g1 !"
        exit -1
fi

unzip -o ./downloads/duniter4j-es-${VERSION}-standalone.zip
mv duniter4j-es-${VERSION} duniter4j-es-${VERSION}-g1
sudo mv duniter4j-es-${VERSION}-g1 /opt/
sudo rm /opt/duniter4j-es-g1
sudo ln -s /opt/duniter4j-es-${VERSION}-g1 /opt/duniter4j-es-g1

mkdir /opt/duniter4j-es-${VERSION}-g1/data
mv /opt/duniter4j-es-${VERSION}-g1/config/elasticsearch.yml /opt/duniter4j-es-${VERSION}-g1/config/elasticsearch.yml.ori

./stop-es-nodes.sh

if [ "$OLD_VERSION" != "$VERSION" ];
then
        echo "--- Restoring files (data+config) from previous version $OLD_VERSION... ---------------------"
        tar -cvf /opt/duniter4j-es-${OLD_VERSION}-g1/data/save.tar.gz /opt/duniter4j-es-${OLD_VERSION}-g1/data/g1-*
        mv /opt/duniter4j-es-${OLD_VERSION}-g1/data/g1-* /opt/duniter4j-es-${VERSION}-g1/data  
        cp /opt/duniter4j-es-${OLD_VERSION}-g1/config/elasticsearch.yml /opt/duniter4j-es-${VERSION}-g1/config
fi

#./start-es-nodes.sh

echo "--- Successfully installed duniter4j-es v$VERSION ! -------------"
echo ""

