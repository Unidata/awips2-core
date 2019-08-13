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

#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
# Apt 26, 2018   6974         bsteffen      Consolidated VWP related functions here.
#

from meteolib import ztopsa
from numpy import concatenate, float32, isnan, zeros

def samplePlot(wSp, WD, wW, rms):
    """Calculate Sampling String."""
    sampleStrings = list()
    for i in range(len(wSp)):
        if (isnan(WD[i]) or WD[i] <= -8888):
            windDirStr = '***'
        else:
            windDirStr = "%3.3ddeg " % WD[i]
        if (isnan(wSp[i]) or wSp[i] <= -8888):
            windSpdStr = '***'
        else:
            windSpdStr = "%dkts " % (wSp[i] * 1.944)
        if (isnan(rms[i]) or rms[i] <= -8888):
            rmsStr = '***'
        else:
            rmsStr = "rms:%.0fkts " % rms[i]
        if (isnan(wW[i]) or wW[i] <= -8888):
            wCompStr = '***'
        else:
            wCompStr = "w:%.0fcm/s " % wW[i]
        sampleStrings.append(windDirStr + windSpdStr + rmsStr + wCompStr)
    return sampleStrings

def P(levels, staElev):
    """ Combine levels with staElev and use ztopsa to calculate columns of "standard" pressure.

    Args:
        levels:  distance in meters above ground level at each vertical coordinate
        staElev: distance in meters above sea level of the radar station.

    Returns:
        Pressure in hPa
    """
    return ztopsa(GH(levels, staElev))

def GH(levels, staElev):
    """ Combine levels with staElev to create columns of height above mean sea level.

    Args:
        levels:  distance in meters above ground level at each vertical coordinate
        staElev: distance in meters above sea level of the radar station.

    Returns:
        Height in meters above sea level
    """
    staElev = staElev.reshape(-1, 1)
    return concatenate((staElev, levels + staElev), 1)

def zAGL(levels):
    """ Prepend an extra 0 onto levels to create columns of zAGL that match the size of GH and P.

    Args:
        levels:  distance in meters above ground level at each vertical coordinate

    Returns:
        Height in meters above ground level
    """
    return concatenate((zeros([levels.shape[0],1], float32), levels), 1)

def uW(vwpU):
    """ Prepend an extra 0 onto vwpU to create columns of uW that match the size of GH and P.

    Args:
        vwpU:  u component of wind in meters per second at each vertical coordinate

    Returns:
        U component of wind in meters per second
    """
    return concatenate((zeros([vwpU.shape[0],1], float32), vwpU), 1)

def vW(vwpV):
    """ Prepend an extra 0 onto vwpV to create columns of vW that match the size of GH and P.

    Args:
        vwpV:  v component of wind in meters per second at each vertical coordinate

    Returns:
        V component of wind in meters per second
    """
    return concatenate((zeros([vwpV.shape[0],1], float32), vwpV), 1)
