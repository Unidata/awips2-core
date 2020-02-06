# #
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
# #

from collections import OrderedDict
from java.util import HashMap, LinkedHashMap, ArrayList
from java.lang import String
import array
import pickle

#
# Provides convenience methods for Java-Python bridging
#
# For utilization of the basics, doing the following will work just fine :
#
# import JUtil
# JUtil.pyValToJavaObj(val)
#
# However, to do more advanced conversion, you can register a handler and it will then be
# available to do the necessary conversion.
# 
# For example, Geometry objects are not currently handled.  The GeometryHandler module will
# handle them however.  Code would be written as follows :
#
# import JUtil
# from GeometryHandler import jtsToShapely, shapelyToJTS
# JUtil.registerJavaToPython(jtsToShapely)
# JUtil.registerPythonToJava(shapelyToJTS)
# JUtil.pyValToJavaObj(val) 
#
#     SOFTWARE HISTORY
#
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    05/01/08                      njensen        Initial Creation.
#    03/12/13         1759         dgilling       Extend Java List types handled
#                                                 by javaObjToPyVal().
#    08/20/13         2250         mnash          Handle Dates, doubles, and arrays
#    10/15/13                      mnash          Refactor to reduce dependencies and clean up
#    Apr 23, 2015     4259         njensen        Updated for new JEP API
#    Nov 28, 2016     5959         njensen        Renamed methods for clarity
#    Jul 31, 2019     7878         tgurney        Add helpers for handling
#                                                 pickled objects as java byte[]
#
#



def registerPythonToJava(method):
    '''
    We want to register new handlers per PythonInterpreter.  
    This allows us to utilize different conversions that may not
    be needed for some PythonInterpreters, but to use them in others
    where they are required.
    '''
    pythonHandlers.append(method)
    
def registerJavaToPython(method):
    javaHandlers.append(method)

def pyValToJavaObj(val):
    '''
    The main method for Python to Java conversion in JUtil.  If no conversion
    method can be found, a string representation of the python value will be 
    returned as we do not want to crash the JVM.
    '''
    retVal = val
    if retVal is not None :
        for handleMethod in pythonHandlers:
            success, obj = handleMethod(val)
            if success:
                retVal = obj
                break
    return retVal

def javaObjToPyVal(obj, customConverter=None):
    '''
    The main method for Java to Python conversion in JUtil.  If no conversion
    method can be found, the PyJObject will be returned and may still be able
    to be used that way from within Python.
    '''
    retVal = obj
    if retVal is not None :
        for handleMethod in javaHandlers:
            success, val = handleMethod(obj, customConverter)
            if success:
                retVal = val
                break
    return retVal

def _javaByteArrayToPyBytes(jarray):
    # TODO: Consider using this as a handler for PyJArrays of bytes once there
    # is some way to tell what type a given PyJArray is. May be able to do this
    # after upgrading to jep 3.9 or later.
    #
    # Until then, javaObjToPyVal will return a list of ints when called on
    # a PyJArray of bytes. This mirrors the behavior of javaObjToPyVal as
    # applied to other types of Java primitive arrays.
    pyarray = array.array('b', jarray)
    view = memoryview(pyarray).cast('B')
    return bytes(view)

def javaPickle(obj):
    """Return pickled Python object as a Java byte[]"""
    obj = pickle.dumps(obj)
    return pyValToJavaObj(obj)

def javaUnpickle(jarray):
    """Return Python object unpickled from Java byte[]"""
    byteString = _javaByteArrayToPyBytes(jarray)
    return pickle.loads(byteString)

def javaStringListToPylist(jlist):
    '''
    Going forward should use javaObjToPyVal instead.
    '''
    pylist = []
    size = jlist.size()
    for i in range(size):
        pylist.append(str(jlist.get(i)))
    return pylist

def pylistToJavaStringList(pylist):
    '''
    Going forward should use pyValToJavaObj instead.
    '''
    jlist = ArrayList()
    for i in pylist:
        jlist.add(String(i))
    return jlist


def javaMapToPyDict(javaMap, customConverter=None):
    '''
    Going forward should use javaObjToPyVal instead.
    '''
    keys = javaMap.keySet()
    itr = keys.iterator()
    if javaMap.java_name == "java.util.LinkedHashMap":
        pyDict = OrderedDict()
    else:
        pyDict = {}
    while itr.hasNext():
        key = next(itr)
        obj = javaMap.get(key)
        pyDict[javaObjToPyVal(key)] = javaObjToPyVal(obj, customConverter)
    return pyDict


def pyDictToJavaMap(pyDict):
    '''
    Going forward should use pyValToJavaObj instead.
    '''
    if pyDict is None :
        return None

    if isinstance(pyDict, OrderedDict):
        jmap = LinkedHashMap()
    else:
        jmap = HashMap()

    for key in pyDict:
        jmap.put(pyValToJavaObj(key), pyValToJavaObj(pyDict[key]))
    return jmap

class JavaWrapperClass(object):
    def toJavaObj(self):
        raise NotImplementedError("Subclasses must override this method.")

# this initializes the basic handlers for Java->Python conversion and Python->Java conversion

from JUtilHandler import javaBasicsToPyBasics, pyBasicsToJavaBasics, javaCollectionToPyCollection, pyCollectionToJavaCollection, javaObjectToPyObject, pyObjectToJavaObject

pythonHandlers = [pyBasicsToJavaBasics, pyCollectionToJavaCollection, pyObjectToJavaObject]
javaHandlers = [javaBasicsToPyBasics, javaCollectionToPyCollection, javaObjectToPyObject]
