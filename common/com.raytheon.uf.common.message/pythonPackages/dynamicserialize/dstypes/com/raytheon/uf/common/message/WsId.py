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
# Modified by njensen to add __repr__
#
# SOFTWARE HISTORY
#
# Date          Ticket#  Engineer  Description
# ------------- -------- --------- ---------------------------------------------
# Apr 25, 2012  545      randerso  Repurposed the lockKey field as threadId
# Jun 12, 2013  2099     dgilling  Implemented toPrettyString().
# Feb 06, 2017  5959     randerso  Removed Java .toString() calls 
# Jun 24, 2020  8187     randerso  Changed to use hostName instead of integer
#                                  network address.
#
##    

import socket
import os
import pwd
import threading

class WsId(object):

    def __init__(self, hostName=None, userName=None, progName=None):
        self.hostName = hostName
        if hostName is None:
            self.hostName = socket.gethostname()
        
        self.userName = userName
        if userName is None:
            self.userName = pwd.getpwuid(os.getuid()).pw_name
            
        self.progName = progName
        if progName is None:
            self.progName = "unknown"
        
        self.pid = os.getpid()
        
        self.threadId = threading.current_thread().ident

    def getHostName(self):
        return self.hostName

    def setHostName(self, hostName):
        self.hostName = hostName

    def getUserName(self):
        return self.userName

    def setUserName(self, userName):
        self.userName = userName

    def getProgName(self):
        return self.progName

    def setProgName(self, progName):
        self.progName = progName

    def getPid(self):
        return self.pid

    def setPid(self, pid):
        self.pid = pid

    def getThreadId(self):
        return self.threadId

    def setThreadId(self, threadId):
        self.threadId = threadId
        
    def toPrettyString(self):        
        return self.userName + "@" + hostname + ":" + self.progName + ":" + str(self.pid) + ":" + str(self.threadId)
    
    def __str__(self):
        s = ":".join([self.hostName, self.userName, self.progName, str(self.pid), str(self.threadId)])
        return s
    
    def __repr__(self):
        return self.__str__()