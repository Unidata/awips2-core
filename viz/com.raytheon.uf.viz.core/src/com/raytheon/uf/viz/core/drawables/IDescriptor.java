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

import java.util.Map;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;

/**
 * An IDescriptor describes the interface to renderable displays related to a
 * List of {@link AbstractVizResource}s and their data to display. This
 * interface supports:
 * <ul>
 * <li>Coordinate Reference System of the display (e.g. latlon, lambert
 * conformal, cartesian)</li>
 * <li>Frames information (times to display)</li>
 * <li>Grid geometry of the render space (see below)</li>
 * <li>ResourceList of resources on the display</li>
 * <li>Time Matcher (matches the differing times of resources to shared frames)</li>
 * </ul>
 * 
 * Within Viz displays there are typically four different spaces (areas)
 * describing x, y coordinates. When comparing coordinates to data or user
 * input, it's important that the coordinates being compared are in the same
 * space. The typical spaces are:
 * <ul>
 * <li>Render space (aka pixel space or GL space): A cartesian coordinate system
 * that covers the entirety of what can be zoomed or panned to within the
 * display. Used at the graphics layers. Render space is directly proportional
 * to CRS space.</li>
 * <li>Screen space (aka SWT space or canvas space): A cartesian coordinate
 * system related to the x,y coordinates on the screen itself, such as a
 * particular pixel location or the mouse location. For a single render/paint
 * operation, this space is proportional to the render space and CRS space. A
 * zoom, pan, frame change, etc (ie new render) will alter the ratio to the
 * other spaces.</li>
 * <li>CRS space (aka projection): A coordinate system describing the projection
 * of the display, typically maps. Examples include latlon, mercator,
 * stereographic, lambert conformal, or cartesian (non-maps). CRS space is
 * directly proportional to render space.</li>
 * <li>LatLon space (aka world space or WGS84): The most common CRS used to
 * describe points on the Earth. Useful for map coordinate operations but less
 * accurate for grid or shape operations that must take into into account the
 * fact that the Earth is a spheroid for accurate displays.</li>
 * </ul>
 * 
 * TODO Establish consistent and unambiguous names for the different spaces, and
 * begin improving method signatures to avoid confusion and ambiguity.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 *   
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Sep 04, 2007           chammack  Initial Creation.
 * Oct 22, 2009  3348     bsteffen  added ability to limit number of frames
 * Feb 10, 2015  3974     njensen   Improved javadoc
 * May 13, 2015  4461     bsteffen  Add another FramesInfo constructor.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public interface IDescriptor extends IResourceGroup {

    public static class FramesInfo {

        boolean setIndex = false;

        boolean setFrames = false;

        boolean setMap = false;

        DataTime[] frameTimes = null;

        int frameIndex;

        Map<AbstractVizResource<?, ?>, DataTime[]> timeMap;

        /**
         * Constructor to use if wanting to change frames, index, and time map
         * 
         * @param frameTimes
         * @param frameIndex
         */
        public FramesInfo(DataTime[] frameTimes, int frameIndex,
                Map<AbstractVizResource<?, ?>, DataTime[]> timeMap) {
            this.frameTimes = frameTimes;
            this.frameIndex = frameIndex;
            this.timeMap = timeMap;
            setFrames = true;
            setIndex = true;
            setMap = true;
        }

        /**
         * Constructor to use if wanting to change frames, and time map
         * 
         * @param frameTimes
         * @param timeMap
         */
        public FramesInfo(DataTime[] frameTimes,
                Map<AbstractVizResource<?, ?>, DataTime[]> timeMap) {
            this.frameTimes = frameTimes;
            this.timeMap = timeMap;
            setFrames = true;
            setMap = true;
        }

        /**
         * Constructor to use if wanting to change both frames and index
         * 
         * @param frameTimes
         * @param frameIndex
         */
        public FramesInfo(DataTime[] frameTimes, int frameIndex) {
            this.frameTimes = frameTimes;
            this.frameIndex = frameIndex;
            setFrames = true;
            setIndex = true;
        }

        public FramesInfo(Map<AbstractVizResource<?, ?>, DataTime[]> timeMap) {
            this.timeMap = timeMap;
            this.setMap = true;
        }

        /**
         * Constructor to use if only wanting to change frames
         * 
         * @param frameTimes
         */
        public FramesInfo(DataTime[] frameTimes) {
            this.frameTimes = frameTimes;
            setFrames = true;
        }

        /**
         * Constructor to use if only wanted to change index
         * 
         * @param frameIndex
         */
        public FramesInfo(int frameIndex) {
            this.frameIndex = frameIndex;
            setIndex = true;
        }

        public DataTime[] getFrameTimes() {
            return frameTimes;
        }

        public int getFrameIndex() {
            return frameIndex;
        }

        public int getFrameCount() {
            return (frameTimes == null ? 0 : frameTimes.length);
        }

        public Map<AbstractVizResource<?, ?>, DataTime[]> getTimeMap() {
            return timeMap;
        }

        public DataTime getTimeForResource(AbstractVizResource<?, ?> rsc) {
            return getTimeForResource(rsc, getFrameIndex());
        }

        public DataTime getTimeForResource(AbstractVizResource<?, ?> rsc,
                int idx) {
            DataTime[] dt = timeMap.get(rsc);
            return getFrame(dt, idx);
        }

        public DataTime getCurrentFrame() {
            return getFrame(frameTimes, frameIndex);
        }

        private DataTime getFrame(DataTime[] frames, int idx) {
            if (frames == null
                    || frames.length <= idx
                    || idx < 0
                    || (frameTimes != null && frameTimes.length > idx
                            && frameTimes[idx] != null && !frameTimes[idx]
                                .isVisible())) {
                return null;
            }
            return frames[idx];
        }
    }

    /** The default width of the world in pixels */
    public static final int DEFAULT_WORLD_WIDTH = 20000;

    /** The default height of the world in pixels */
    public static final int DEFAULT_WORLD_HEIGHT = 10000;

    /**
     * Possible operations when changing frames:
     * 
     * FIRST - The first possible frame LAST - The last possible frame NEXT -
     * The next sequential frame PREVIOUS - The previous sequential frame
     * 
     * @deprecated Use IFrameCoordinator.FrameChangeOperation and call
     *             getFrameCoordinator().changeFrame(...)
     */
    @Deprecated
    public static enum FrameChangeOperation {
        FIRST, LAST, NEXT, PREVIOUS
    }

    /**
     * Possible modes for changing frames
     * 
     * TIME_ONLY - Advance only using time (ignore/stationary space) SPACE_ONLY
     * - Advance only in space (ignore/stationary time) TIME_AND_SPACE - Advance
     * in time and space (the highest spatial level
     * 
     * @deprecated Use IFrameCoordinator.FrameChangeMode and call
     *             getFrameCoordinator().changeFrame(...)
     */
    @Deprecated
    public static enum FrameChangeMode {
        TIME_ONLY, SPACE_ONLY, TIME_AND_SPACE
    }

    public static interface IFrameChangedListener {
        void frameChanged(IDescriptor descriptor, DataTime oldTime,
                DataTime newTime);
    }

    /**
     * Add a frame change listener to the descriptor
     * 
     * @param listener
     */
    public void addFrameChangedListener(IFrameChangedListener listener);

    /**
     * Remove a frame change listener from the descriptor
     * 
     * @param listener
     */
    public void removeFrameChangedListener(IFrameChangedListener listener);

    /**
     * Get the number of time frames in the descriptor
     * 
     * @return the frame count
     * @deprecated Use getFramesInfo() then use getFrameCount() on FramesInfo
     */
    @Deprecated
    public abstract int getFrameCount();

    /**
     * Get the current frame of the map descriptor
     * 
     * @return the current frame
     * @deprecated Use getFramesInfo() for thread safe use!
     * 
     */
    @Deprecated
    public abstract int getCurrentFrame();

    /**
     * Set the current frame of the map descriptor
     * 
     * @param frame
     *            the current frame number
     * 
     * @deprecated Use setFramesInfo(...) for thread safe use!
     */
    @Deprecated
    public abstract void setFrame(int frame);

    /**
     * Return the times for frames
     * 
     * @return times of the frames
     * @deprecated Use getFramesInfo() for thread safe use!
     */
    @Deprecated
    public DataTime[] getFrames();

    /**
     * Get coordinate reference system
     * 
     * @return the coordinate reference system
     */
    public abstract CoordinateReferenceSystem getCRS();

    /**
     * Set the number of frames in the display
     * 
     * @param frameCount
     *            the number of frames
     */
    public abstract void setNumberOfFrames(int frameCount);

    /**
     * Return the number of frames in the display
     * 
     * @return the number of frames
     */
    public abstract int getNumberOfFrames();

    /**
     * Limit the number of frames that will actually be displayed to
     * min(frameCount, this.frameCount). This function should be used if so the
     * descriptor can remember the "real" number of frames when you call
     * unlimitNumberOfFrames. If you need to control numberOfFrames you should
     * probably be calling setNumberOfFrames instead
     * 
     * @param frameCount
     *            the maximum number of frames to displayed
     * @return true if this effects the number of frames displayed(you need to
     *         redo time matching)
     */
    public abstract boolean limitNumberOfFrames(int frameCount);

    /**
     * remove the limit on the number of frames displayed
     * 
     * @return true if this effects the number of frames displayed(you need to
     *         redo time matching)
     */
    public abstract boolean unlimitNumberOfFrames();

    /**
     * Get the DataTimes of all of the frames of the display
     * 
     * @return the dataTimes
     * @deprecated Use getFramesInfo() for thread safe use!
     */
    @Deprecated
    public DataTime[] getDataTimes();

    /**
     * Set the data times
     * 
     * @param dataTimes
     * @deprecated Use setFramesInfo(...) for thread safe use!
     */
    @Deprecated
    public void setDataTimes(DataTime[] dataTimes);

    /**
     * Return the grid geometry
     * 
     * @return the grid geometry
     */
    public abstract GeneralGridGeometry getGridGeometry();

    /**
     * Set the geometry for the descriptor
     * 
     * @param geometry
     * @throws VizException
     */
    public abstract void setGridGeometry(GeneralGridGeometry geometry)
            throws VizException;

    /**
     * Change a frame given a specified operation mode
     * 
     * @param operation
     *            the operation to perform (see FrameChangeOperation)
     * 
     * @param mode
     *            the mode to use (see FrameChangeMode)
     * 
     * @deprecated Use getFrameCoordinator().changeFrame(...) with
     *             IFrameCoordinator.FrameChangeOperation/FrameChangeMode
     */
    @Deprecated
    public abstract void changeFrame(FrameChangeOperation operation,
            FrameChangeMode mode);

    /**
     * Convenience method to transform a set of pixel coordinates to world
     * coordinates
     * 
     * @param pixel
     *            the pixel coordinates (x, y)
     * @return the world coordinates (x, y)
     */
    public abstract double[] pixelToWorld(double[] pixel);

    /**
     * Convenience method to transform a set of world coordinates to pixel
     * coordinates
     * 
     * @param worldPixel
     *            an array of two of world coordinates (x, y)
     * @return the pixel coordinates (x, y)
     */
    public abstract double[] worldToPixel(double[] worldPixel);

    /**
     * @return the timeMatcher
     */
    public AbstractTimeMatcher getTimeMatcher();

    /**
     * @param timeMatcher
     *            the timeMatcher to set
     */
    public void setTimeMatcher(AbstractTimeMatcher timeMatcher);

    /**
     * Re-does time matching for the descriptor
     * 
     * @throws VizException
     */
    public void redoTimeMatching() throws VizException;

    /**
     * Synchronize time matching with the other descriptor
     * 
     * @param other
     */
    public void synchronizeTimeMatching(IDescriptor other);

    /**
     * Determine what time the resource should be drawn at
     * 
     * @param rsc
     * @return the data time for the resource that corresponds to the current
     *         frame
     */
    public DataTime getTimeForResource(AbstractVizResource<?, ?> rsc);

    /**
     * Get the renderable display the descriptor is loaded to
     * 
     * @return the renderable display
     */
    public IRenderableDisplay getRenderableDisplay();

    /**
     * Set the renderable display for the descriptor, NOTE: descriptor should be
     * == to descriptor.getRenderableDisplay().getDescriptor()
     * 
     * @param display
     *            display to set
     * 
     */
    public void setRenderableDisplay(IRenderableDisplay display);

    /**
     * Determines if this descriptor can load resources from another descriptor
     * 
     * @param other
     * @return if the descriptor are compatible
     */
    public boolean isCompatible(IDescriptor other);

    /**
     * Thread safe method of setting the frame information including frame times
     * and/or frame index.
     * 
     * @param info
     */
    public void setFramesInfo(FramesInfo info);

    /**
     * Thread safe method of getting the frame information including frame times
     * and frame index
     * 
     * @return the FramesInfo
     */
    public FramesInfo getFramesInfo();

    /**
     * Get the frame coordination object
     * 
     * @return the frame coordination object for the descriptor
     */
    public IFrameCoordinator getFrameCoordinator();
}
