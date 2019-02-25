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

#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Aug 05, 2015   4703         njensen       Removed unused imports
# 11/02/2018     20976       mgamazaychikov Added execute2 method
#

from numpy import concatenate, zeros, shape, nan

def execute1(pvv):
    pv = zeros((shape(pvv)[0], 1))
    pv.fill(nan)
    result = concatenate((pv, pvv),1)
    return result
def execute2(gvv, p, t):
    rgas  = 287.058                     # J/(kg-K) => m2/(s2 K)
    g     = 9.80665                     # Gravitational acceleration m/s2
    result = -gvv*p/(rgas*t)*g*100.0    # omega (Pa/s)
    return result
