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

import JUtil

#
# Basic handler for Python to Java and back (this handles all the main things
# that JUtil needs to do, but none of the special ones that should and can be
# added if the code wants to be more powerful.
#
# Note : All methods that convert from Java to Python should take a custom converter, even if it is not used.
#
#
#
# SOFTWARE HISTORY
#
# Date          Ticket#  Engineer  Description
# ------------- -------- --------- ---------------------------------------------
# Oct 14, 2013  2250     mnash     Initial creation of JUtil handler
# Feb 06, 2014           mnash     Fixed fallbacks by using OrderedDict, fixed 
#                                  exception by declaring a size
# Apr 23, 2015  4259     njensen   Updated for new Jep API
# Nov 21, 2016  5959     njensen   Removed primitive conversions for Jep 3.6
# Feb 06, 2017  5959     randerso  Removed Java .toString() calls 
# Nov 17, 2017  20471    randerson Removed String() cast from _toJavaString()
# Jul 31, 2019  7878     tgurney   Add a handler to convert Python
#                                  bytes/bytearray to Java byte[]
#
#

from collections import OrderedDict
import datetime

import jep

from java.lang import Object
from java.lang.reflect import Array
from java.util import Date
from java.util import Collections, HashMap, LinkedHashMap, ArrayList, HashSet
from java.util import List, Set, Map
from com.raytheon.uf.common.python import PyJavaUtil


# Java -> Python conversion

def javaBasicsToPyBasics(obj, customConverter=None):
    '''
    Determines the correct method to call out of the dict to convert Java basic
    objects to Python
    '''
    if hasattr(obj, 'java_name'):
        classname = obj.java_name
        if classname in javaBasics :
            return True, javaBasics[classname](obj)
    return False, obj

def _toPythonString(obj, customConverter=None):
    '''
    Turns a Java String to a Python str
    '''
    return str(obj)

