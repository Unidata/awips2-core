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
# The ingest mode was originally designed to handle decoding, storage, and
# purge of data received from the SBN.  Over time it took on the role of
# running all services not covered by the request, ingestGrib, and ingestDat
# JVMs.
##

export MAX_MEM=2560 # in Meg

if [ $HIGH_MEM == "on" ]; then
    export MAX_MEM=$((MAX_MEM*2))
fi

export EDEX_DEBUG_PORT=5006

export IGNITE_CLUSTER_1_COMM_PORT=47102
export IGNITE_CLUSTER_1_DISCO_PORT=47502
export IGNITE_CLUSTER_2_COMM_PORT=47107
export IGNITE_CLUSTER_2_DISCO_PORT=47507
