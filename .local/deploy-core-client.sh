#!/bin/bash

set JAVA_HOME=/usr/lib/jvm/java-8-oracle
CESIUM_PLUS_POD_DIR="${HOME}/git/duniter/cesium-plus-pod"
DEPLOY_DIR="${CESIUM_PLUS_POD_DIR}/cesium-plus-pod-assembly/target/es-run-home/plugins/cesium-plus-pod-core"

# Go to project root
cd ..

echo "***************************************"
echo " Compiling core-client... "

# Remove old JAR
rm duniter4j-core-client/target/*.jar

# Compile the core-client
mvn install -pl duniter4j-core-client --quiet -DskipTests
if [[ $? -ne 0 ]]; then
    exit 1
fi
echo " Successfully compiled ! "

echo "***************************************"
echo " Installing into Cesium+ pod (target assembly)... "

# Copy jar
mkdir -p ${DEPLOY_DIR}
if [[ $? -ne 0 ]]; then
    exit 1
fi

rm -f "${DEPLOY_DIR}/duniter4j-core-client-*.jar"
if [[ $? -ne 0 ]]; then
    exit 1
fi

cd duniter4j-core-client/target/
JAR_FILE=`ls *.jar`
cp -v ${JAR_FILE} ${DEPLOY_DIR}/
if [[ $? -ne 0 ]]; then
    exit 1
fi

echo " Successfully deployed !"
echo "************************"
