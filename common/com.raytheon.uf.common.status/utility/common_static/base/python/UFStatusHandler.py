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

from com.raytheon.uf.common.status import UFStatus
Priority = UFStatus.Priority
import logging

#
# Python logging mechanism for logging through Java to UFStatus.
# When using this class the python logger level should not be set.
# This will allow the filtering to be handled by the UFStatus. 
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    07/19/09                      njensen       Initial Creation.
#    02/13/2015      4038          rferrel        Force root logging to NOTSET
#    Apr 25, 2015    4952          njensen        Updated for new JEP API
#    
# 
#

# Force root logging to lowest level NOTSET (0) in order to allow the emit 
# to determine what should be passed to the IUFStatusHandler.
logging.getLogger().setLevel(logging.NOTSET)

    
class UFStatusHandler(logging.Handler):

    def __init__(self, pluginName, category, source=None, level=logging.NOTSET):
        logging.Handler.__init__(self, level)
        self._pluginName = pluginName
        self._category = category
        self._source = source
        self._handler = UFStatus.getHandler(self._pluginName, self._source)
        
    
    def emit(self, record):        
        "Implements logging.Handler's interface.  Record argument is a logging.LogRecord."
        priority = None
        if record.levelno >= 50:
            priority = Priority.CRITICAL
        elif record.levelno >= 40:
            priority = Priority.SIGNIFICANT
        elif record.levelno >= 30:
            priority = Priority.PROBLEM
        elif record.levelno >= 20:
            priority = Priority.EVENTA
        elif record.levelno >= 10:
            priority = Priority.EVENTB
        else:
            priority = Priority.VERBOSE
        
        if self._handler.isPriorityEnabled(priority) :
            msg = self.format(record)
            self._handler.handle(priority, msg)
