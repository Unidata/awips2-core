#!/bin/bash
##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract DG133W-05-CQ-1067 with the US Government.
# 
# U.S. EXPORT CONTROLLED TECHNICAL DATA
# This software product contains export-restricted data whose
# export/transfer/disclosure is restricted by U.S. law. Dissemination
# to non-U.S. persons whether in the United States or abroad requires
# an export license or other authorization.
# 
# Contractor Name:        Raytheon Company
# Contractor Address:     6825 Pine Street, Suite 340
#                         Mail Stop B8
#                         Omaha, NE 68106
#                         402.291.0100
# 
# See the AWIPS II Master Rights File ("Master Rights File.pdf") for
# further licensing information.
##

##
# The request mode was designed to handle all requests from CAVE.  Therefore
# if services on another JVM failed, CAVE could still receive and send data.
# A few other services have crept in but this JVM mode remains relatively
# pristine.
#
# The creation of the Java class RequestRouter made it possible for CAVE code to
# easily send requests to other JVMs.
##

export MAX_MEM=2304 # in Meg

if [ $HIGH_MEM == "on" ]; then
    export MAX_MEM=4096
fi

export SERIALIZE_POOL_MAX_SIZE=24
export SERIALIZE_STREAM_INIT_SIZE_MB=2
export SERIALIZE_STREAM_MAX_SIZE_MB=8

export EDEX_DEBUG_PORT=5005
export HTTP_PORT=9581

export IGNITE_CLUSTER_1_COMM_PORT=47101
export IGNITE_CLUSTER_1_DISCO_PORT=47501
export IGNITE_CLUSTER_2_COMM_PORT=47106
export IGNITE_CLUSTER_2_DISCO_PORT=47506

#clean up leftover processes from last run of request
pkill -f sendAT
pkill -f requestAT
pkill -f ingestAT
