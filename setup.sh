#!/bin/bash

export JRE_HOME="$(/usr/libexec/java_home)"

if [[ "$PATH" != *$ANT_HOME/bin* ]]
then
    export PATH="$ANT_HOME/bin:$PATH"
fi
