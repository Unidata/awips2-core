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


import numpy
#from numpy import zeros
#from numpy import where
#from numpy import greater_equal
#from numpy import 


def execute1(gammaE,MpV):
   "Calculate slantwise and vertical instability, assign as icons"
   MpV = MpV*1E5
     
   tmp = numpy.zeros(gammaE.shape,dtype=gammaE.dtype)
  
   mask1 = numpy.less(gammaE, 0.0)
   tmp = numpy.where(mask1, 192, tmp)
     
   mask2 = numpy.greater_equal(gammaE, 0.0)
   mask3 = numpy.less(MpV, 0.20)
   #0.25
   tmp = numpy.where(numpy.ma.logical_and(mask2, mask3), 30, tmp)
  
   return tmp
