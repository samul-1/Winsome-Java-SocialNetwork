#!/bin/bash
BUILD_PATH=./build
PATHS_FILE=./paths.txt

rm -rf ${BUILD_PATH}; mkdir ${BUILD_PATH}

find . -name "*.java" > ${PATHS_FILE}

javac -d ${BUILD_PATH} -cp "./libs/jackson-core-2.13.0.jar:./libs/jackson-annotations-2.13.0.jar:./libs/jackson-databind-2.13.0.jar" @${PATHS_FILE}

rm ${PATHS_FILE}