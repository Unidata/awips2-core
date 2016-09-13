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
# Aug 05, 2015   4703         njensen       cast NaN to float32
# Oct 27, 2015   4703         bsteffen      correct use of where
#
from numpy import where, float32, NaN

const1 = 33.86389
const2 = 100

def execute(altimeter, accum_altimeter24):
    ALT0 = where(altimeter > 0 , altimeter, float32(NaN))
    ALT24 = where(accum_altimeter24 > 0,
                        accum_altimeter24,
                        float32(NaN))
    return (ALT0 - ALT24) * const1 * const2