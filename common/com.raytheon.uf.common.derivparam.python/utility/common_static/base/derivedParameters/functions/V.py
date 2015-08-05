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

# ----------------------------------------------------------------
# Calculate V component from a vector or from a magnitude and direction
#
# ----------------------------------------------------------------

#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Aug 05, 2015   4703         njensen       Optimized
#

from numpy import NaN, float32, cos, radians

def execute(magOrVec, dir=None):
    if dir is None:
        return magOrVec[0]

    dir[dir < 0] = float32(NaN)
    dir[dir > 360] = float32(NaN)
    magOrVec[magOrVec < 0] = float32(NaN)
    magOrVec[magOrVec > 250] = float32(NaN)
    theta = radians(dir)
    V = (-1 * magOrVec) * cos(theta)
    return V