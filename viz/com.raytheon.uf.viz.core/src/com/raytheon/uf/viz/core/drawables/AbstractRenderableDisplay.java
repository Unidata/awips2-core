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
package com.raytheon.uf.viz.core.drawables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.AbstractGraphicsFactoryAdapter;
import com.raytheon.uf.viz.core.GraphicsFactory;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.preferences.ColorFactory;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceList.AddListener;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;

/**
 * 
 * Abstract renderable display class, implements common functionality between
 * all IRenderableDisplays. Note: classes extending this class should be away
 * that cloneDisplay/createNewDisplay will not work properly unless the
 * extending class has the annotation XmlRootElement above the class.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 06, 2009           bgonzale    Initial creation
 * Jun 24, 2013  2140     randerso    Added paintResource method
 * Oct 22, 2013  2491     bsteffen    Switch clone to ProcedureXmlManager
 * Dec 09, 2016  6027     bsteffen    Copy bounds in clone
 * Apr 18, 2017  6049     bsteffen    Fix race condition causing NPE
 * Mar 20, 2018  6855     njensen     Rewrote calcPixelExtent(Rectangle)	
 * Oct 02, 2019  69438    ksunil      add FRAME_NUM_IN_LOOP to the globals
 * </pre>
 * 
 * @author mschenke
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractRenderableDisplay implements IRenderableDisplay {
    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractRenderableDisplay.class);

    private static volatile RGB BACKGROUND_COLOR = null;

    protected static RGB getStartingBackgroundColor() {
        if (BACKGROUND_COLOR == null) {
            if (PlatformUI.isWorkbenchRunning()) {
                BACKGROUND_COLOR = ColorFactory.getInstance()
                        .getColor("com.raytheon.uf.viz.core.backgroundColor");
            } else {
                return new RGB(0, 0, 0);
            }
        }
        return BACKGROUND_COLOR;
    }

    /* guesstimate of cursor */
    public static final int CURSOR_HEIGHT = 18;

    protected RGB backgroundColor;
    
    /** The view area */
    private IView view;

    protected Rectangle canvasBounds;

    protected AbstractDescriptor descriptor;

    protected IGraphicsTarget initializedTarget;

    protected IDisplayPaneContainer container;

    /** The blink interval in milliseconds. */
    protected long blinkInterval = 500;

    /** The last blink time in computer epoch milliseconds */
    protected long timeLastBlink;

    protected boolean currentBlinkState;

    protected RenderableDisplayListener listener;

    private boolean swapping = false;

    private Map<String, Object> globals = new HashMap<>();

    private AbstractGraphicsFactoryAdapter graphicsAdapter;

    public AbstractRenderableDisplay() {
        super();
        this.listener = new RenderableDisplayListener();
        backgroundColor = getStartingBackgroundColor();
        setGraphicsAdapter(GraphicsFactory.getGraphicsAdapter());
    }

    public AbstractRenderableDisplay(IExtent extent, IDescriptor descriptor) {
        this();
        this.setDescriptor(descriptor);
        this.view.setExtent(extent);
    }

    @Override
    public void dispose() {
        if (this.descriptor != null) {
            descriptor.getResourceList().clear();
            this.descriptor.getResourceList()
                    .removePostAddListener(this.listener);
            this.descriptor.getResourceList()
                    .removePostRemoveListener(this.listener);
        }
        this.initializedTarget = null;
    }

    @Override
    public IExtent getExtent() {
        return this.view.getExtent();
    }

    @Override
    public int getWorldHeight() {
        return descriptor.getGridGeometry().getGridRange().getHigh(1) + 1;
    }

    @Override
    public int getWorldWidth() {
        return descriptor.getGridGeometry().getGridRange().getHigh(0) + 1;
    }

    @Override
    public void recenter(double[] center) {

        try {

            double[] p2 = descriptor.worldToPixel(center);

            // move into the view
            double[] curCenter = getView().getExtent().getCenter();

            double deltaX = (p2[0] - curCenter[0]);
            double deltaY = (p2[1] - curCenter[1]);

            getView().getExtent().shift(deltaX, deltaY);

        } catch (Exception e) {
            statusHandler.handle(Priority.SIGNIFICANT,
                    "Unable to transform map center to display coordinates", e);
        }
    }

    @Override
    public void calcPixelExtent(Rectangle clientArea) {	
        double zoomLevel = view.getZoom();	
        double[] c1 = view.getExtent().getCenter();	
        this.view.scaleToClientArea(clientArea, getDimensions());	
        double[] c2 = view.getExtent().getCenter();	
        double deltaX = (c1[0] - c2[0]);	
        double deltaY = (c1[1] - c2[1]);	
        view.getExtent().shift(deltaX, deltaY);	
        view.zoom(zoomLevel);	
    }

    @Override
    public boolean isBlinking() {
        if (blinkInterval == 0.0) {
            return false;
        }
        for (ResourcePair rp : this.descriptor.getResourceList()) {
            if (rp.getProperties().isBlinking()) {
                return true;
            } else if (rp.getResource() instanceof IResourceGroup) {
                for (ResourcePair rp2 : ((IResourceGroup) rp.getResource())
                        .getResourceList()) {
                    if (rp2.getProperties().isBlinking()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean isBlinkStateChanged() {
        return (System.currentTimeMillis()
                - this.timeLastBlink) > blinkInterval;
    }

    @Override
    public IDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * This setter is needed for JAXB
     * 
     * @param ad
     */
    protected void setAbstractDescriptor(AbstractDescriptor ad) {
        this.setDescriptor(ad);
    }

    @XmlElement(name = "descriptor")
    protected AbstractDescriptor getAbstractDescriptor() {
        return this.descriptor;
    }

    @Override
    public void setDescriptor(IDescriptor desc) {
        if (this.descriptor != null) {
            this.descriptor.getResourceList()
                    .removePostAddListener(this.listener);
            this.descriptor.getResourceList()
                    .removePostRemoveListener(this.listener);
        }

        this.descriptor = (AbstractDescriptor) desc;
        this.descriptor.getResourceList().addPostAddListener(this.listener);
        this.descriptor.getResourceList().addPostRemoveListener(this.listener);
        this.descriptor.setRenderableDisplay(this);

        customizeResourceList(this.descriptor.getResourceList());
    }

    /**
     * Customize the resource list by adding any renderable display specific
     * resources or listeners. Called right before
     * AbstractRenderableDisplay.setDescriptor finishes. Custom resources should
     * be constructed before being added. It is only recommended to add SYSTEM
     * resources in this method, non system resources such as maps and products
     * should be constructed after the fact due to serialization issues.
     * 
     * @param resourceList
     */
    protected void customizeResourceList(ResourceList resourceList) {

    }

    @Override
    public int[] getDimensions() {
        return new int[] { getWorldWidth(), getWorldHeight(), 0 };
    }

    @Override
    public IView getView() {
        return view;
    }

    @Override
    public double[] gridToScreen(double[] grid, IGraphicsTarget target) {
        return this.view.gridToScreen(grid, target);
    }

    @Override
    public double recalcZoomLevel(int[] dimensions) {
        return this.view.recalcZoomLevel(dimensions);
    }

    @Override
    public void scaleAndBias(double factor, double screenX, double screenY,
            IGraphicsTarget target) {
        this.view.scaleAndBias(factor, screenX, screenY, target);
    }

    @Override
    public void scaleToClientArea(Rectangle clientArea) {
        this.view.scaleToClientArea(clientArea, getDimensions());
    }

    @Override
    public double[] screenToGrid(double x, double y, double depth,
            IGraphicsTarget target) {
        return this.view.screenToGrid(x, y, depth, target);
    }

    @Override
    public void setBounds(Rectangle bounds) {
        this.canvasBounds = bounds;
    }

    @Override
    public Rectangle getBounds() {
        return canvasBounds;
    }

    @Override
    public void setExtent(IExtent pe) {
        this.view.setExtent(pe);
    }

    @Override
    public void shiftExtent(double[] startScreen, double[] endScreen,
            IGraphicsTarget target) {
        this.view.shiftExtent(startScreen, endScreen, target);
    }

    @Override
    public double getZoom() {
        return this.view.getZoom();
    }

    @Override
    public void zoom(double zoomLevel) {
        this.view.zoom(zoomLevel);
    }

    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        target.setBackgroundColor(backgroundColor);
    }

    @Override
    public void setup(IGraphicsTarget target) {
        this.initializedTarget = target;
        this.view.setupView(target);
    }

    protected PaintProperties calcPaintDataTime(PaintProperties paintProps,
            AbstractVizResource<?, ?> rsc) {
        paintProps.setDataTime(descriptor.getTimeForResource(rsc));
        return paintProps;
    }

    protected class RenderableDisplayListener
            implements AddListener, RemoveListener, IRefreshListener {

        @Override
        public void notifyAdd(ResourcePair rp) throws VizException {
            rp.getResource().registerListener(this);
            refreshTarget();
        }

        @Override
        public void notifyRemove(ResourcePair rp) throws VizException {

            if (rp.getResource() != null) {
                rp.getResource().unregisterListener(this);
            }

            refreshTarget();
        }

        @Override
        public void refresh() {
            refreshTarget();
        }

        private void refreshTarget() {
            IGraphicsTarget target = AbstractRenderableDisplay.this.initializedTarget;
            if (target != null) {
                target.setNeedsRefresh(true);
            }
        }

    }

    /*
     * Calculate and return the current boolean blink state.
     * 
     * @return true if current blink is on; false otherwise.
     */
    protected boolean getCurrentBlinkState() {
        if (blinkInterval <= 0) {
            return true;
        }
        if ((System.currentTimeMillis() - this.timeLastBlink) > blinkInterval) {
            currentBlinkState = !currentBlinkState;
            timeLastBlink = System.currentTimeMillis();
        }
        return currentBlinkState;
    }

    @Override
    public void clear() {

    }

    @Override
    public RGB getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void setBackgroundColor(RGB backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    @Override
    public IRenderableDisplay createNewDisplay() {
        try {
            ProcedureXmlManager jaxb = ProcedureXmlManager.getInstance();
            AbstractRenderableDisplay clonedDisplay = jaxb.unmarshal(
                    AbstractRenderableDisplay.class, jaxb.marshal(this));
            List<ResourcePair> rscsToRemove = new ArrayList<>();
            for (ResourcePair rp : clonedDisplay.getDescriptor()
                    .getResourceList()) {
                // Remove any non system resources or map resources
                if (!(rp.getProperties().isMapLayer()
                        || rp.getProperties().isSystemResource())) {
                    rscsToRemove.add(rp);
                }
            }
            for (ResourcePair rp : rscsToRemove) {
                clonedDisplay.getDescriptor().getResourceList().remove(rp);
            }
           
            clonedDisplay.setExtent(this.getExtent().clone());
            return clonedDisplay;
        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to create new display.", e);
        }
        return null;
    }

    public AbstractRenderableDisplay cloneDisplay() {
        try {
            ProcedureXmlManager jaxb = ProcedureXmlManager.getInstance();
            AbstractRenderableDisplay clonedDisplay = jaxb.unmarshal(
                    AbstractRenderableDisplay.class, jaxb.marshal(this));
            if (getExtent() != null) {
                clonedDisplay.setExtent(this.getExtent().clone());
            }
            if (getBounds() != null) {
                clonedDisplay.setBounds(getBounds());
            }
            return clonedDisplay;
        } catch (SerializationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error cloning renderable display", e);
        }
        return null;
    }

    @Override
    public void setSwapping(boolean swapping) {
        this.swapping = swapping;
    }

    @Override
    public boolean isSwapping() {
        return this.swapping;
    }

    @Override
    public Map<String, Object> getGlobalsMap() {	
        globals.put(VizConstants.FRAME_COUNT_ID,	
                getDescriptor().getFramesInfo().getFrameCount());	
        globals.put(VizConstants.FRAME_NUM_IN_LOOP,	
                getDescriptor().getFramesInfo().frameIndex);	
        return globals;	
    }

    protected long getBlinkInterval() {
        return blinkInterval;
    }

    protected void setBlinkInterval(long blinkInterval) {
        this.blinkInterval = blinkInterval;
    }

    @Override
    public IDisplayPaneContainer getContainer() {
        return container;
    }

    @Override
    public void setContainer(IDisplayPaneContainer container) {
        this.container = container;
    }

    @Override
    public void refresh() {
        listener.refresh();
    }

    @Override
    public AbstractGraphicsFactoryAdapter getGraphicsAdapter() {
        return graphicsAdapter;
    }

    @Override
    public void setGraphicsAdapter(AbstractGraphicsFactoryAdapter adapter) {
        if (this.graphicsAdapter != adapter) {
            this.graphicsAdapter = adapter;
            this.view = adapter.constructView();
        }
    }

    /**
     * Standardized method to handle paint a resource and handle errors. This is
     * usually called from the {@link #paint(IGraphicsTarget, PaintProperties)}
     * method of subclasses.
     */
    protected void paintResource(ResourcePair pair, IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        try {
            pair.getResource().paint(target, paintProps);
        } catch (Throwable e) {
//        	pair.getResource().dispose();
            pair.getProperties().setVisible(false);
            pair.getResource().dispose();
            String name = pair.getResource().getSafeName();
            throw new VizException("Paint error: " + e.getMessage()
                    + ":: The resource [" + name
                    + "] has been disabled.", e);
        }
    }
}
