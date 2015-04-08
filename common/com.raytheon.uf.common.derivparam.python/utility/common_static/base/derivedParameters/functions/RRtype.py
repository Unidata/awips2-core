##
# 
#<DerivedParameter unit="dbZ" name="Radar with PType" abbreviation="RRtype" xmlns:ns2="group" xmlns:ns3="http://www.example.org/productType">
#     <Method levels="Surface" name="RRtype.execute1">
#         <Field abbreviation="CSNOW"/>
#         <Field abbreviation="CICEP"/>
#         <Field abbreviation="CFRZR"/>
#         <Field abbreviation="CRAIN"/>
#         <Field level="1kmAgl" abbreviation="RR"/>       
#     </Method>
#     <Method levels="Surface" name="RRtype.execute2">
#         <Field abbreviation="CSNOW"/>
#         <Field abbreviation="CICEP"/>
#         <Field abbreviation="CFRZR"/>
#         <Field abbreviation="CRAIN"/>
#         <Field level="EA" abbreviation="CXR"/>       
#     </Method>
# </DerivedParameter>
##
#
####################################

import numpy


def execute1(CSNOW,CICEP,CFRZR,CRAIN,RR):
   #USING 1kmAgl reflectivity
   # Assign zeros to the array 
   tmp = numpy.zeros(CSNOW.shape,dtype=CSNOW.dtype)
   #
   # Transform the type grids. 
   # result = Each type is set to a value: 
   # e.g., SN 1, IP 81, FZRA 161, RA 241
   result = (CSNOW * 1)+(CICEP*81.)+(CFRZR*161.)+(CRAIN*241.)
   # Plunk in reflectivty into tmp, but only > 0 dbZ
   # else make it a large negative value to have no display
   mask1 = numpy.less(RR, 0.0)
   tmp = numpy.where(mask1, -99999.0, RR)
   # Then, we add reflectivity from ~0-80dbz to each value of type. 
   # This provides the reflectivity footprint. 
   # The result is a product with binned data: 
   # e.g., SN 1-80, IP 81-160, FZRA 161-240, RA 241-320
     
   tmp = result + tmp
    
   # So, the higher the reflectivity, the higher the value within
   # each range of type. 40dbz Snow = 40 units. 40 dbz of FZRA = 201 units
   # Then, make a curve to display these ranges.  
   
   # Final check - display reflectivity only at ptype locations. 
   mask2 = numpy.greater(result, 0.0)
   tmp = numpy.where(mask2, tmp, 0.0)
   
   return tmp  


def execute2(CSNOW,CICEP,CFRZR,CRAIN,CXR):
#  # USING composite reflectivity.  
   # Assign zeros to the array 
   tmp = numpy.zeros(CSNOW.shape,dtype=CSNOW.dtype)
   #
   # Transform the type grids. 
   # result = Each type is set to a value: 
   # e.g., SN 1, IP 81, FZRA 161, RA 241
   result = (CSNOW * 1)+(CICEP*81.)+(CFRZR*161.)+(CRAIN*241.)
   # Plunk in reflectivty into tmp, but only > 0 dbZ
   mask1 = numpy.less(CXR, 0.0)
   tmp = numpy.where(mask1, -99999.0, CXR)
   # Then, we add reflectivity from ~0-80dbz to each value of type. 
   # This provides the reflectivity footprint. 
   # The result is a product with binned data: 
   # e.g., SN 1-80, IP 81-160, FZRA 161-240, RA 241-320
     
   tmp = result + tmp
    
   # So, the higher the reflectivity, the higher the value within
   # each range of type. 40dbz Snow = 40 units. 40 dbz of FZRA = 201 units
   # Then, make a curve to display these ranges.  
   
   # Final check - display reflectivity only at ptype locations. 
   mask2 = numpy.greater(result, 0.0)
   tmp = numpy.where(mask2, tmp, 0.0)
   
   return tmp  