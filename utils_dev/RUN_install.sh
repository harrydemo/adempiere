#!/bin/sh

#	This script installs adempiere
#	Ported from Windows script Marek Mosiewicz<marek.mosiewicz@jotel.com.pl>

#check java home
if [ $JAVA_HOME ]; then
  export PATH=$JAVA_HOME/bin:$PATH	
else
  echo JAVA_HOME is not set.
  echo You may not be able to build Adempiere
  echo Set JAVA_HOME to the directory of your local JDK.
  exit
fi

#classpath
export ANT_CLASSPATH=$CLASSPATH:../tools/lib/ant.jar:../tools/lib/ant-launcher.jar:../tools/lib/ant-swing.jar:../tools/lib/ant-commons-net.jar:../tools/lib/commons-net.jar:$JAVA_HOME/lib/tools.jar

echo Installing ...
$JAVA_HOME/bin/java -classpath $ANT_CLASSPATH -Dant.home="." $ANT_PROPERTIES org.apache.tools.ant.Main install

echo Done ...

exit 0
