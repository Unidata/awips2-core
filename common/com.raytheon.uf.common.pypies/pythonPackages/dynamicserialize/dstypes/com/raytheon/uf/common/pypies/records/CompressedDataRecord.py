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

# File auto-generated against equivalent DynamicSerialize Java class
# and then modified by bsteffen

import numpy
import StringIO
import gzip

class CompressedDataRecord(object):

    def __init__(self):
        self.type = None
        self.uncompressedData = None
        self.compressedData = None
        self.name = None
        self.dimension = None
        self.sizes = None
        self.maxSizes = None
        self.props = None
        self.minIndex = None
        self.group = None
        self.dataAttributes = None
        self.fillValue = None
        self.maxChunkSize = None

    def getType(self):
        return self.type

    def setType(self, type):
        self.type = type

    def getCompressedData(self):
        return self.compressedData

    def setCompressedData(self, compressedData):
        self.compressedData = compressedData

    def getName(self):
        return self.name

    def setName(self, name):
        self.name = name

    def getDimension(self):
        return self.dimension

    def setDimension(self, dimension):
        self.dimension = dimension

    def getSizes(self):
        return self.sizes

    def setSizes(self, sizes):
        self.sizes = sizes

    def getMaxSizes(self):
        return self.maxSizes

    def setMaxSizes(self, maxSizes):
        self.maxSizes = maxSizes

    def getProps(self):
        return self.props

    def setProps(self, props):
        self.props = props

    def getMinIndex(self):
        return self.minIndex

    def setMinIndex(self, minIndex):
        self.minIndex = minIndex

    def getGroup(self):
        return self.group

    def setGroup(self, group):
        self.group = group

    def getDataAttributes(self):
        return self.dataAttributes

    def setDataAttributes(self, dataAttributes):
        self.dataAttributes = dataAttributes

    def getFillValue(self):
        return self.fillValue

    def setFillValue(self, fillValue):
        self.fillValue = fillValue

    def getMaxChunkSize(self):
        return self.maxChunkSize

    def setMaxChunkSize(self, maxChunkSize):
        self.maxChunkSize = maxChunkSize
    
    def determineStorageType(self):
        if self.type == "BYTE":
            return numpy.byte
        elif self.type == "SHORT":
            return numpy.short
        elif self.type == "INT":
            return numpy.int32
        elif self.type == "LONG":
            return numpy.int64
        elif self.type == "FLOAT":
            return numpy.float32
        elif self.type == "DOUBLE":
            return numpy.float64
        else:
            raise TypeError("Unexpected compressed type " + str(self.type))
    
    def decompress(self):
        datatype = numpy.dtype(self.determineStorageType()).newbyteorder('>')
        compressedFile = StringIO.StringIO(numpy.getbuffer(self.compressedData))
        self.compressedData = None
        decompressedFile = gzip.GzipFile(fileobj=compressedFile)
        self.uncompressedData = numpy.frombuffer(decompressedFile.read(), datatype)
    
    def retrieveDataObject(self):
        if self.uncompressedData is None:
            self.decompress()
        return self.uncompressedData
    
    def putDataObject(self, obj):
        self.compressedData = None
        self.uncompressedData = obj

    prepareStore = decompress