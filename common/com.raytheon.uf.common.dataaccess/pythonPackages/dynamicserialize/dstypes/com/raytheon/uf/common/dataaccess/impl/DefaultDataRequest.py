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

# File auto-generated against equivalent DynamicSerialize Java class
# and then modified post-generation to sub-class IDataRequest.
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    05/28/13        2023          dgilling       Initial Creation.
#    12/15/16        6040          tgurney        Override __str__
# 
#


from awips.dataaccess import IDataRequest

from dynamicserialize.dstypes.org.locationtech.jts.geom import Envelope
from dynamicserialize.dstypes.com.raytheon.uf.common.dataplugin.level import Level


class DefaultDataRequest(IDataRequest):

    def __init__(self):
        self.datatype = None
        self.identifiers = {}
        self.parameters = []
        self.levels = []
        self.locationNames = []
        self.envelope = None
        
    def setDatatype(self, datatype):
        self.datatype = str(datatype)
        
    def addIdentifier(self, key, value):
        self.identifiers[key] = value
    
    def removeIdentifier(self, key):
        del self.identifiers[key]
    
    def setParameters(self, *params):
        self.parameters = list(map(str, params))
    
    def setLevels(self, *levels):
        self.levels = list(map(self.__makeLevel, levels))
    
    def __makeLevel(self, level):
        if type(level) is Level:
            return level
        elif type(level) is str:
            return Level(level)
        else:
            raise TypeError("Invalid object type specified for level.")
    
    def setEnvelope(self, env):
        self.envelope = Envelope(env.envelope)

    def setLocationNames(self, *locationNames):
        self.locationNames = list(map(str, locationNames))

    def getDatatype(self):
        return self.datatype

    def getIdentifiers(self):
        return self.identifiers

    def getParameters(self):
        return self.parameters

    def getLevels(self):
        return self.levels

    def getEnvelope(self):
        return self.envelope

    def getLocationNames(self):
        return self.locationNames

    def __str__(self):
        fmt = ('DefaultDataRequest(datatype={}, identifiers={}, parameters={}, ' +
        'levels={}, locationNames={}, envelope={})')
        return fmt.format(self.datatype, self.identifiers, self.parameters, self.levels,
                          self.locationNames, self.envelope)
