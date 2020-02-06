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
# Aug 05, 2015   4703         njensen       cast scalars to float32
# Apt 26, 2018   6974         bsteffen      remove execute2.
#

from numpy import equal, where, zeros, concatenate, greater, float32
from unit import pascalToMilliBar

def execute1(pressure, sfcPress):
    sfcPress = sfcPress.reshape(- 1, 1)
    result = concatenate((sfcPress, pressure), 1)
    return pascalToMilliBar(result)

def execute3(numProfLvlsStation, MB):
    ret = zeros(numProfLvlsStation.shape, float32)
    ret.fill(MB)
    return ret

def execute4(prCloudStation,lowCldStation,midCldStation,hiCldStation):
    prCloudMB = prCloudStation/100
    CCPval = ccpExecute(lowCldStation,midCldStation,hiCldStation)
    isCeiling = where(greater(CCPval, 0.5), CCPval, float32(-9999))
    prCloudMB[isCeiling == -9999] = float32(-9999)
    prCloudMB[prCloudStation == -9999] = float32(-9999)
    return prCloudMB

def execute5(height, elevation):
    # array height is in meters
    # scalar elevation is in meters
    pressure = where(equal(height, - 9999), float32(-9999), height + elevation)
    pressure = meteolib.ztopsa(pressure)
    return pressure

def execute6(numLevelsStation, MB):
    ret = zeros(numLevelsStation.shape, float32)
    ret.fill(MB)
    return ret

def ccpExecute(lowCldStation,midCldStation,hiCldStation):
    P = maximum(lowCldStation,midCldStation,hiCldStation)
    P[P != -9999] /= 100
    return P
