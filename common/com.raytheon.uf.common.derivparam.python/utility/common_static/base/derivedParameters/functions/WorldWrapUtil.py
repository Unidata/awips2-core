##
# This software was developed and / or modified by Raytheon Company,
# pursuant to Contract EA133W-17-CQ-0082 with the US Government.
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
###

#
# SOFTWARE HISTORY
#
# Date          Ticket  Engineer  Description
# ------------- ------- --------- -----------------
# Nov 09, 2018  7531    bsteffen  Initial Creation
#

from numpy import empty, shape
import inspect

def HandleWorldWrapX(f):
    """
        Decorator for derived parameter functions that use neighboring grid
        cells. This decorator solves a problem for world wide grids where a
        line of missing data appears along the "seam" of the grid because
        normal calculations do not take into account that neighboring grid
        cells can be accessed from the other side of the grid.
        
        Functions that use this decorator can take an extra optional argument
        that will specify whether the grid is a world wide grid that allows
        wrapping. When this argument is True then the input data to the
        function is expanded, extra columns are copied to each side from the
        opposite side. This allows the calculations to access the neighbor
        along the edges, and the missing region is pushed to the new columns.
        After the normal function executes, the new columns are stripped off
        the result allowing a grid to be returned without the missing regions
        along the "seam". All the column manipulation is performed in the 
        decorator, allowing the original function to remain unchanged.
    """
    nargs = len(inspect.getargspec(f).args)
    def wrapper(*args):
        worldWrapX = False
        if len(args) == nargs + 1:
            worldWrapX = args[-1]
            args = args[:-1]
            if worldWrapX:
                args = wrapX(*args)
        r = f(*args)
        if worldWrapX:
            if isinstance(r, tuple):
                t = []
                for i in r:
                    t.append(i[:,1:-1])
                r = tuple(t)
            else:
                r = r[:,1:-1]
        return r
    return wrapper


def wrapX(*args):
    """
        Expand every arg, adding extra columns to each side to account for
        world wrapping. This method can convert 2d arrays, tuples of 2d arrays,
        and it also ignores scalar values. This will return a tuple that is
        exactly like the provided args except all 2d arrays are expanded. 
    """
    result = []
    for a in args:
        if isinstance(a, tuple):
            result.append(wrapX(*a))
        else:
            ashape = shape(a)
            if sum(ashape)==len(ashape):
                result.append(a)
            else:
                r = empty((ashape[0], ashape[1]+2), a.dtype)
                r[:,1:-1] = a
                r[:,0] = a[:,-1]
                r[:,-1] = a[:,0]
                result.append(r)
    return tuple(result)