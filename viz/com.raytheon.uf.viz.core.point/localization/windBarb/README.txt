*******************************************************
I. SUMMARY:
*******************************************************

This README.txt file contains instructions on how to create configuration files that override the default 
properties used to draw 'Wind Barbs' from the various views found within the CAVE Graphical User Interface (GUI). 


*******************************************************
II. DESCRIPTION:
*******************************************************

Each view from within the CAVE GUI which is capable of drawing 'Wind Barbs' has been assigned its own 
unique XML configuration file template and unique file name. 


A. The complete XML structure of a 'Wind Barb' configuration file is shown below:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="RadarXYResource">
            <baseSize></baseSize>
            <offsetRatio></offsetRatio>
            <minimumMagnitude></minimumMagnitude>
            <barbRotationDegrees></barbRotationDegrees>
            <barbLengthRatio></barbLengthRatio>
            <barbSpacingRatio></barbSpacingRatio>
            <barbFillFiftyTriangle></barbFillFiftyTriangle>
            <calmCircleMaximumMagnitude></calmCircleMaximumMagnitude>
            <calmCircleSizeRatio></calmCircleSizeRatio>
            <arrowHeadSizeRatio></arrowHeadSizeRatio>
            <linearArrowScaleFactor></linearArrowScaleFactor>
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>


B. The configuration file consists of the following eleven 'Wind Barb' properties which may be assigned values. 
The 'Data Type' column describes the type of data which may be assigned to each property.

            Property:                       Data Type:
                    
            <baseSize>                      Integer or Decimal Number  (i.e. 1 or 1.0)
            <offsetRatio>                   Integer or Decimal Number
            <minimumMagnitude>              Integer or Decimal Number
            <barbRotationDegrees>           Integer or Decimal Number
            <barbLengthRatio>               Integer or Decimal Number
            <barbSpacingRatio>              Integer or Decimal Number
            <barbFillFiftyTriangle>         Boolean                    (i.e. 'true' or 'false')
            <calmCircleMaximumMagnitude>    Integer or Decimal Number
            <calmCircleSizeRatio>           Integer or Decimal Number
            <arrowHeadSizeRatio>            Integer or Decimal Number
            <linearArrowScaleFactor>        Integer or Decimal Number


D. Please note that the <windBarbPlugin> element consists of the 'className' attribute which is unique to 
each configuration file template and is pre-configured within each template so that it does not need 
to be modified:

            Example:

            <windBarbPlugin className="RadarXYResource">


E. In addition, each configuration file has a unique name which is specific to each template:

            Example:
            
            RadarPluginWindBarbConfig.xml


*******************************************************
III. CREATING 'WIND BARB' CONFIGURATION FILES:
*******************************************************

A. Select a template from the 'CONFIGURATION FILE TEMPLATES' section of this document based on the CAVE GUI 
to which the modified 'Wind Barb' properties are to be applied. In the example below, the 'Radar' template 
has been selected:

            Example:
            
            <windBarbPluginConfig>
                <windBarbPluginList>
                    <windBarbPlugin className="RadarXYResource">
                    </windBarbPlugin>
                </windBarbPluginList>
            </windBarbPluginConfig>


B. Create a configuration file (i.e. text file) with the file name specified for the chosen template and 
save it a temporary directory on the user's machine:

            Example:

            /tmp/RadarPluginWindBarbConfig.xml


C. Determine which default 'Wind Barb' properties should to be overridden and update the configuration file 
by inserting only the XML elements specific to those properties as child elements to the <windBarbPlugin> 
element. In the example below, the <baseSize> and <offsetRatio> elements have been inserted into the template 
and have been given decimal values:

            Example:
            
            <windBarbPluginConfig>
                <windBarbPluginList>
                    <windBarbPlugin className="RadarXYResource">
                        <baseSize>24.0</baseSize>
                        <offsetRatio>1.0</offsetRatio>
                    </windBarbPlugin>
                </windBarbPluginList>
            </windBarbPluginConfig>


