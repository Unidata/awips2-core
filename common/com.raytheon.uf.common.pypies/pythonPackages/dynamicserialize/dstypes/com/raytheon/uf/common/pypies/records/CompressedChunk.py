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
#
#

#
# Compressed version of a DataRecord.
#
#
#     SOFTWARE HISTORY
#
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    09/19/18       7435           ksunil         Eliminate compression/decompression on HDF5



class CompressedChunk(object):

    def __init__(self):
        self.data = None
        self.sizes = None
        self.offsets = None
        self.index = None

    def getData(self):
        return self.data

    def setData(self, data):
        self.data = data

    def getSizes(self):
        return self.sizes

    def setSizes(self, sizes):
        self.sizes = sizes
        
    def getIndex(self):
        return self.index

    def setIndex(self, index):
        self.index = index
        
    def getOffsets(self):
        return self.offsets

    def setOffsets(self, offsets):
        self.offsets = offsets
