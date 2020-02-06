/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.core.maps.dialogs;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.projection.MapProjection;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.NoSuchIdentifierException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.operation.Projection;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.maps.actions.NewMapEditor;
import com.raytheon.uf.viz.core.maps.scales.IMapScaleDisplay;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.dialogs.CaveJFACEDialog;
import org.locationtech.jts.geom.Coordinate;

/**
 * Dialog that creates a custom geotools projection
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 16, 2008  783      randerso  Initial Creation
 * Jan 29, 2013  15567    snaples   Remove Orthographic projection from list
 *                                  temporarily
 * Apr 21, 2016  5579     bsteffen  Default to match current display. Add Import
 *                                  Extent From Display. Add tooltips.
 * 
 * </pre>
 * 
 * @author randerso
 */
public class CreateProjectionDialog extends CaveJFACEDialog {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(CreateProjectionDialog.class);

    /**
     * Map from parameter name to a short description of the parameter. The
     * parameter names are all lower case. Not all parameters are included, only
     * the most common parameters are defined.
     */
    private final Map<String, String> parameterHelpText;

    private DefaultMathTransformFactory factory;

    private ParameterValueGroup parameters;

    private Combo projList;

    private Text projNameText;

    private class ParamUI {
        public Label label;

        public Text text;

        public ParamUI(Label label, Text text) {
            this.label = label;
            this.text = text;
        }
    }

    private final Map<Parameter<?>, ParamUI> paramMap = new HashMap<>();

    // private Composite paramComp;
    private Group paramGroup;

    private Button validateBtn;

    private Text validationText;

    private Composite dlgComp;

    private Button cornersBtn;

    private Button centerBtn;

    private Button importExtentButton;

    private Text llLatText;

    private Text llLonText;

    private Text urLatText;

    private Text urLonText;

    private Text centerLatText;

    private Text centerLonText;

    private Text widthText;

    private Text heightText;

    private Label widthUnit;

    private Label heightUnit;

    private CoordinateReferenceSystem crs;

    private Group cornersGroup;

    private Group centerGroup;

    private GeneralGridGeometry newMapGeom;

    /**
     * @param parentShell
     */
    public CreateProjectionDialog(Shell parentShell) {
        super(parentShell);
        parameterHelpText = createPrameterHelpText();
    }

