import imp
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

def execute(*args):
    """Test all derived functions for proper operation

    """

    import Vector
    imp.reload(Vector)
    Vector.test()

    import Add
    imp.reload(Add)
    Add.test()

    import Multiply
    imp.reload(Multiply)
    Multiply.test()

    import Divide
    imp.reload(Divide)
    Divide.test()

    import Difference
    imp.reload(Difference)
    Difference.test()

    import Poly
    imp.reload(Poly)
    Poly.test()

    import Average
    imp.reload(Average)
    Average.test()

    import LinTrans
    imp.reload(LinTrans)
    LinTrans.test()

    import Test
    imp.reload(Test)
    Test.test()

    import Rotate
    imp.reload(Rotate)
    Rotate.test()

    import Magnitude
    imp.reload(Magnitude)
    Magnitude.test()

    import Cape
    imp.reload(Cape)
    Cape.test()

    print("-"*60)
    print("Function Test Complete")
