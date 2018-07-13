#!/bin/bash
##
# The request mode was designed to handle all requests from CAVE.  Therefore
# if services on another JVM failed, CAVE could still receive and send data.
# A few other services have crept in but this JVM mode remains relatively
# pristine.
#
# The creation of the Java class RequestRouter made it possible for CAVE code to
# easily send requests to other JVMs.
##
export INIT_MEM=128 # in Meg
export MAX_MEM=2024 # in Meg

export SERIALIZE_POOL_MAX_SIZE=500
export SERIALIZE_STREAM_INIT_SIZE_MB=2
export SERIALIZE_STREAM_MAX_SIZE_MB=8

export EDEX_DEBUG_PORT=5005
export HTTP_PORT=9581
