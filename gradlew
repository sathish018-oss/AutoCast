#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done
SAVED="`pwd`"
CDPATH= cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='-Xmx1024m -Xms256m'

# Use the maximum available options if this is not a 64-bit OS
# (i.e. empty ARCH_FLAGS)
if [ -z "$ARCH_FLAGS" ]; then
    case "`uname -m`" in
        i?86) ARCH_FLAGS="-m32" ;;
        *)    ARCH_FLAGS="" ;;
    esac
fi

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange paths for the JRE
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
case "`uname`" in
    Darwin* | SunOS* | HP-UX* | AIX*)
        MAX_FD_LIMIT=`ulimit -H -n`
        if [ $? -eq 0 ] ; then
            if [ "$MAX_FD_LIMIT" = "unlimited" ] ; then
                # ulimit -H -n returns "unlimited" on macOS for non-root users
                MAX_FD_LIMIT=""
            else
                ulimit -n $MAX_FD_LIMIT
                if [ $? -ne 0 ] ; then
                    echo "Could not set maximum file descriptor limit: $MAX_FD_LIMIT"
                fi
            fi
        fi
        ;;
esac

# For Darwin, add options to specify how the application appears in the dock
case "`uname`" in
    Darwin*)
        GRADLE_OPTS="$GRADLE_OPTS \"-Ddock:name=$APP_NAME\" \"-Ddock:icon=$APP_HOME/media/gradle.icns\""
        ;;
esac

# For Cygwin or MSYS2, switch paths to Windows format before running java
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

# Determine the Gradle command line arguments
exec "$JAVACMD" $DEFAULT_JVM_OPTS $GRADLE_OPTS "-Dorg.gradle.appname=$APP_BASE_NAME" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