D. Launch CAVE and open the 'Localization' perspective.

E. From the 'Localization' perspective, navigate to the 'CAVE/Wind Barbs' folder.

F. Right-click the mouse on the 'Wind Barbs' folder and select the 'Import File...' option.

G. Navigate to the temporary directory where the configuration file was saved and select the file to import 
it into the 'CAVE/Wind Barbs' folder within the 'Localization' perspective.

H. Switch to the appropriate perspective within CAVE where the 'Wind Barbs' should be generated relative
to the 'Wind Barb' configuration file which was just created (i.e. the 'D2D' perspective in this example) and 
generate the 'Wind Barbs'.

I. The 'Wind Barbs' should be generated with the updated properties.


*******************************************************
IV. CONFIGURATION FILE TEMPLATES:
*******************************************************

DESCRIPTION:
 
A template consists of the following components:

a. Template Name: Name of the template.

b. Configuration File Name: The unique configuration file name associated with a template.

c. Configuration File Template: The template is made up of the basic XML elements which are specific to each 
template.

d. Each template also contains an example of how to navigate to the CAVE GUI view which is associated with 
each template.
 

LIST OF TEMPLATES:

=======================================================
A. Template Name: 'Radar'

a. Configuration File Name: 'RadarPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="RadarXYResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Radar >> Dial Radars >> All Dial Radars >> ktlh-kyux >> kudx >> 
kudx Derived Products >> VAD Wind Profile (VWP)' option:

a. 'Wind Barbs' should be populated in the 'Radar XY Plot' tab of the CAVE GUI

=======================================================
B. Template Name: 'Cross Section'

a. Configuration File Name: 'CrossSectionWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="CrossSectionVectorResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Volume >> Browser' option to open the 'Volume Browser' dialog box:

a. Left click the mouse button on the fourth Menu bar option ('Plan view') and select the 'Cross section' 
option at which point the 'Plan view' option should change to 'Cross section'

b. Select 'Source >> Volume >> HRRR'

c. Then, select 'Fields >> Basic >> Wind'

d. Then, select 'Planes >> Specified >> LineA'

e. Then, select 'HRRR LineA Wind (kts)' from the 'Product Selection List' column and click the 'Load' button

f. A 'Cross Section' tab should open in CAVE's D2D Perspective and 'Wind Barbs' should be visible in this view

=======================================================
C. Template Name: 'Time Height'

a. Configuration File Name: 'TimeHeightWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="TimeHeightVectorResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Volume >> Browser' option to open the 'Volume Browser' dialog box:

a. Left click the mouse button on the fourth Menu bar option ('Plan view') and select the 'Time height' option 
at which point the 'Plan view' option should change to 'Time height'

b. Select 'Source >> Volume >> HRRR'

c. Then, select 'Fields >> Basic >> Wind'

d. Then, select 'Planes >> Points >> D2D Points >> Tsect A'

e. Then, select 'HRRR Tsect A Wind (kts)' from the 'Product Selection List' column and click the 'Load' button

f. A 'Time Height' tab should open in CAVE's D2D Perspective and 'Wind Barbs' should be visible in this view

=======================================================
D. Template Name: 'Var Height'

a. Configuration File Name: 'VarHeightPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="VarHeightResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Volume >> Browser' option to open the 'Volume Browser' dialog box:

a. Left click the mouse button on the fourth Menu bar option ('Plan view') and select the 'Var vs Hgt' option 
at which point the 'Plan view' option should change to 'Var vs Hgt'

b. Select 'Source >> Volume >> HRRR'

c. Then, select 'Fields >> Basic >> Wind'

d. Then, select 'Planes >> Points >> D2D Points >> VarHgt A'

e. Then, select 'HRRR VarHgt A Wind (kts)' from the 'Product Selection List' column and click the 'Load' button

f. A 'Var Height' tab should open in CAVE's D2D Perspective and 'Wind Barbs' should be visible in this view

