#!/bin/bash
JAVA=/usr/bin/java

HOME_DIR="/opt/cep-engine-service"
JAVA_OPTIONS=$HOME_DIR/jvm_options
JAR_FILE="target/cep-engine-0.1.0.jar"

cd $HOME_DIR
$JAVA `cat $JAVA_OPTIONS` -jar $JAR_FILE