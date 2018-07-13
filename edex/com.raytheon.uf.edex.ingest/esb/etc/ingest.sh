#!/bin/bash
##
# The ingest mode was originally designed to handle decoding, storage, and
# purge of data received from the SBN.  Over time it took on the role of
# running all services not covered by the request, ingestGrib, and ingestDat
# JVMs.
##
export INIT_MEM=512 # in Meg
export MAX_MEM=4096 # in Meg
export EDEX_DEBUG_PORT=5006