    private static Map<String, String> createPrameterHelpText() {
        /*
         * Based on the ESRI GIS Dictionary
         * (http://support.esri.com/en/knowledgebase/Gisdictionary)
         */
        Map<String, String> parameterHelpText = new HashMap<>();
        parameterHelpText.put("semi_major",
                "The equatorial radius of a spheroid.");
        parameterHelpText.put("semi_minor", "The polar radius of a spheroid.");
        parameterHelpText
                .put("central_meridian",
                        "The line of longitude that defines the center and often the x-origin of a projected coordinate system.");
        parameterHelpText
                .put("latitude_of_origin",
                        "The latitude value that defines the origin of the y-coordinate values for a projection.");
        parameterHelpText
                .put("scale_factor",
                        "A value (usually less than one) that converts a tangent projection to a secant projection.");
        parameterHelpText
                .put("false_easting",
                        "The linear value added to all x-coordinates of a map projection so that none of the values in the geographic region being mapped are negative.");
        parameterHelpText
                .put("false_northing",
                        "The linear value added to all y-coordinates of a map projection so that none of the values in the geographic region being mapped are negative.");
        parameterHelpText
                .put("standard_parallel_1",
                        "The line of latitude in a conic or cylindrical projection in normal aspect where the projection surface touches the globe.");
        return parameterHelpText;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Create Projection");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        dlgComp = (Composite) super.createDialogArea(parent);

        Composite projComp = new Composite(dlgComp, SWT.NONE);
        Layout layout = new GridLayout(2, false);
        projComp.setLayout(layout);

        factory = new DefaultMathTransformFactory();
        Set<?> methods = factory.getAvailableMethods(Projection.class);
        // Removed for DR 15567
        // String[] projections = new String[methods.size()];

        // int i = 0;
        List<Object> prjj = new ArrayList<>();
        for (Object obj : methods) {
            if (obj instanceof MapProjection.AbstractProvider) {
                MapProjection.AbstractProvider prj = (MapProjection.AbstractProvider) obj;
                // DR15567 Remove "Orthographic" projection temporarily
                String orthProj = prj.getName().getCode();
                if (orthProj == "Orthographic") {
                    continue;
                } else {
                    prjj.add(prj.getName().getCode());
                }
                // Put this back in when ready to revert the code
                // projections[i++] = prj.getName().getCode();
            }
        }
        // DR15567
        String[] projections = new String[prjj.size()];
        int j = 0;
        for (Object obj : prjj) {
            projections[j++] = obj.toString();
        }
        // End of mods
        Arrays.sort(projections);

        new Label(projComp, SWT.NONE).setText("Projection:");
        projList = new Combo(projComp, SWT.DROP_DOWN | SWT.READ_ONLY);
        projList.setItems(projections);
        projList.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleProjectionSelection();
            }
        });
        projList.setText(projections[0]);

        new Label(projComp, SWT.NONE).setText("Name:");
        projNameText = new Text(projComp, SWT.BORDER);
        projNameText
                .setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        setProjectionNameText(projList.getText());

        paramGroup = new Group(dlgComp, SWT.BORDER);
        paramGroup.setText("Parameters");
        layout = new GridLayout(2, false);
        paramGroup.setLayout(layout);
        paramGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Group defineByGroup = new Group(dlgComp, SWT.BORDER_SOLID);
        defineByGroup.setText("Define Extent by ");
        layout = new GridLayout(2, true);
        defineByGroup.setLayout(layout);
        defineByGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));

        cornersBtn = new Button(defineByGroup, SWT.RADIO);
        cornersBtn.setText("Corners");
        cornersBtn.setSelection(true);

        centerBtn = new Button(defineByGroup, SWT.RADIO);
        centerBtn.setText("Center");

        importExtentButton = new Button(dlgComp, SWT.PUSH);
        importExtentButton.setText("Import Extent From Display");
        importExtentButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                importViewExtent();
            }

        });

        cornersGroup = new Group(dlgComp, SWT.BORDER);
        cornersGroup.setText("Extent");
        layout = new GridLayout(3, false);
        cornersGroup.setLayout(layout);
        cornersGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));

        new Label(cornersGroup, SWT.NONE).setText("");
        new Label(cornersGroup, SWT.NONE).setText("Latitude:");
        new Label(cornersGroup, SWT.NONE).setText("Longitude:");

        new Label(cornersGroup, SWT.NONE).setText("Lower Left:");
        llLatText = new Text(cornersGroup, SWT.BORDER);
        llLonText = new Text(cornersGroup, SWT.BORDER);

        new Label(cornersGroup, SWT.NONE).setText("Upper Right:");
        urLatText = new Text(cornersGroup, SWT.BORDER);
        urLonText = new Text(cornersGroup, SWT.BORDER);

        centerGroup = new Group(dlgComp, SWT.BORDER);
        centerGroup.setText("Extent");
        layout = new GridLayout(3, false);
        centerGroup.setLayout(layout);
        centerGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));

        new Label(centerGroup, SWT.NONE).setText("");
        new Label(centerGroup, SWT.NONE).setText("Latitude:");
        new Label(centerGroup, SWT.NONE).setText("Longitude:");

        new Label(centerGroup, SWT.NONE).setText("Center:");
        centerLatText = new Text(centerGroup, SWT.BORDER);
        centerLonText = new Text(centerGroup, SWT.BORDER);

        new Label(centerGroup, SWT.NONE).setText("Width:");
        widthText = new Text(centerGroup, SWT.BORDER);
        widthUnit = new Label(centerGroup, SWT.NONE);
        widthUnit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));

        new Label(centerGroup, SWT.NONE).setText("Height:");
        heightText = new Text(centerGroup, SWT.BORDER);
        heightUnit = new Label(centerGroup, SWT.NONE);
        heightUnit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                | GridData.GRAB_HORIZONTAL));

        centerGroup.setVisible(false);
        ((GridData) centerGroup.getLayoutData()).exclude = true;

        centerBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (centerBtn.getSelection()) {
                    centerGroup.setVisible(true);
                    ((GridData) centerGroup.getLayoutData()).exclude = false;

                    cornersGroup.setVisible(false);
                    ((GridData) cornersGroup.getLayoutData()).exclude = true;

                    validateParameters();

                    getShell().pack();
                }
            }
        });

        cornersBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (cornersBtn.getSelection()) {
                    centerGroup.setVisible(false);
                    ((GridData) centerGroup.getLayoutData()).exclude = true;

                    cornersGroup.setVisible(true);
                    ((GridData) cornersGroup.getLayoutData()).exclude = false;

                    validateParameters();

                    getShell().pack();
                }
            }
        });

        Group validGroup = new Group(dlgComp, SWT.BORDER);
        layout = new GridLayout(2, false);
        validGroup.setLayout(layout);
        validGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        validateBtn = new Button(validGroup, SWT.PUSH);
        validateBtn.setText("Validate");
        validateBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateParameters();
            }

        });

        validationText = new Text(validGroup, SWT.READ_ONLY | SWT.WRAP
                | SWT.BORDER | SWT.V_SCROLL);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        layoutData.minimumHeight = validationText.getLineHeight() * 3;
        validationText.setLayoutData(layoutData);

        applyDialogFont(dlgComp);
        return dlgComp;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);
        if (!loadCurrentProjection()) {
            handleProjectionSelection();
            importExtentButton.setEnabled(false);
        }
        return contents;
    }

    private void handleProjectionSelection() {
        try {
            parameters = factory.getDefaultParameters(projList.getText());

            populateParamGroup();

            setProjectionNameText(projList.getText());

            validateParameters();

        } catch (NoSuchIdentifierException e) {
            validationText.setText("Error getting parameters: "
                    + e.getLocalizedMessage());
        }

        getShell().pack(true);
    }

    private void setProjectionNameText(String projection) {
        projNameText.setText(projection.replace("_", " "));
    }

    private void validateParameters() {
        crs = null;
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setEnabled(false);

        // update edited parameter values
        for (Map.Entry<Parameter<?>, ParamUI> entry : paramMap.entrySet()) {
            if (!"null".equals(entry.getValue().text.getText())) {
                Parameter<?> param = entry.getKey();

                ParameterDescriptor<?> pd = param.getDescriptor();

                Class<?> clazz = pd.getValueClass();

                String text = entry.getValue().text.getText();

                try {
                    if (clazz.equals(Double.class)) {
                        Double d = Double.parseDouble(text);
                        param.setValue(d);
                    } else {
                        MessageBox mb = new MessageBox(this.getShell(),
                                SWT.ICON_ERROR);
                        mb.setMessage("Unexpected paramter type: "
                                + clazz.getSimpleName());
                        mb.setText("Error");
                        mb.open();
                        entry.getValue().text.setFocus();
                        return;
                    }
                } catch (NumberFormatException e) {
                    MessageBox mb = new MessageBox(this.getShell(),
                            SWT.ICON_ERROR);
                    mb.setMessage('"' + text + '"' + " is not a valid "
                            + clazz.getSimpleName() + " value.");
                    mb.setText("Error");
                    mb.open();
                    entry.getValue().text.setFocus();
                    return;
                } catch (InvalidParameterValueException e) {
                    MessageBox mb = new MessageBox(this.getShell(),
                            SWT.ICON_ERROR);
                    mb.setMessage(e.getMessage());
                    mb.setText("Error");
                    mb.open();
                    entry.getValue().text.setFocus();
                    return;
                }
            }
        }

        String name = parameters.getDescriptor().getName().getCode();
        try {
            crs = MapUtil.constructProjection(name, parameters);
        } catch (Exception e) {
            crs = null;
            validationText.setText(e.getLocalizedMessage());
        }

        if (crs == null) {
            widthUnit.setText("(?)");
            heightUnit.setText("(?)");

            setEnabledGroup(cornersGroup, false);
            setEnabledGroup(centerGroup, false);
            return;
        }

        CoordinateSystem cs = crs.getCoordinateSystem();

        widthUnit.setText('(' + cs.getAxis(0).getUnit().toString() + ')');
        heightUnit.setText('(' + cs.getAxis(1).getUnit().toString() + ')');

        setEnabledGroup(cornersGroup, true);
        setEnabledGroup(centerGroup, true);

        validateExtent();
        if (newMapGeom == null) {
            return;
        }

        validationText.setText("Valid");
        okButton.setEnabled(true);
    }

    private void setEnabledGroup(Group group, boolean enabled) {
        group.setEnabled(enabled);
        for (Control control : group.getChildren()) {
            control.setEnabled(enabled);
        }
    }

    private void validateExtent() {
        newMapGeom = null;

        if (crs == null) {
            return;
        }

        CoordinateSystem cs = crs.getCoordinateSystem();

        Coordinate ll = null;
        Coordinate ur = null;
        Coordinate center = null;
        double width = -1;
        double height = -1;
        if (cornersBtn.getSelection()) {
            // get lat/lon extent
            ll = new Coordinate();
            ll.y = validateDouble(llLatText.getText(), -90, 90);
            if (Double.isNaN(ll.y)) {
                llLatText.setFocus();
                return;
            }

            ll.x = validateDouble(llLonText.getText(), -180, 180);
            if (Double.isNaN(ll.x)) {
                llLonText.setFocus();
                return;
            }

            ur = new Coordinate();
            ur.y = validateDouble(urLatText.getText(), -90, 90);
            if (Double.isNaN(ur.y)) {
                urLatText.setFocus();
                return;
            }

            ur.x = validateDouble(urLonText.getText(), -180, 180);
            if (Double.isNaN(ur.x)) {
                urLonText.setFocus();
                return;
            }
        } else if (centerBtn.getSelection()) {
            center = new Coordinate();
            center.y = validateDouble(centerLatText.getText(), -90, 90);
            if (Double.isNaN(center.y)) {
                centerLatText.setFocus();
                return;
            }

            center.x = validateDouble(centerLonText.getText(), -180, 180);
            if (Double.isNaN(center.x)) {
                centerLonText.setFocus();
                return;
            }

            CoordinateSystemAxis axis = cs.getAxis(0);
            width = validateDouble(widthText.getText(), 1,
                    axis.getMaximumValue() - axis.getMinimumValue());
            if (Double.isNaN(width)) {
                widthText.setFocus();
                return;
            }

            axis = cs.getAxis(1);
            height = validateDouble(heightText.getText(), 1,
                    axis.getMaximumValue() - axis.getMinimumValue());
            if (Double.isNaN(height)) {
                heightText.setFocus();
                return;
            }
        }

        // create the new projection
        try {
            if (cornersBtn.getSelection()) {
                newMapGeom = MapDescriptor.createGridGeometry(crs, ll, ur);
            } else if (centerBtn.getSelection()) {
                newMapGeom = MapDescriptor.createGridGeometry(crs, center,
                        width, height);
            }

        } catch (Exception e) {
            validationText.setText(e.getLocalizedMessage());
            return;
        }

    }

    /**
     * Import the extent from the currently active display. This is specifically
     * the currently visible extent, not the entire valid area.
     */
    private void importViewExtent() {
        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
        if (container == null) {
            return;
        }
        for (IDisplayPane pane : container.getDisplayPanes()) {
            IRenderableDisplay display = pane.getRenderableDisplay();
            IDescriptor descriptor = display.getDescriptor();
            if (descriptor instanceof IMapDescriptor) {
                IMapDescriptor mapDescriptor = (IMapDescriptor) descriptor;
                IExtent extent = display.getExtent();
                if (cornersBtn.getSelection()) {
                    double[] ll = { extent.getMinX(), extent.getMinY() };
                    ll = mapDescriptor.pixelToWorld(ll);
                    double[] ur = { extent.getMaxX(), extent.getMaxY() };
                    ur = mapDescriptor.pixelToWorld(ur);
                    llLonText.setText(Double.toString(ll[0]));
                    llLatText.setText(Double.toString(ll[1]));
                    urLonText.setText(Double.toString(ur[0]));
                    urLatText.setText(Double.toString(ur[1]));
                } else if (centerBtn.getSelection()) {
                    GridEnvelope gridRange = mapDescriptor.getGridGeometry()
                            .getGridRange();
                    Envelope envelope = mapDescriptor.getGridGeometry()
                            .getEnvelope();
                    double[] center = extent.getCenter();
                    center = mapDescriptor.pixelToWorld(center);
                    centerLonText.setText(Double.toString(center[0]));
                    centerLatText.setText(Double.toString(center[1]));
                    double width = extent.getWidth() / gridRange.getSpan(0)
                            * envelope.getSpan(0);
                    double height = extent.getHeight() / gridRange.getSpan(1)
                            * envelope.getSpan(1);

                    widthText.setText(Double.toString(width));
                    heightText.setText(Double.toString(height));
                }
                validateParameters();
                return;
            }
        }
    }

    /**
     * Copy the parameters from the currently displayed map into the dialog.
     * 
     * @return true if a useable display was found and populated. False
     *         indicates that no currently active map display was found and the
     *         UI was not initialized.
     */
    private boolean loadCurrentProjection() {
        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
        if (container == null) {
            return false;
        }
        for (IDisplayPane pane : container.getDisplayPanes()) {
            IRenderableDisplay display = pane.getRenderableDisplay();
            IDescriptor descriptor = display.getDescriptor();
            if (descriptor instanceof IMapDescriptor) {
                IMapDescriptor mapDescriptor = (IMapDescriptor) descriptor;
                if (display instanceof IMapScaleDisplay) {
                    projNameText.setText(((IMapScaleDisplay) display)
                            .getScaleName());
                }
                MapProjection worldProjection = CRS
                        .getMapProjection(mapDescriptor.getCRS());

                projList.select(projList.indexOf(worldProjection.getName()));

                parameters = worldProjection.getParameterValues();

                populateParamGroup();

                GeneralGridGeometry gridGeometry = mapDescriptor
                        .getGridGeometry();
                GridEnvelope gridRange = gridGeometry.getGridRange();
                Envelope envelope = gridGeometry.getEnvelope();
                double[] ll = { gridRange.getLow(0), gridRange.getLow(1) };
                double[] ur = { gridRange.getHigh(0), gridRange.getHigh(1) };
                double[] center = {
                        (gridRange.getHigh(0) - gridRange.getLow(0)) / 2,
                        (gridRange.getHigh(1) - gridRange.getLow(1)) / 2 };
                center = mapDescriptor.pixelToWorld(center);

                ll = mapDescriptor.pixelToWorld(ll);
                ur = mapDescriptor.pixelToWorld(ur);
                llLonText.setText(Double.toString(ll[0]));
                llLatText.setText(Double.toString(ll[1]));
                urLonText.setText(Double.toString(ur[0]));
                urLatText.setText(Double.toString(ur[1]));
                centerLonText.setText(Double.toString(center[0]));
                centerLatText.setText(Double.toString(center[1]));
                widthText.setText(Double.toString(envelope.getSpan(0)));
                heightText.setText(Double.toString(envelope.getSpan(1)));

                getShell().pack(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Create widgets for modifying the {@link #parameters}. The parameters
     * field must be set before calling this method.
     */
    private void populateParamGroup() {
        for (ParamUI pui : paramMap.values()) {
            pui.label.dispose();
            pui.text.dispose();
        }
        paramMap.clear();

        for (Object obj : parameters.values()) {
            Parameter<?> param = (Parameter<?>) obj;
            String paramName = param.getDescriptor().getName().getCode();

            Label label = new Label(paramGroup, SWT.NONE);
            label.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.GRAB_HORIZONTAL));
            label.setText(paramName + " (" + param.getUnit() + ")");

            if (("semi_major".equals(paramName) || "semi_minor"
                    .equals(paramName)) && (param.getValue() == null)) {
                param.setValue(MapUtil.AWIPS_EARTH_RADIUS);
            }

            Text text = new Text(paramGroup, SWT.BORDER);
            text.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL
                    | GridData.GRAB_HORIZONTAL));
            String helpText = parameterHelpText.get(paramName.toLowerCase());
            text.setToolTipText(helpText);
            label.setToolTipText(helpText);
            paramMap.put(param, new ParamUI(label, text));

            text.setText("" + param.getValue());
        }
        paramGroup.layout();
    }

    @Override
    protected void okPressed() {
        validateParameters();

        if (newMapGeom == null) {
            return;
        }

        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();
        if (container == null) {
            try {
                container = new NewMapEditor().execute(null);
            } catch (ExecutionException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error opening new editor", e);
            }
        }
        for (IDisplayPane pane : container.getDisplayPanes()) {
            IRenderableDisplay display = pane.getRenderableDisplay();
            IMapDescriptor oldDescriptor = (IMapDescriptor) display
                    .getDescriptor();
            try {
                // Reproject by setting new grid geometry
                oldDescriptor.setGridGeometry(newMapGeom);
                // Give projection custom name if IMapScaleDisplay
                if (display instanceof IMapScaleDisplay) {
                    ((IMapScaleDisplay) display).setScaleName(projNameText
                            .getText().trim());
                }
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error setting GridGeometry: ", e);
            }

            // reset the display to fully zoomed extent
            pane.setZoomLevel(1.0f);
            pane.scaleToClientArea();
        }
        container.refresh();

        super.okPressed();
    }

    private double validateDouble(String text, double min, double max) {
        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e1) {
            validationText.setText('"' + text + '"'
                    + " is not a valid Double value.");
            return Double.NaN;
        }
        if ((value < min) || (value > max)) {
            DecimalFormat df = new DecimalFormat("0.#");
            validationText.setText("Value must be in the range ["
                    + df.format(min) + " .. " + df.format(max) + "]");
            return Double.NaN;
        }

        return value;
    }
}
