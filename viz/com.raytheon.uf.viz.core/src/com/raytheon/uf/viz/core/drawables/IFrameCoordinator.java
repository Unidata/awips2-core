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

import java.util.Date;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;

/**
 * Frame coordination interface, every descriptor should have a coordinator set.
 * The frame coordinator is responsible for frame changing for looping and
 * manual changes
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 18, 2011            mschenke     Initial creation
 * May 13, 2015  4461      bsteffen     Add determineFrameIndex
 * Aug 07, 2015  4700      bsteffen     Add SPACE_AND_TIME
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public interface IFrameCoordinator {

    /**
     * Possible operations when changing frames:
     * 
     * FIRST - The first possible frame LAST - The last possible frame NEXT -
     * The next sequential frame PREVIOUS - The previous sequential frame
     */
    public static enum FrameChangeOperation {
        FIRST, LAST, NEXT, PREVIOUS
    }

    /**
     * Possible modes for changing frames.
     * 
     * <pre>
     * TIME_ONLY - Advance only using time (ignore/stationary space)
     * SPACE_ONLY - Advance only in space (ignore/stationary time)
     * TIME_AND_SPACE - Advance in time and space (the highest spatial level for the latest time)
     * SPACE_AND_TIME - Advance in space and time (the latest time for the highest spatial elevation)
     * </pre>
     * 
     * To clarify the difference between the modes below is some sample code of
     * how a loop in each mode might work. This example assumes a simplified set
     * of frames where all levels are available for all times, a real frame
     * coordinator will have significantly more complexity while trying to
     * maintain a similar ordering.
     * 
     * <strong>TIME_ONLY</strong>
     * 
     * <pre>
     * for(Date time : allTheTimes){
     *     paint(time, currentLevelValue)
     * }
     * </pre>
     * 
     * <strong>SPACE_ONLY</strong>
     * 
     * <pre>
     * for(Double levelValue : allTheLevels){
     *     paint(currentTime, levelValue)
     * }
     * </pre>
     * 
     * <strong>TIME_AND_SPACE</strong>
     * 
     * <pre>
     * for(Date time : allTheTimes){
     *     for(Double levelValue : allTheLevels){
     *         paint(time, levelValue)
     *     }
     * }
     * </pre>
     * 
     * <strong>SPACE_AND_TIME</strong>
     * 
     * <pre>
     * for(Double levelValue : allTheLevels){
     *     for(Date time : allTheTimes){
     *         paint(time, levelValue)
     *     }
     * }
     * </pre>
     */
    public static enum FrameChangeMode {
        TIME_ONLY, SPACE_ONLY, TIME_AND_SPACE, SPACE_AND_TIME
    }

    /**
     * An enum specifying the different modes of animation. A bit D2D specific.
     * Need to figure out how another perspective could use this kind of
     * interface and adapt it
     */
    public static enum AnimationMode {
        Vertical, Temporal, Latest
    }

    /**
     * Tell the coordinator to change frame based on the loop properties
     * 
     * @param loopProperties
     */
    public void changeFrame(LoopProperties loopProperties);

    /**
     * Tell the coordinator to change the frame given the mode and operation
     * 
     * @param operation
     * @param mode
     */
    public void changeFrame(FrameChangeOperation operation, FrameChangeMode mode);

    /**
     * Tell the coordinator to change the frame given the desired time
     * 
     * @param frameTime
     */
    public void changeFrame(Date frameTime);

    /**
     * Tell the coordinator to change the frame given the desired time
     * 
     * @param frameTime
     */
    public void changeFrame(DataTime frameTime);

    /**
     * Get the coordinators current animation mode
     * 
     * @return
     */
    public AnimationMode getAnimationMode();

    /**
     * Used when changing the frames in the descriptor to determine which frame
     * time should be displayed when the frames are changed. Because this method
     * can be called from the descriptor while the frames are changing, it does
     * not change the currently displayed frame but instead returns the index
     * that should be displayed.
     */
    public int determineFrameIndex(DataTime[] currentFrames,
            int currentIndex, DataTime[] newFrames);
}
