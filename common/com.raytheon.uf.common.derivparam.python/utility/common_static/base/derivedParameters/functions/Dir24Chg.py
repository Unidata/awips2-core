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
###

# ----------------------------------------------------------------
#
#
# ----------------------------------------------------------------
#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Aug 05, 2015   4703         njensen       Optimized
# Oct 27, 2015   4703         bsteffen      correct use of U,V
#

import U
import V
from numpy import arctan2

def execute(windSpeed, windDir, accum_windSpeed24, accum_windDir24):
    U0 = U.execute(windSpeed, windDir)
    V0 = V.execute(windSpeed, windDir)

    U24 = U.execute(accum_windSpeed24, accum_windDir24)
    V24 = V.execute(accum_windSpeed24, accum_windDir24)

    DU = U0 - U24
    DV = V0 - V24

    WD = 57.2957 * arctan2(-DU,-DV)
    WD[WD < 0] += 360

    return WD
