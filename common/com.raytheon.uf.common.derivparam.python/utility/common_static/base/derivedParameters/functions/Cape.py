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

#    SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    --/--/-----                                  Initial creation
#    May 06, 2014    3101          njensen        Cast numpy shape values to int
#                                                  for cross platform compatibility

from com.raytheon.uf.common.derivparam.python.function import CapeFuncPythonAdapter as CapeFunc
from numpy import zeros

def execute(*args):
    return __execute(*args)[0]

def __execute(*args):
    """ Calculate Convective Available Potential Energy. 
        
    @param temperature
                 if there are five or six input arguments the leading argument is
                 3D (virtual) temperature. 
                 
                 Otherwise, the leading inputs are a list of pressure values and a 
                 list of corresponding (virtual) temperature values.
    @param pressure
    @param potentialTemperature
    @param specificHumidity
                 of the starting parcel
    @param useVirtualTemp
                 if true (1) use virtual temperatures, otherwise (0) use plain
                 temperatures
    @param upperTerminationPressure
                 optional argument designating the upper pressure at which the CAPE
                 computation terminates
                 
    @return a scalar
    
    Temperature must be in Kelvins
    
    """
    
    argLength = len(args)
    
    upperTerminationPressure = None
    useVirtualTemp = args[argLength - 1]
    specificHumidity = args[argLength - 2]
    potentialTemperature = args[argLength - 3]
    pressure = args[argLength - 4]
    temperatureListLengthOffset = 4
    
    if type(useVirtualTemp) != float:
        # an upperTerminationPressure has been supplied
        upperTerminationPressure = useVirtualTemp
        useVirtualTemp = args[argLength - 2]
        specificHumidity = args[argLength - 3]
        potentialTemperature = args[argLength - 4]
        pressure = args[argLength - 5]
        temperatureListLengthOffset = 5
    
    pressureValues = []
    temperatureValues = []
    
    if type(args[0]) == list:
        # we have 3D temperature data
        temperatureValues = args[0][0]
        pressureValues = zeros(temperatureValues.shape, temperatureValues.dtype)
    else:
        # we have a list of pressures followed by temperatures
        temperatureListLength = (argLength - temperatureListLengthOffset) / 2 
        for i in range(0, temperatureListLength):
            pressureValues.append(args[i])
            temperatureValues.append(args[i + temperatureListLength])
    
    # expand each pressure value to a full grid size
    for pressureLevel in range(0, pressureValues.shape[0]):
        gridLevelValues = pressureValues[pressureLevel]
        gridLevelValues[:] = args[0][1][0][pressureLevel]
    
    pressureValue = pressure
    pressure = zeros(temperatureValues.shape, temperatureValues.dtype)
    pressure[:] = pressureValue
    threeDshape = pressureValues.shape
    if upperTerminationPressure is None:        
        return CapeFunc.capeFunc(useVirtualTemp, pressureValues, temperatureValues, pressure, potentialTemperature, specificHumidity, int(threeDshape[1]), int(threeDshape[2]), int(threeDshape[0]))
    else:        
        return CapeFunc.capeFuncTop(useVirtualTemp, pressureValues, temperatureValues, pressure, potentialTemperature, specificHumidity, upperTerminationPressure, int(threeDshape[1]), int(threeDshape[2]), int(threeDshape[0]))
