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
 #
# The primary purpose of this hook is to allow files at multiple localization
# levels to be combined into a single module. This allows a file override to
# override a single method while inheriting other methods from higher level
# files.
#
# Another function of this hook is to allow sub-directories to be treated as
# packages automatically, without the need for an __init__.py file.
#
# The final thing this hook does is allow the localization files to be imported
# without relying on the file existing on the local filesystem. The content of
# of the file are streamed directly to a string which is executed to create the
# modules.   
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    12/20/10                      njensen        Initial creation
#    06/20/16        5439          bsteffen       import directly from localization files.
#    10/05/16        5891          bsteffen       Treat all directories as modules, even without __init___.py.
#    06/01/17        5891          bsteffen       Separate files into their own modules to support source code lookup. 
#    05/30/18        6995          njensen        Move sep from global scope to class scope to avoid reinit issue
#    01/30/20        7911          tgurney        Remove use of imp module
#    
# 
#

import sys
import types

from com.raytheon.uf.common.derivparam.library import DerivedParameterGenerator
from com.raytheon.uf.common.derivparam.python import MasterDerivScriptFactory
from com.raytheon.uf.common.localization import IPathManager
from com.raytheon.uf.common.localization import LocalizationContext
from com.raytheon.uf.common.localization import PathManagerFactory
from java.util import TreeMap


class DerivParamImporter(object):
    
    def __init__(self):
        self.pathManager = PathManagerFactory.getPathManager()
        self.localizationType = LocalizationContext.LocalizationType.COMMON_STATIC
        self.functionsDir = DerivedParameterGenerator.FUNCTIONS_DIR
        # separator should not be global as it may get cleaned up when re-initializing the Python interpreter
        self.sep = IPathManager.SEPARATOR
    
    def __getRegularFiles(self, name):
        return self.pathManager.getTieredLocalizationFile(self.localizationType, self.functionsDir + self.sep + name + '.py')
    
    def __getPackageFiles(self, name):
        return self.pathManager.getTieredLocalizationFile(self.localizationType, self.functionsDir + self.sep + name + self.sep + '__init__.py')

    def __getDirectoryFiles(self, name):
        return self.pathManager.getTieredLocalizationFile(self.localizationType, self.functionsDir + self.sep + name)
    
    def __isDerivParam(self, name):
        files = self.__getRegularFiles(name)
        if files.isEmpty():
            files = self.__getPackageFiles(name)
            if files.isEmpty():
                files = self.__getDirectoryFiles(name)
                return not files.isEmpty()
        return True
    
    def find_module(self, fullname, path=None):
        if path is None:
            if self.__isDerivParam(fullname):
                return self
        elif 'DerivParamImporter' in path:
            if self.__isDerivParam(fullname.replace('.', self.sep)):
                return self
        return None
    
    def load_module(self, fullname):
        if fullname in sys.modules:
            return sys.modules[fullname]
        combined = types.ModuleType(fullname)
        combined.__loader__ = self
        fullpath = fullname.replace('.', self.sep)
        files = self.__getRegularFiles(fullpath)
        if files.isEmpty():
            files = self.__getPackageFiles(fullpath)
            combined.__path__ = ['DerivParamImporter']
        files = TreeMap(files)
        for file in files.values():
            loader = LocalizedModuleLoader(file)
            localname = str(file.getContext().getLocalizationLevel()) + '-' + fullname
            localmod = loader.load_module(localname)
            combined.__dict__.update(localmod.__dict__)
        sys.modules[fullname] = combined
        return combined

# This  loader is used internally by the DerivParamImporter to load a single
# localization file into a module. The reason for a separate loader is to support
# source code lookup for traceback and conversion to java stack traces. Although
# these modules end up in sys.modules, they are not intended to be used outside of
# the DerivParamImporter
class LocalizedModuleLoader(object):
    
    def __init__(self, file):
        self.file = file
        # separator should not be global as it may get cleaned up when re-initializing the Python interpreter
        self.sep = IPathManager.SEPARATOR

    def load_module(self, fullname):
        module = types.ModuleType(fullname)
        exec(self.get_code(fullname), module.__dict__)
        module.__loader__ = self
        # Must put the module in sys.module or source lookup doesn't work.
        sys.modules[fullname] = module
        return module
        
    def get_code(self, fullname):
        fullfile = str(self.file.getContext()) + self.sep + self.file.getPath()
        return compile(self.get_source(fullname), fullfile, 'exec')

    def get_source(self, fullname):
        return MasterDerivScriptFactory.readLocalizationFile(self.file)
