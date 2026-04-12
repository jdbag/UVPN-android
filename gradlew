#!/bin/sh

#
# Gradle startup script for UN*X
# Fixed: removed quotes from DEFAULT_JVM_OPTS that caused "-Xmx64m" class error
#

PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME="`dirname "$PRG"`"
cd "$APP_HOME" >/dev/null
APP_HOME="`pwd -P`"

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# ── FIX: No quotes wrapping the JVM flags ──────────────────
# The bug was: DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
# Java reads "-Xmx64m" (with quotes) as a class name → ClassNotFoundException
DEFAULT_JVM_OPTS="-Xmx2048m -Xms512m"

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ] ; then
        echo "ERROR: JAVA_HOME points to invalid dir: $JAVA_HOME" >&2
        exit 1
    fi
else
    JAVACMD="java"
fi

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

exec "$JAVACMD" \
    $DEFAULT_JVM_OPTS \
    $JAVA_OPTS \
    $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
