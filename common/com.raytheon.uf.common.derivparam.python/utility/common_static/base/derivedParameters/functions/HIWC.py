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
# Returns heat index or wind chill
#
# ----------------------------------------------------------------
#
# SOFTWARE HISTORY
#
# Date           Ticket#      Engineer      Description
# ------------   ----------   -----------   -----------
#                             ????          Initial creation
# Dec 02, 2013   14455        qlin          Changed 1.85200 to 3.6 at wSpd_kmh = wSpd * 1.85200
# Aug 05, 2015   4703         njensen       Optimized
#

import T
import DpT
import HeatIndex
import WindChill
from unit import knotToMetersPS
from unit import celciusToKelvin

# @param temperature: Temperature in degrees C
# @param tempFromTenths: tempFromTenths in degrees C
# @param dewpoint: dewpoint in degrees C
# @param tdpFromTenths: dpFromTenths in degrees C
# @param rwindSpeed: Wind Speed in knots
# @return: Heat Index or Wind Chill in degrees K
# @rtype: numpy array of Python floats or Python float
def execute1(temperature,tempFromTenths,dewpoint,dpFromTenths,windSpeed):
    TK = T.execute1(temperature,tempFromTenths) #Outputs Kelvin
    DpTK = DpT.execute4(dewpoint,dpFromTenths) #Outputs Kelvin
    wSpd = knotToMetersPS(windSpeed)

    return execute3(TK,DpTK,wSpd)

# @param temperature: Temperature in degrees K
# @param dewpoint: dewpoint in degrees K
# @param relHumidity: Relative humidity from 0 to 100
# @param windSpeed: Wind Speed in meter per second
# @return: Heat Index or Wind Chill in degrees K
# @rtype: numpy array of Python floats or Python float
def execute2(temperature,dewpoint,relHumidity,windSpeed):
    DpTK = DpT.execute5(dewpoint,temperature,relHumidity) #Outputs Kelvin
    return execute3(temperature,DpTK,windSpeed)

# @param T: Temperature in degrees K
# @param DpT: dewpoint in degrees K
# @param wSpd: Wind Speed in meter per second
# @return: Heat Index or Wind Chill in degrees K
# @rtype: numpy array of Python floats or Python float
def execute3(T,DpT,wSpd):
    TC = T - 273.15 #convert from K to C
    DpTC = DpT - 273.15 #convert to from K to C
    wSpd_kmh = wSpd * 3.6 #convert from m/s to km/h
    Hi = HeatIndex.calculate(TC,DpTC) #Outputs Celsius
    Wc = WindChill.calculate(TC,wSpd_kmh) #Outputs Celsius
    maskHi = (Hi != -9999.0)
    Hi[maskHi] = celciusToKelvin(Hi)[maskHi]
    maskWc = (Wc != -9999.0)
    Wc[maskWc] = celciusToKelvin(Wc)[maskWc]
    maskHi = (Hi == -9999.0)
    Hi[maskHi] = Wc[maskHi]
    return Hi
