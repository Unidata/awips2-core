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
# Globally imports and sets up instances of similarly structured python modules
# and/or classes.  For example, if a set of python module files all contain a
# class with the same common parent class.
#
# Designed to be used as a master controller for inspecting and running
# python modules/files from Java.  This class should remain purely python.
# For Java interactions, extend this class.
#
# *IMPORTANT*: Under no condition should you ever delete a module object.  The
# interpreter can potentially get quite messed up.  If you are absolutely sure
# you do not want the module object around, you can pop it off sys.modules.  That
# will decrease the module's reference count and it could potentially be garbage
# collected if there are no more references.  However, popping it off sys.modules
# can also lead to adverse effects such as screwing up the imports that that
# module imported.  (In short, look at addModule() removeModule() and
# reloadModule() and if you override those, you do so at your own risk).
#
#
# SOFTWARE HISTORY
#
# Date          Ticket#  Engineer  Description
# ------------- -------- --------- --------------------------------------------
# Oct 20, 2008           njensen   Initial Creation.
# Jan 17, 2013  1486     dgilling  Make a new-style class.
# Sep 23, 2013  16614    njensen   Fixed reload method
# Jan 20, 2014  2712     bkowal    It is now possible to add errors from a
#                                  subclass.
#
# Mar 25, 2014  2963     randerso  Added check to instantiate method to verify
#                                  module contains desired class throw a useful
#                                  error message if not
# Jan 10, 2015  3974     njensen   Improved documentation
# Feb 13, 2018  6906     randerso  Moved reloadModules and getStartupErrors
#                                  into MasterInterface and eliminated
#                                  RollbackMasterInterface.
# Dec 12, 2018  6906     randerso  Catch attempt to reload deleted module and log
#                                  at debug level
#
#

import os, string
import sys, inspect, traceback
import LogStream

class MasterInterface(object):

    def __init__(self):
        self.scripts = set()
        self.__importErrors = []
        self.__instanceMap = {}

    def importModules(self, scriptPath):
        for s in scriptPath.split(os.path.pathsep):
            if os.path.exists(s):
                scriptfiles = os.listdir(s)

                for filename in scriptfiles:
                    split = string.split(filename, ".")
                    if len(split) == 2 and len(split[0]) > 0 and split[1] == "py" and not filename.endswith("Interface.py"):
                        try:
                            MasterInterface.addModule(self, split[0])
                        except Exception, e:
                            msg = split[0] + "\n" + traceback.format_exc()
                            self.__importErrors.append(msg)
            else:
                os.makedirs(s)

    def getMethodArgs(self, moduleName, className, methodName):
        members = self.__getClassMethods(moduleName, className)
        result = []
        for x, y in members:
            if x == methodName:
                count = y.func_code.co_argcount
                args = y.func_code.co_varnames
                i = 0
                for a in args:
                    if i < count:
                        result.append(a)
                    else:
                        break
                    i = i + 1
        return result

    def getMethodInfo(self, moduleName, className, methodName):
        members = self.__getClassMethods(moduleName, className)
        result = None
        for n, m in members:
            if n == methodName:
                result = m.__doc__
                break
        return result

    def hasMethod(self, moduleName, className, methodName):
        md = sys.modules[moduleName]
        classObj = md.__dict__.get(className)
        return classObj.__dict__.has_key(methodName)

    def __getClassMethods(self, moduleName, className):
        md = sys.modules[moduleName]
        classObj = md.__dict__.get(className)
        return inspect.getmembers(classObj, inspect.ismethod)

    def isInstantiated(self, moduleName):
        return self.__instanceMap.has_key(moduleName)

    def instantiate(self, moduleName, className, **kwargs):
        if sys.modules[moduleName].__dict__.has_key(className):
            instance = sys.modules[moduleName].__dict__.get(className)(**kwargs)
            self.__instanceMap[moduleName] = instance
        else:
            msg = "Module %s (in %s) has no class named %s" % (moduleName, sys.modules[moduleName].__file__, className)
            raise Exception(msg)

    def runMethod(self, moduleName, className, methodName, **kwargs):
        instance = self.__instanceMap[moduleName]
        methods = inspect.getmembers(instance, inspect.ismethod)
        for name, methodObj in methods:
            if name == methodName:
                break
        result = methodObj(**kwargs)
        return result

    def removeModule(self, moduleName):
        if self.isInstantiated(moduleName):
            self.__instanceMap.__delitem__(moduleName)
        if sys.modules.has_key(moduleName):
            self.clearModuleAttributes(moduleName)
            sys.modules.pop(moduleName)
        if moduleName in self.scripts:
            self.scripts.remove(moduleName)

    def addModule(self, moduleName):
        # we may be overriding something in self.scripts, so let's
        # force an import here
        if moduleName in self.scripts:
            self.clearModuleAttributes(moduleName)
            sys.modules.pop(moduleName)
        __import__(moduleName)
        if not moduleName in self.scripts:
            self.scripts.add(moduleName)

    def getImportErrors(self):
        returnList = self.__importErrors
        self.__importErrors = []
        return returnList

    def addImportError(self, error):
        self.__importErrors.append(error)

    def reloadModule(self, moduleName):
        if sys.modules.has_key(moduleName):
            # From the python documentation:
            # "When a module is reloaded, its dictionary (containing the module's
            # global variables) is retained. Redefinitions of names will override the
            # old definitions, so this is generally not a problem. If the new version
            # of a module does not define a name that was defined by the old
            # version, the old definition remains."
            #
            #
            # Because the user might have removed items
            # from the module's dictionary, we cannot trust reload() to
            # remove old items.  We will manually remove everything
            # but built-ins to ensure everything gets re-initialized when
            # reload() is called.
            self.clearModuleAttributes(moduleName)
            reload(sys.modules[moduleName])
                

    def clearModuleAttributes(self, moduleName):
        if sys.modules.has_key(moduleName):
            mod = sys.modules[moduleName]
            modGlobalsToRemove = [k for k in mod.__dict__ if not k.startswith('_')]
            for k in modGlobalsToRemove:
                mod.__dict__.pop(k)

    def reloadModules(self):
        # make a copy of self.scripts so we can remove from it in the loop
        s = self.scripts.copy()
        for script in s:
            try:
                self.reloadModule(script)
            except ImportError as e:
                if "No module named " + script in e.message():
                    # This can result due to a race condition when deleting
                    # multiple files, just log as debug
                    LogStream.logDebug("Attempting to reload non-existent module", script, LogStream.exc())
                    self.removeModule(script)
                else:
                    raise

    def getStartupErrors(self):
        from java.util import ArrayList
        errorList = ArrayList()
        for err in self.getImportErrors():
            errorList.add(str(err))
        return errorList

