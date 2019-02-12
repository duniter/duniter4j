#!/bin/bash

set JAVA_HOME=/usr/lib/jvm/java-8-oracle
CESIUM_PLUS_POD_DIR="${HOME}/git/duniter/cesium-plus-pod"
DEPLOY_DIR="${CESIUM_PLUS_POD_DIR}/cesium-plus-pod-assembly/target/es-run-home/plugins/cesium-plus-pod-core"

# Go to project root
cd ..
ROOT=`pwd`

echo "***************************************"
echo " Compiling core-* ... "

# Remove old JAR
rm duniter4j-core-client/target/*.jar
rm duniter4j-core-shared/target/*.jar

# Compile the core-client
mvn install --quiet -DskipTests
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
rm -f "${DEPLOY_DIR}/duniter4j-core-shared-*.jar"
if [[ $? -ne 0 ]]; then
    exit 1
fi

cd ${ROOT}/duniter4j-core-client/target/
JAR_FILE=`ls *.jar`
cp -v ${JAR_FILE} ${DEPLOY_DIR}/
if [[ $? -ne 0 ]]; then
    exit 1
fi

cd ${ROOT}/duniter4j-core-shared/target/
JAR_FILE=`ls *.jar`
cp -v ${JAR_FILE} ${DEPLOY_DIR}/
if [[ $? -ne 0 ]]; then
    exit 1
fi

echo " Successfully deployed !"
echo "************************"
