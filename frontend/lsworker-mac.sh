#!/bin/bash
mkdir ~/.lsw
export JAVA_HOME="$1"/Contents/Java/jre
export PATH=$JAVA_HOME/bin:$PATH
arch=$(uname -m)
which java
java -version
java -Dspring.profiles.active=mac -Dmacos.arch=$arch -Dapple.awt.UIElement=true -jar "$1"/Contents/Java/luckystackworker.jar >> ~/.lsw/lsw.log
