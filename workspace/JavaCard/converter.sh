#!/bin/bash

export JC_HOME="/home/arno/workspace/security/wpo/cardsdk/java_card_kit-2_2_2-rr-bin-linux-do"
export CLASSES="$JC_HOME/lib/apduio.jar:$JC_HOME/lib/apdutool.jar:$JC_HOME/lib/jcwde.jar:$JC_HOME/lib/converter.jar:$JC_HOME/lib/scriptgen.jar:$JC_HOME/lib/offcardverifier.jar:$JC_HOME/lib/api.jar:$JC_HOME/lib/installer.jar:$JC_HOME/lib/capdump.jar:$JC_HOME/samples/classes:$CLASSPATH"

echo $CLASSES
java -classpath "$CLASSES" com.sun.javacard.converter.Converter $*
