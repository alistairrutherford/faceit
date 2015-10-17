#!/bin/sh
JRE_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home

CLASSPATH=""
for i in `ls lib/*.jar`; do
  CLASSPATH="$CLASSPATH":"$i"
done

echo "Building Face catalog with CLASSPATH=$CLASSPATH"
$JRE_HOME/bin/java -cp resources:$CLASSPATH com.netthreads.faceit.FaceFilter
