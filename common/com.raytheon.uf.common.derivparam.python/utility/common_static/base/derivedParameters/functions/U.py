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
# Calculate U component from a vector or from a magnitude and direction
# 
# ----------------------------------------------------------------

import numpy

const1 = 0.0174533

def execute(magOrVec, dir=None):
    if dir is None:
        return magOrVec[0]
    dir = numpy.where(dir < 0, numpy.NaN, numpy.where(dir > 360, numpy.NaN,dir))
    magOrVec = numpy.where(magOrVec < 0, numpy.NaN, numpy.where(magOrVec > 250, numpy.NaN, magOrVec))
    theta = dir * const1
    U = (-1 * magOrVec) * numpy.sin(theta)
    return U