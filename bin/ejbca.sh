#!/usr/bin/env bash

# OS specific support.
cygwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
esac

# Check that JAVA_HOME is set
if [ -f $JAVA_HOME ]; then
    echo "You must set JAVA_HOME before running the EJBCA cli."
    exit 1
fi

# Wich command are we running?
if [ "$1" = "batch" ] ; then 
	class_name=se.anatom.ejbca.batch.BatchMakeP12
elif [ "$1" = "ca" ] ; then
	class_name=se.anatom.ejbca.admin.ca
elif [ "$1" = "jobrunner" ] ; then
	class_name=se.anatom.ejbca.util.JobRunner
elif [ "$1" = "ra" ] ; then
	class_name=se.anatom.ejbca.admin.ra
elif [ "$1" = "setup" ] ; then
	class_name=se.anatom.ejbca.admin.setup
elif [ "$1" = "template" ] ; then
	class_name=se.anatom.ejbca.admin.SVGTemplatePrinter
else
	echo "Usage: $0 [batch|ca|ra|setup|template|jobrunner] options"
	echo "For options information, specify a command directive"
	exit 1
fi

# discard $1 from the command line args
shift

# J2EE server classpath
if [ -n "$JBOSS_HOME" ]; then
    echo "Using JBoss JNDI provider..."
    J2EE_DIR="${JBOSS_HOME}"/client
elif [ -n "$WEBLOGIC_HOME" ]; then
    echo "Using Weblogic JNDI provider..."
    J2EE_DIR="${WEBLOGIC_HOME}"/lib
else
    echo "Could not find a valid J2EE server for JNDI provider.."
    echo "Specify a JBOSS_HOME or WEBLOGIC_HOME environment variable"
    exit 1
fi

EJBCA_HOME=..
if [ ! -x ejbca.sh ]
then
EJBCA_HOME=.
fi
# Check that classes exist
if [ ! -d ${EJBCA_HOME}/bin/classes ]
then    
        echo "You must build EJBCA before using the cli, use 'ant'."
        exit 1
fi

# library classpath
CP="$EJBCA_HOME"
for i in "${J2EE_DIR}"/*.jar
do
	CP="$i":"$CP"
done
for i in "${EJBCA_HOME}"/lib/*.jar
do
	CP="$i":"$CP"
done
for i in "${EJBCA_HOME}"/dist/*.jar
do
	CP="$i":"$CP"
done

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  CP=`cygpath --path --windows "$CP"`
fi

exec "$JAVA_HOME/bin/java" -cp $CP $class_name "$@"

