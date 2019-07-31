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
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    04/23/2015      4383          randerso       Changed to roll back everything under 
#                                                 the utility tree, not just non-base files
#
##

import sys, builtins 
# import LogStream

class RollBackImporter:
    def __init__(self):
        "Creates an instance and installs as the global importer"
        self.previousModules = set(sys.modules.keys())
        self.realImport = builtins.__import__
        builtins.__import__ = self._import
        self.newModules = set()

    def _import(self, name, globals=None, locals=None, fromlist=[], level=0):
        result = self.realImport(name, globals, locals, fromlist, level)
        
        if hasattr(result, '__file__'):
            if result.__file__.startswith("/awips2/edex/data/utility/"):
#                 LogStream.logDebug("IMPORTING:", name, result)
                self.newModules.add(result.__name__)
#             else:
#                 LogStream.logDebug("IGNORING NON-LOCALIZED:", name, result)
#         else:
#             LogStream.logDebug("IGNORING BUILTIN:", name, result)
        return result

    def rollback(self):
        for modname in self.newModules:
            if modname not in self.previousModules:
                # Force reload when modname next imported
                if modname in sys.modules:
#                     LogStream.logDebug("UNLOADING:", modname)
                    del(sys.modules[modname])
#                 else:
#                     LogStream.logDebug("MODULE NOT FOUND:", modname)
#             else:
#                 LogStream.logDebug("SKIPPING PRELOADED:", modname)

        self.newModules.clear()