=======================================================
E. Template Name: 'Objective Analysis'

a. Configuration File Name: 'ObjectiveAnalysisPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="OAResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Volume >> Browser' option to open the 'Volume Browser' dialog box:

a. The fourth Menu bar option in the dialog box should display the 'Plan view' text

b. Select 'Point >> MetarOA'

c. Then, select 'Fields >> Wind >> Wind(Total)'

d. Then, select 'Planes >> Misc >> Sfc'

e. Then, select a row from the 'Product Selection List' column and click the 'Load' button

=======================================================
F. Template Name: 'Satellite'

a. Configuration File Name: 'PointDataPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="WindPlotRenderable">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Satellite >> Derived Products Plots >> Scatterometer Winds >> 
ASCAT Winds 25 km' option:

a. 'Wind Barbs' should be populated in the 'Map' tab of the CAVE GUI

=======================================================
G. Template Name: 'GFE'

a. Configuration File Name: 'GFEPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="GFEResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the GFE Perspective in CAVE

2) From the 'Grid Manager' tab, scroll down to the 'Wind SF Fcst (OAX)' row:

a. Right-click the mouse on an empty grid space (i.e. dotted rectangle) in that row and select the 
'Create from scratch' option

b. Right-click and hold the mouse pointer on the color bar shown in the 'Map' tab and select the 
'Set Pickup Value…' option to open the 'PickUp Value' dialog box

c. To change the wind magnitude/direction, ensure that the 'Both' radio button is selected in the 
'PickUp Value' dialog box and then enter the magnitude and direction in the text boxes. Note that the direction 
is in 10 degree increments so 18 indicates a 180 degree (south) wind. Once you have set the magnitude and 
direction as desired, click the 'Assign Value' button. If the 'Empty Edit Area Warning' dialog box is displayed, 
check the 'Do not show this message again' check-box and then click the 'Yes' button.

d. 'Wind Barbs' with the assigned magnitude/direction should be displayed in the 'Map' tab

=======================================================
H. Template Name: 'Plan View'

a. Configuration File Name: 'GeolocatedGridDataDisplaysBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="AbstractGridResource">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the D2D Perspective in CAVE

2) From the top Menu bar in CAVE, select the 'Volume >> Browser' option to open the 'Volume Browser' dialog box:

a. The fourth Menu bar option in the dialog box should display the 'Plan view' text

b. Select 'Source >> Volume >> GFS80'

c. Then, select 'Fields >> Wind >> Wind(Total)'

d. Then, select 'Planes >> Misc >> Sfc'

e. Then, select 'GFS80 Sfc Wind (Total) (kts)' from the 'Product Selection List' column and click the 'Load' 
button

f. The 'Map' tab in CAVE's D2D Perspective should be populated with 'Wind Barbs'

=======================================================
I. Template Name: 'Grid'

a. Configuration File Name: 'NcgridPluginWindBarbConfig.xml'

b. Configuration File Template:

<windBarbPluginConfig>
    <windBarbPluginList>
        <windBarbPlugin className="GriddedVectorDisplay">
        </windBarbPlugin>
    </windBarbPluginList>
</windBarbPluginConfig>

c. Example steps for navigating to the CAVE view which generates 'Wind Barbs' for this template:

1) Open the NCP Perspective in CAVE

2) Select the 'Resource Manager' icon at the the top of the CAVE GUI

3) When the 'Resource Manager' dialog box opens:

a. Select the 'GRID/GFS/basic_wx/pmsl_thkn_wind' option from the 'Selected Resources' option box and the select 
the 'New' button

b. When the 'Select New Resource' dialog box opens, select the 'GRID >> GFS >> basic_wx >> pmsl_thkn_wind' 
option and then select the 'Add Resource' button

c. Select the 'Close' button to close the 'Select New Resource' dialog box

d. Select the 'Load RBD' button

e. 'Wind Barbs' should be loaded into the 'Welcome' tab of the CAVE GUI

=======================================================

