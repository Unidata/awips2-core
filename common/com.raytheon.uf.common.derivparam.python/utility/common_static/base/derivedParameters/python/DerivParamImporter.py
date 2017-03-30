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
# Python import hook (PEP 302) for providing inheritance to derived parameters.
# This enables an individual to override one of the methods of a derived
# parameter function while still retaining the base versions of the other
# methods.
#   
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    12/20/10                      njensen        Initial creation
#    06/20/16        5439          bsteffen       import derivtly from files.
#    
# 
#

import sys, imp

from java.util import TreeMap
from com.raytheon.uf.common.localization import PathManagerFactory
from com.raytheon.uf.common.localization import IPathManager
from com.raytheon.uf.common.localization import LocalizationContext
from com.raytheon.uf.common.derivparam.library import DerivedParameterGenerator 
from com.raytheon.uf.common.derivparam.python import MasterDerivScriptFactory

sep = IPathManager.SEPARATOR

class DerivParamImporter(object):
    
    def __init__(self):
        self.pathManager = PathManagerFactory.getPathManager()
        self.localizationType = LocalizationContext.LocalizationType.COMMON_STATIC
        self.functionsDir = DerivedParameterGenerator.FUNCTIONS_DIR
    
    def __getRegularFiles(self, name):
        return self.pathManager.getTieredLocalizationFile(self.localizationType, self.functionsDir + sep + name + '.py')
    
    def __getPackageFiles(self, name):
        return self.pathManager.getTieredLocalizationFile(self.localizationType, self.functionsDir + sep + name + sep + '__init__.py')
    
    def __isDerivParam(self, name):
        files = self.__getRegularFiles(name)
        if files.isEmpty():
            files = self.__getPackageFiles(name)
            return not files.isEmpty()
        return True
    
    def find_module(self, fullname, path=None):
        if path is None:
            if self.__isDerivParam(fullname):
                return self
        elif 'DerivParamImporter' in path:
            if self.__isDerivParam(fullname.replace('.', sep)):
                return self
        return None
    
    def load_module(self, fullname):
        if sys.modules.has_key(fullname):
            return sys.modules[fullname]
        combined = imp.new_module(fullname)
        fullpath = fullname.replace('.', sep)
        files = self.__getRegularFiles(fullpath)
        if files.isEmpty():
            files = self.__getPackageFiles(fullpath)
            combined.__path__ = ['DerivParamImporter']
        files = TreeMap(files)
        for file in files.values():
            source = MasterDerivScriptFactory.readLocalizationFile(file)
            exec source in combined.__dict__
        sys.modules[fullname] = combined
        return combined