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
# and then modified post-generation to make it a abstract base class.
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    07/23/14         #3185        njensen        Initial Creation.
#    Jun 01, 2016     5587         tgurney        Change self.datatype to
#                                                 self.request
#
#

import abc

class AbstractIdentifierRequest(object):
    __metaclass__ = abc.ABCMeta

    def __init__(self):
        self.request = None

    def getRequest(self):
        return self.request

    def setRequest(self, request):
        self.request = request

