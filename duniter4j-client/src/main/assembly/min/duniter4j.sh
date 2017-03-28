#!/bin/bash

READLINK=`which readlink`
if [ -z "$READLINK"  ]; then
  message "Required tool 'readlink' is missing. Please install before launch \"$0\" file."
  exit 1
fi

# ------------------------------------------------------------------
# Ensure BASEDIR points to the directory where the soft is installed.
# ------------------------------------------------------------------
SCRIPT_LOCATION=$0
if [ -x "$READLINK" ]; then
  while [ -L "$SCRIPT_LOCATION" ]; do
    SCRIPT_LOCATION=`"$READLINK" -e "$SCRIPT_LOCATION"`
  done
fi

export APPDIR=`dirname "$SCRIPT_LOCATION"`
export JARDIR="$APPDIR/duniter4j"
export JAR="$JARDIR/${project.build.finalName}.${project.packaging}"
export I18N_DIR="$APPDIR/i18n"

# Retrieve the JAVA installation
if [ "~$JAVA_HOME" -eq "~" ]; then
    export JAVA_HOME="$APPDIR/jre"
    export JAVA_COMMAND="$JAVA_HOME/bin/java"

    if [ -f "$JAVA_HOME/bin/java" ]; then
        # If embedded JRE exists, make sure java is executable
        chmod +x "$JAVA_COMMAND"
    else
        # If not Embedded JRE, use the default binary
        export JAVA_COMMAND=java
    fi
else
    export JAVA_COMMAND="$JAVA_HOME/bin/java"
fi

if [ -d "$HOME" ]; then
    export BASEDIR="$HOME/.config/duniter4j"
    export CONFIG_DIR="$BASEDIR/config"
    export CONFIG_FILE="$CONFIG_DIR/duniter4j-client.config"
    export LOG_FILE="$BASEDIR/logs/${project.build.finalName}.log"
else
    export BASEDIR="$APPDIR"
    export CONFIG_DIR="$APPDIR/config"
    export CONFIG_FILE="$CONFIG_DIR/config/duniter4j-client.config"
    export LOG_FILE="$APPDIR/logs/${project.build.finalName}.log"

    echo "Using base"
fi

# Create the config dir if need
mkdir -p "$CONFIG_DIR"

# Create the config file (if need)
if [ ! -f "$CONFIG_FILE" ]; then
    echo "INFO - Initialized configuration file: $CONFIG_FILE"
    cp -u $JARDIR/duniter4j-client.config $CONFIG_FILE
fi


cd $APPDIR

while true; do

  $JAVA_COMMAND $JAVA_OPTS -Dduniter4j.log.file=$LOG_FILE -Dduniter4j.i18n.directory=$I18N_DIR -jar $JAR --basedir $BASEDIR --config $CONFIG_FILE $*
  exitcode=$?

  if [ ! "$exitcode" -eq  "130" ]; then
    echo "INFO - Application stopped with exitcode: $exitcode"
  fi

  ## Continue only if exitcode=88 (will restart the application)
  if [ ! "$exitcode" -eq  "88" ]; then
    # quit now!
    exit $exitcode
  fi
done