def _toPythonDatetime(obj, customConverter=None):
    '''
    Turns a Java Date to a Python datetime
    '''
    return datetime.datetime.fromtimestamp(obj.getTime() // 1000)

# Python -> Java conversion

def pyBasicsToJavaBasics(val):
    '''
    Method registered with JUtil to figure out any conversion of Python to Java.
    Returns a default of String of that value if nothing else can be found.
    '''
    valtype = type(val)
    if valtype in pythonBasics :
        return True, pythonBasics[valtype](val)
    return False, str(val)

def _toJavaString(val):
    '''
    Turns a Python str to a Java String
    '''
    return str(val)

def _toJavaDate(val):
    '''
    Turns a Python datetime to a Java Date
    '''
    return Date(int(val.timestamp()) * 1000)

def _toJavaByteArray(val):
    jarr = jep.jarray(len(val), jep.JBYTE_ID)
    for i, byte in enumerate(val):
        jarr[i] = byte
    return jarr

# the dict that registers the Python data type to the method for conversion
pythonBasics = OrderedDict({
    str: _toJavaString,
    bytes: _toJavaByteArray,
    bytearray: _toJavaByteArray,
    datetime.datetime: _toJavaDate
    })

# the dict that registers the Java String of type to the method for conversion
javaBasics = OrderedDict({'java.util.Date': _toPythonDatetime})

'''
The following methods will handle Python and Java collection conversion.
'''


# make a jarray to find out if we have that
JEP_ARRAY_TYPE = type(jep.jarray(0, Object))

# Java -> Python conversion

def javaCollectionToPyCollection(obj, customConverter=None):
    '''
    Method registered with JUtil for conversion of Java collections to Python
    collections.
    '''
    if hasattr(obj, 'java_name'):
        classname = obj.java_name
        if classname in javaCollections :
            return True, javaCollections[classname](obj, customConverter)
        elif PyJavaUtil.isArray(obj):
            return True, _fromJavaArray(obj, customConverter)
        else :
            # we have some fallback capability, if we don't specifically handle a class, we
            # want to try some of the more common types and see if those are available for 
            # conversion
            for javaClass in fallbackCollections :
                if PyJavaUtil.isSubclass(obj, javaClass):
                    return True, fallbackCollections[javaClass](obj, customConverter)
    elif isinstance(obj, JEP_ARRAY_TYPE):
        return True, _fromJepArray(obj, customConverter)   
    return False, obj


def _toPythonList(obj, customConverter=None): 
    '''
    Converts to a Python list.
    '''           
    retVal = []
    size = obj.size()
    for i in range(size):
        retVal.append(JUtil.javaObjToPyVal(obj.get(i), customConverter))
    return retVal

def _toPythonTuple(obj, customConverter=None):
    '''
    Converts to a Python tuple.
    '''
    return tuple(_toPythonList(obj, customConverter))
    
def _toPythonDict(obj, customConverter=None):
    '''
    Converts to a Python dict.
    '''
    return __toPythonDictInternal(obj, {}, customConverter)

def _toPythonSet(obj, customConverter=None):
    '''
    Converts to a Python set.
    '''
    retVal = set()
    itr = obj.iterator()
    while itr.hasNext():
        val = next(itr) 
        retVal.add(JUtil.javaObjToPyVal(val, customConverter))
    return retVal

def _toPythonOrderedDict(obj, customConverter=None):
    '''
    Converts to a Python OrderedDict.
    '''
    return __toPythonDictInternal(obj, OrderedDict(), customConverter)

def _fromJavaArray(obj, customConverter=None):
    '''
    Converts from a Java array to a Python list.
    '''
    retVal = []
    size = Array.getLength(obj)
    for i in range(size):
        retVal.append(JUtil.javaObjToPyVal(Array.get(obj, i), customConverter))
    return retVal

def _fromJepArray(obj, customConverter=None):  
    '''
    Converts from a Jep array to a Python list.
    '''  
    retVal = []
    size = len(obj)
    for i in range(size):
        retVal.append(JUtil.javaObjToPyVal(obj[i], customConverter))
    return retVal

def __toPythonDictInternal(javaMap, pyDict, customConverter=None):
    '''
    Converts to a Python dict.  Passed in the dict type, and then handles the key conversion.
    '''
    keys = javaMap.keySet()
    itr = keys.iterator()
    while itr.hasNext() :
        key = next(itr)
        obj = javaMap.get(key)
        pyDict[JUtil.javaObjToPyVal(key)] = JUtil.javaObjToPyVal(obj, customConverter)
    return pyDict

# Python -> Java conversion
    
def pyCollectionToJavaCollection(val):
    '''
    Method registered with JUtil for conversion of collections in Python to
    Java collections.
    '''
    valtype = type(val)
    if valtype in pythonCollections :
        return True, pythonCollections[valtype](val)
    # not directly in the dict, so lets check whether they are subclasses
    for pytype in pythonCollections :
        if issubclass(pytype, valtype):
            return True, pythonCollections[valtype](val)
    return False, str(val)

def _toJavaList(val):
    '''
    Turns a Python list to a Java List
    '''
    retObj = ArrayList()
    for i in val :
        retObj.add(JUtil.pyValToJavaObj(i))
    return retObj

def _toJavaUnmodifiableList(val):
    '''
    Turns a Python tuple to a Java UnmodifiableList
    '''
    return Collections.unmodifiableList(_toJavaList(val))

def _toJavaLinkedMap(val):
    '''
    Turns a Python OrderedDict to a Java LinkedHashMap
    '''
    return __toJavaMapInternal(val, LinkedHashMap())

def _toJavaMap(val):
    '''
    Turns a Python dict to a Java HashMap
    '''
    return __toJavaMapInternal(val, HashMap())

def _toJavaSet(val):
    '''
    Turns a Python set to a Java set
    '''
    return __toJavaSetInternal(val)

def _toJavaUnmodifiableSet(val):
    '''
    Turns a Python frozenset to a Java unmodifiableset
    '''
    return Collections.unmodifiableSet(__toJavaSetInternal(val))

def __toJavaSetInternal(val):
    '''
    Does the actual conversion of the elements inside of the set or frozenset to Set
    '''
    retObj = HashSet()
    for v in val :
        retObj.add(JUtil.pyValToJavaObj(v))
    return retObj


def __toJavaMapInternal(pyDict, jmap):
    '''
    Does the actual conversion of the elements inside of the dict to Map
    '''
    for key in pyDict:
        jmap.put(JUtil.pyValToJavaObj(key), JUtil.pyValToJavaObj(pyDict[key]))
    return jmap

javaCollections = OrderedDict({'java.util.ArrayList':_toPythonList, 'java.util.Arrays$ArrayList':_toPythonList, 'java.util.Collections$UnmodifiableRandomAccessList':_toPythonTuple, 'java.util.HashMap':_toPythonDict, 'java.util.LinkedHashMap':_toPythonOrderedDict})
pythonCollections = OrderedDict({ list:_toJavaList, tuple:_toJavaUnmodifiableList, OrderedDict:_toJavaLinkedMap, dict:_toJavaMap, set:_toJavaSet, frozenset:_toJavaUnmodifiableSet })
fallbackCollections = OrderedDict({ List:_toPythonList, Map:_toPythonDict, Set:_toPythonSet })

'''
Handles other types of Java to Python conversion and back.
'''

def javaObjectToPyObject(obj, customConverter=None):
    '''
    Method registered with JUtil to convert Java objects to Python objects
    that aren't already defined above.
    '''
    if hasattr(obj, 'java_name'):
        if customConverter is not None:
            return True, customConverter(obj)
        # couldn't convert to pure Python object, let it be a PyJObject
        return False, obj
    # assume it's already a Python object
    return True, obj

def pyObjectToJavaObject(val):
    '''
    Method registered with JUtil to convert Python objects to Java objects.
    '''
    valtype = type(val)
    for pyType in pythonClasses :
        if issubclass(valtype, pyType):
            return True, pythonClasses[pyType](val)
    return False, str(val)

def _toJavaClass(val):
    '''
    Utilizes the JUtil.JavaWrapperClass object and its corresponding
    toJavaObj() method that returns a Java object.
    '''
    return val.toJavaObj()

# registers the data type for conversion to a Java class.
pythonClasses = OrderedDict({JUtil.JavaWrapperClass:_toJavaClass})
