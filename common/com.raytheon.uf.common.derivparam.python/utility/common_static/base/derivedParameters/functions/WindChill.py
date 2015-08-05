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
# Calculates the Wind Chill from temperature(C) and windSpeed(km/h)
# ----------------------------------------------------------------

#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Aug 05, 2015   4703         njensen       Optimized
#

from numpy import power, where, float32, NaN

def calculate(T,wSpd):

    # T and DpT have to be in C for the below calculation
    badValue = where(T > 16, float32(1), float32(0))
    noChill = where(wSpd < 6.4, float32(1), float32(0))
    wSpd = where(wSpd > 128.75, float32(128.75), wSpd)
    wSpd = power(wSpd, 0.16)
    Wc = 13.12 + 0.6215 * T - 11.37 * wSpd + 0.3965 * T * wSpd
    #Returned in Celsius
    return where(badValue == 1, float32(NaN), where(noChill == 1, T, Wc))