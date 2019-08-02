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
# Python wrapper class that wraps a Java DataTime behind familiar python objects.
#  
#    
# SOFTWARE HISTORY
#
# Date          Ticket#  Engineer  Description
# ------------- -------- --------- ---------------------------------------------
# Dec 12, 2012           njensen   Initial Creation.
# May 01, 2014  3095     bsteffen  Don't default fcstTime to 0 in init.
# Jun 24, 2014  3096     mnash     Fix pieces of implementation to better match native python version
# Feb 06, 2017  5959     randerso  Removed Java .toString() calls 
#    
# 
#

import AbsTime
import JUtil
import TimeRange

from com.raytheon.uf.common.time import DataTime as JavaDataTime


class DataTime(JUtil.JavaWrapperClass):

    def __init__(self, dtime, fcstTime=None):
        if isinstance(dtime, AbsTime.AbsTime):
            self.__dt = JavaDataTime(dtime.toJavaObj())
        elif isinstance(dtime, str):
            self.__dt = JavaDataTime(dtime)
        else:
            # assuming Java object
            self.__dt = dtime                        
        # TODO add support for other possible types of dtime?
        if fcstTime is not None:
            self.__dt.setFcstTime(fcstTime)
        
    def getRefTime(self):
        return self.__dt.getRefTime()
    
    def setRefTime(self, refTime):
        self.__dt.setRefTime(refTime)
        
    def getFcstTime(self):
        return self.__dt.getFcstTime().getTime()
    
    def setFcstTime(self, fcstTime):
        self.__dt.setFcstTime(fcstTime)
    
    def getValidPeriod(self):
        return TimeRange.TimeRange(self.__dt.getValidPeriod())
    
    def setValidPeriod(self, tr):
        self.__dt.setValidPeriod(tr.toJavaObj())
        
    def getUtilityFlags(self):
        self.__dt.getUtilityFlags()
    
    def setUtilityFlags(self, utilityFlags):
        self.__dt.setUtilityFlags(utilityFlags)
    
    def getLevelValue(self):
        return self.levelValue

    def setLevelValue(self, levelValue):
        self.__dt.setLevelValue(levelValue)
    
    def __eq__(self, other):
        return self.__dt.equals(other.toJavaObj())
    
    def __ne__(self, other):
        return not self == other
    
    def __lt__(self, other):
        return self.__dt.compareTo(other.toJavaObj()) < 0

    def __le__(self, other):
        return self.__dt.compareTo(other.toJavaObj()) <= 0

    def __gt__(self, other):
        return self.__dt.compareTo(other.toJavaObj()) > 0

    def __ge__(self, other):
        return self.__dt.compareTo(other.toJavaObj()) >= 0
    
    def __str__(self):
        return str(self.__dt)

    def __repr__(self):
        return str(self.__dt)
    
    def toJavaObj(self):
        return self.__dt

        
