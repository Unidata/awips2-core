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

import java.util.Arrays;
import java.util.Date;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.datastructure.LoopProperties.LoopMode;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;

/**
 * Default IFrameCoordinator implementation, functionality was originally in
 * AbstractDescriptor but became too d2d dependent so it was decided to move
 * into separate class so other people may provide different implementations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- --------- --------------------------
 * Oct 18, 2011           mschenke  Initial creation
 * May 13, 2015  4461     bsteffen  Add determineFrameIndex
 * Aug 07, 2015  4700     bsteffen  Add support for SPACE_AND_TIME
 * Oct 09, 2015  4863     bsteffen  Maintain same valid time when frame
 *                                  times change.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class FrameCoordinator implements IFrameCoordinator {

    /** Interface for determining if a frame is valid */
    public static interface IFrameValidator {
        public boolean isValid(DataTime frame);
    }

    private class FrameValidator implements IFrameValidator {

        private FramesInfo frameInfo;

        private FrameValidator(FramesInfo frameInfo) {
            this.frameInfo = frameInfo;
        }

        @Override
        public boolean isValid(DataTime frame) {
            return FrameCoordinator.this.isValid(frameInfo, frame);
        }
    }

    /**
     * Default validator, should ONLY check visible flag on the DataTime
     */
    private static final IFrameValidator DEFAULT_VALIDATOR = new IFrameValidator() {
        @Override
        public boolean isValid(DataTime frame) {
            return frame.isVisible();
        }
    };

    /** Descriptor frames are coordinated for */
    protected IDescriptor descriptor;

    /** Current animation mode */
    protected AnimationMode currentAnimationMode;

    /** The current loop direction 1 = forward, -1 = reverse */
    protected int loopDirection = 1;

    /** Lock object for locking on the descriptor */
    protected Object lock;

    /**
     * FrameCoordinator constructor
     * 
     * @param descriptor
     *            the descriptor to coordinate frames for
     */
    public FrameCoordinator(IDescriptor descriptor) {
        this.descriptor = descriptor;
        currentAnimationMode = AnimationMode.Latest;
        lock = descriptor;
        if (descriptor instanceof AbstractDescriptor) {
            lock = ((AbstractDescriptor) descriptor).getLockObject();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.drawables.IFrameCoordinator#changeFrame(java
     * .util.Date)
     */
    @Override
    public void changeFrame(Date frameTime) {
        changeFrame(new DataTime(frameTime));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.drawables.IFrameCoordinator#changeFrame(com.
     * raytheon.uf.common.time.DataTime)
     */
    @Override
    public void changeFrame(DataTime frameTime) {
        // Default behavior for now
        FramesInfo info = descriptor.getFramesInfo();
        DataTime[] currTimes = info.getFrameTimes();
        int idx = Arrays.binarySearch(currTimes, frameTime);
        // Force within range
        idx = Math.min(currTimes.length - 1, Math.max(0, idx));
        descriptor.setFramesInfo(new FramesInfo(idx));
    }

    @Override
    public void changeFrame(FrameChangeOperation operation, FrameChangeMode mode) {
        // Grab the current frame information
        FramesInfo info = descriptor.getFramesInfo();
        int frameIndex = info.getFrameIndex();
        // Bad index, can't do anything
        if (frameIndex < 0) {
            return;
        }

        // This validator makes it so no times with no data will be skipped
        IFrameValidator validator = new FrameValidator(info);
        // IFrameValidator validator = DEFAULT_VALIDATOR;
        DataTime[] frames = info.getFrameTimes();
        int newIndex = frameIndex;

        synchronized (descriptor) {
            if (frames == null || frames.length == 0) {
                newIndex = 0;
            } else {
                switch (mode) {
                case TIME_ONLY: {
                    currentAnimationMode = AnimationMode.Temporal;
                    newIndex = getNextTimeIndex(frames, frameIndex, operation,
                            validator);
                    break;
                }
                case SPACE_ONLY: {
                    currentAnimationMode = AnimationMode.Vertical;
                    newIndex = getNextVerticalIndex(frames, frameIndex,
                            operation, validator);
                    break;
                }
                case TIME_AND_SPACE: {
                    newIndex = getNextDataTimeIndex(frames, frameIndex,
                            operation, validator);
                    if (operation == FrameChangeOperation.LAST) {
                        currentAnimationMode = AnimationMode.Latest;
                    } else {
                        currentAnimationMode = AnimationMode.Temporal;
                    }
                    break;
                }
                case SPACE_AND_TIME: {
                    newIndex = getNextSpaceTimeIndex(frames, frameIndex,
                            operation, validator);
                    if (operation == FrameChangeOperation.LAST) {
                        currentAnimationMode = AnimationMode.Latest;
                    } else {
                        currentAnimationMode = AnimationMode.Temporal;
                    }
                    break;
                }
                }
            }
        }

        descriptor.setFramesInfo(new FramesInfo(newIndex));
    }

    @Override
    public void changeFrame(LoopProperties loopProperties) {
        long waitTime = Long.MAX_VALUE;
        synchronized (lock) {
            FramesInfo currInfo = descriptor.getFramesInfo();

            // This validator makes it so no times with no data will be skipped
            IFrameValidator validator = new FrameValidator(currInfo);
            // IFrameValidator validator = DEFAULT_VALIDATOR;

            DataTime[] frames = currInfo.getFrameTimes();
            int frameIndex = currInfo.getFrameIndex();
            if (loopProperties == null || !loopProperties.isLooping()) {
                return;
            } else if ((currentAnimationMode == AnimationMode.Vertical)
                    && frames != null && frames.length != 0) {
                waitTime = loopDirection > 0 ? loopProperties.getFwdFrameTime()
                        : loopProperties.getRevFrameTime();
                if (frameIndex == getLastVerticalIndex(frames, frameIndex,
                        validator)) {
                    waitTime = loopProperties.getLastFrameDwell();
                } else if (frameIndex == getFirstVerticalIndex(frames,
                        frameIndex, validator)) {
                    waitTime = loopProperties.getFirstFrameDwell();
                }

            } else if ((currentAnimationMode == AnimationMode.Temporal)
                    && frames != null && frames.length != 0) {
                waitTime = loopDirection > 0 ? loopProperties.getFwdFrameTime()
                        : loopProperties.getRevFrameTime();

                if (frameIndex == getLastTimeIndex(frames, frameIndex,
                        validator)) {
                    waitTime = loopProperties.getLastFrameDwell();
                } else if (frameIndex == getFirstTimeIndex(frames, frameIndex,
                        validator)) {
                    waitTime = loopProperties.getFirstFrameDwell();
                }

            } else if ((currentAnimationMode == AnimationMode.Latest)
                    && frames != null && frames.length != 0) {
                waitTime = loopDirection > 0 ? loopProperties.getFwdFrameTime()
                        : loopProperties.getRevFrameTime();

                if (frameIndex == getNextDataTimeIndex(frames, frameIndex,
                        FrameChangeOperation.LAST, validator)) {
                    waitTime = loopProperties.getLastFrameDwell();
                } else if (frameIndex == getNextDataTimeIndex(frames,
                        frameIndex, FrameChangeOperation.FIRST, validator)) {
                    waitTime = loopProperties.getFirstFrameDwell();
                }
            }

            loopProperties.drawAfterWait(waitTime);

            if (loopProperties.isShouldDraw() && frames != null) {
                if (currentAnimationMode == AnimationMode.Latest) {
                    descriptor.setFramesInfo(new FramesInfo(
                            getNextDataTimeIndex(frames, frameIndex,
                                    loopProperties.getMode(), validator)));
                } else if (currentAnimationMode == AnimationMode.Temporal) {
                    descriptor.setFramesInfo(new FramesInfo(getNextTimeIndex(
                            frames, frameIndex, loopProperties.getMode(),
                            validator)));
                } else if (currentAnimationMode == AnimationMode.Vertical) {
                    descriptor.setFramesInfo(new FramesInfo(
                            getNextVerticalIndex(frames, frameIndex,
                                    loopProperties.getMode(), validator)));
                }
            }
        }
    }

    @Override
    public AnimationMode getAnimationMode() {
        return currentAnimationMode;
    }

    /**
     * Given the frames, and current index, return the index of the last
     * vertical frame.
     * 
     * @param frames
     *            current frames
     * @param dataIndex
     *            current index
     * @param validator
     *            object to use to determine if a frame is valid and should be
     *            used
     * @return
     */
    protected int getLastVerticalIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        for (int idx = frames.length - 1; idx >= 0; --idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt) && dt.equals(currTime, true)) {
                return idx;
            }
        }
        return dataIndex;
    }

    /**
     * Returns the first index into timesteps that has a different levelValue as
     * the DataTime at dataIndex but same time. Uses the DataTime.isVisible flag
     * to determine if a frame is valid or not
     * 
     * @param frames
     *            frames to use
     * @param dataIndex
     *            the index to start at
     * @param validator
     *            object to use to determine valid frames
     * @return the first index with a different levelValue as dataIndex but same
     *         time
     */
    protected int getFirstVerticalIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        for (int idx = 0; idx < frames.length; ++idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt) && dt.equals(currTime, true)) {
                return idx;
            }
        }
        return dataIndex;
    }

    /**
     * Get the last time index for the frames starting at the current index.
     * 
     * @param frames
     *            frames to use
     * @param dataIndex
     *            the index to start at
     * @param validator
     *            object to use to determine valid frames
     * @return last time index
     */
    protected int getLastTimeIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        double vert = currTime.getLevelValue();

        for (int idx = frames.length - 1; idx >= 0; --idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt) && vert == dt.getLevelValue()) {
                return idx;
            }
        }

        return dataIndex;
    }

    /**
     * Returns the first datatime with same levelValue as DataTime at dataIndex.
     * 
     * @param frames
     *            frame times to use
     * @param dataIndex
     *            the index to start at
     * @param validator
     *            Object to use to determine valid frames
     * @return the first index with same levelValue as dataIndex
     */
    protected int getFirstTimeIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        double vert = currTime.getLevelValue();

        for (int idx = 0; idx < frames.length; ++idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt) && vert == dt.getLevelValue()) {
                return idx;
            }
        }

        return dataIndex;
    }

    /**
     * Get the last data time index for the frames starting at the current
     * index.
     * 
     * @param frames
     *            frames to use
     * @param dataIndex
     *            the index to start at
     * @param validator
     *            object to use to determine valid frames
     * @return last data time index
     */
    protected int getLastDataTimeIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        for (int idx = frames.length - 1; idx >= 0; --idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt)) {
                return idx;
            }
        }

        return dataIndex;
    }

    /**
     * Returns the first datatime that is valid
     * 
     * @param frames
     *            frame times to use
     * @param dataIndex
     *            the index to start at
     * @param validator
     *            Object to use to determine valid frames
     * @return the first index that is valid
     */
    protected int getFirstDataTimeIndex(DataTime[] frames, int dataIndex,
            IFrameValidator validator) {
        for (int idx = 0; idx < frames.length; ++idx) {
            DataTime dt = frames[idx];
            if (validator.isValid(dt)) {
                return idx;
            }
        }

        return dataIndex;
    }

    /**
     * Given the FrameChangeOperation, figure out the next time to display
     * taking the levelValue at the times into account
     * 
     * @param dataIndex
     *            index to start at
     * @param op
     *            the operation
     * @return index to use
     */
    protected int getNextTimeIndex(DataTime[] frames, int dataIndex,
            FrameChangeOperation op, IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        double vert = currTime.getLevelValue();
        int next = dataIndex;
        switch (op) {
        case FIRST: {
            next = getFirstTimeIndex(frames, dataIndex, validator);
            break;
        }
        case LAST: {
            next = getLastTimeIndex(frames, dataIndex, validator);
            break;
        }
        case NEXT: {
            int length = frames.length;
            int idx = (dataIndex + 1) % length;
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt) && vert == dt.getLevelValue()) {
                    next = idx;
                    break;
                }

                idx = (idx + 1) % length;
            }
            break;
        }
        case PREVIOUS: {
            int idx = (dataIndex - 1);
            if (idx < 0) {
                idx = frames.length - 1;
            }
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt) && vert == dt.getLevelValue()) {
                    next = idx;
                    break;
                }

                idx--;
                if (idx < 0) {
                    idx = frames.length - 1;
                }
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the LoopMode, figure out the next time to display taking the
     * levelValue at the times into account
     * 
     * @param dataIndex
     *            index to start at
     * @param op
     *            LoopMode operation
     * @return index to use
     */
    private int getNextTimeIndex(DataTime[] frames, int dataIndex, LoopMode op,
            IFrameValidator validator) {
        int next = dataIndex;
        switch (op) {
        case Forward: {
            loopDirection = 1;
            next = getNextTimeIndex(frames, dataIndex,
                    FrameChangeOperation.NEXT, validator);
            break;
        }
        case Backward: {
            loopDirection = -1;
            next = getNextTimeIndex(frames, dataIndex,
                    FrameChangeOperation.PREVIOUS, validator);
            break;
        }
        case Cycle: {
            if (getFirstTimeIndex(frames, dataIndex, validator) == dataIndex) {
                next = getNextTimeIndex(frames, dataIndex, LoopMode.Forward,
                        validator);
            } else if (getLastTimeIndex(frames, dataIndex, validator) == dataIndex) {
                next = getNextTimeIndex(frames, dataIndex, LoopMode.Backward,
                        validator);
            } else if (loopDirection > 0) {
                next = getNextTimeIndex(frames, dataIndex, LoopMode.Forward,
                        validator);
            } else if (loopDirection < 0) {
                next = getNextTimeIndex(frames, dataIndex, LoopMode.Backward,
                        validator);
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the FrameChangeOperation, figure out the next vertical level to
     * display given the dataTime at dataIndex
     * 
     * @param dataIndex
     *            index to get dataTime
     * @param op
     *            operation to use
     * @return index to use
     */
    protected int getNextVerticalIndex(DataTime[] frames, int dataIndex,
            FrameChangeOperation op, IFrameValidator validator) {
        DataTime currTime = frames[dataIndex];
        int next = dataIndex;
        switch (op) {
        case FIRST: {
            next = getFirstVerticalIndex(frames, dataIndex, validator);
            break;
        }
        case LAST: {
            next = getLastVerticalIndex(frames, dataIndex, validator);
            break;
        }
        case NEXT: {
            int length = frames.length;
            int idx = (dataIndex + 1) % length;
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt) && currTime.equals(dt, true)) {
                    next = idx;
                    break;
                }

                idx = (idx + 1) % length;
            }
            break;
        }
        case PREVIOUS: {
            int idx = (dataIndex - 1);
            if (idx < 0) {
                idx = frames.length - 1;
            }
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt) && currTime.equals(dt, true)) {
                    next = idx;
                    break;
                }

                idx--;
                if (idx < 0) {
                    idx = frames.length - 1;
                }
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the LoopMode, figure out the next vertical level to display given
     * the dataTime at dataIndex
     * 
     * @param dataIndex
     *            index to get dataTime
     * @param op
     *            operation to use
     * @return index to use
     */
    private int getNextVerticalIndex(DataTime[] frames, int dataIndex,
            LoopMode op, IFrameValidator validator) {
        int next = dataIndex;
        switch (op) {
        case Forward: {
            loopDirection = 1;
            next = getNextVerticalIndex(frames, dataIndex,
                    FrameChangeOperation.NEXT, validator);
            break;
        }
        case Backward: {
            loopDirection = -1;
            next = getNextVerticalIndex(frames, dataIndex,
                    FrameChangeOperation.PREVIOUS, validator);
            break;
        }
        case Cycle: {
            if (getFirstVerticalIndex(frames, dataIndex, validator) == dataIndex) {
                next = getNextVerticalIndex(frames, dataIndex,
                        LoopMode.Forward, validator);
            } else if (getLastVerticalIndex(frames, dataIndex, validator) == dataIndex) {
                next = getNextVerticalIndex(frames, dataIndex,
                        LoopMode.Backward, validator);
            } else if (loopDirection > 0) {
                next = getNextVerticalIndex(frames, dataIndex,
                        LoopMode.Forward, validator);
            } else if (loopDirection < 0) {
                next = getNextVerticalIndex(frames, dataIndex,
                        LoopMode.Backward, validator);
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the FrameChangeOperation, figure out the next time/level to display
     * looping through space and time
     */
    protected int getNextSpaceTimeIndex(DataTime[] frames, int dataIndex,
            FrameChangeOperation op, IFrameValidator validator) {
        int next = dataIndex;
        switch (op) {
        case FIRST: {
            double lowestLevel = Double.POSITIVE_INFINITY;
            long earliestTimeAtLowestLevel = Long.MAX_VALUE;
            for (int i = 0; i < frames.length; i += 1) {
                DataTime dtime = frames[i];
                double level = dtime.getLevelValue().doubleValue();
                long time = dtime.getValidTimeAsDate().getTime();
                if (!validator.isValid(dtime)) {
                    continue;
                } else if (lowestLevel > level) {
                    lowestLevel = level;
                    earliestTimeAtLowestLevel = time;
                    next = i;
                } else if (lowestLevel == level
                        && earliestTimeAtLowestLevel > time) {
                    earliestTimeAtLowestLevel = time;
                    next = i;
                }
            }
            break;
        }
        case LAST: {
            double highestLevel = Double.NEGATIVE_INFINITY;
            long latestTimeAtHighestLevel = Long.MIN_VALUE;
            for (int i = 0; i < frames.length; i += 1) {
                DataTime dtime = frames[i];
                double level = dtime.getLevelValue().doubleValue();
                long time = dtime.getValidTimeAsDate().getTime();
                if (!validator.isValid(dtime)) {
                    continue;
                } else if (highestLevel < level) {
                    highestLevel = level;
                    latestTimeAtHighestLevel = time;
                    next = i;
                } else if (highestLevel == level
                        && latestTimeAtHighestLevel < time) {
                    latestTimeAtHighestLevel = time;
                    next = i;
                }
            }
            break;
        }
        case NEXT: {
            /* First try to find one at the same level, later time. */
            double frameLevel = frames[dataIndex].getLevelValue();
            long frameTime = frames[dataIndex].getValidTimeAsDate().getTime();
            long earliestTimeAtLowestLevel = Long.MAX_VALUE;
            for (int i = 0; i < frames.length; i += 1) {
                DataTime dtime = frames[i];
                double level = dtime.getLevelValue().doubleValue();
                long time = dtime.getValidTimeAsDate().getTime();
                if (!validator.isValid(dtime)) {
                    continue;
                } else if (frameLevel == level && time > frameTime
                        && time < earliestTimeAtLowestLevel) {
                    earliestTimeAtLowestLevel = time;
                    next = i;
                }
            }
            if(next == dataIndex){
                /* Second try to find the earliest time at the next level up. */
                double lowestLevel = Double.POSITIVE_INFINITY;
                earliestTimeAtLowestLevel = Long.MAX_VALUE;
                for (int i = 0; i < frames.length; i += 1) {
                    DataTime dtime = frames[i];
                    double level = dtime.getLevelValue().doubleValue();
                    long time = dtime.getValidTimeAsDate().getTime();
                    if (!validator.isValid(dtime)) {
                        continue;
                    } else if (level > frameLevel && level < lowestLevel) {
                        lowestLevel = level;
                        earliestTimeAtLowestLevel = time;
                        next = i;
                    } else if(level == lowestLevel && earliestTimeAtLowestLevel > time){
                        earliestTimeAtLowestLevel = time;
                        next = i;
                    }
                }
                if(next == dataIndex){
                    /*
                     * If there is still nothing then the current frame must be
                     * the last frame so loop back to the first frame.
                     */
                    return getNextSpaceTimeIndex(frames, dataIndex,
                            FrameChangeOperation.FIRST, validator);
                }
            }
            break;
        }
        case PREVIOUS: {
            /* First try to find one at the same level, earlier time. */
            double frameLevel = frames[dataIndex].getLevelValue();
            long frameTime = frames[dataIndex].getValidTimeAsDate().getTime();
            long latestTimeAtHighestLevel = Long.MIN_VALUE;
            for (int i = 0; i < frames.length; i += 1) {
                DataTime dtime = frames[i];
                double level = dtime.getLevelValue().doubleValue();
                long time = dtime.getValidTimeAsDate().getTime();
                if (!validator.isValid(dtime)) {
                    continue;
                } else if (frameLevel == level && time < frameTime
                        && time > latestTimeAtHighestLevel) {
                    latestTimeAtHighestLevel = time;
                    next = i;
                }
            }
            if (next == dataIndex) {
                /*
                 * Second try to find the latest time at the previous level
                 * down.
                 */
                double highestLevel = Double.NEGATIVE_INFINITY;
                latestTimeAtHighestLevel = Long.MIN_VALUE;
                for (int i = 0; i < frames.length; i += 1) {
                    DataTime dtime = frames[i];
                    double level = dtime.getLevelValue().doubleValue();
                    long time = dtime.getValidTimeAsDate().getTime();
                    if (!validator.isValid(dtime)) {
                        continue;
                    } else if (level < frameLevel && level > highestLevel) {
                        highestLevel = level;
                        latestTimeAtHighestLevel = time;
                        next = i;
                    } else if (level == highestLevel
                            && latestTimeAtHighestLevel < time) {
                        latestTimeAtHighestLevel = time;
                        next = i;
                    }
                }
                if (next == dataIndex) {
                    /*
                     * If there is still nothing then the current frame must be
                     * the first frame so loop back to the last frame.
                     */
                    return getNextSpaceTimeIndex(frames, dataIndex,
                            FrameChangeOperation.LAST, validator);
                }
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the FrameChangeOperation, figure out the next time/level to display
     * looping through time and space
     * 
     * @param dataIndex
     *            index to start at
     * @param op
     *            operation to use
     * @return index to use
     */
    protected int getNextDataTimeIndex(DataTime[] frames, int dataIndex,
            FrameChangeOperation op, IFrameValidator validator) {
        int next = dataIndex;
        switch (op) {
        case FIRST: {
            next = getFirstDataTimeIndex(frames, dataIndex, validator);
            break;
        }
        case LAST: {
            next = getLastDataTimeIndex(frames, dataIndex, validator);
            break;
        }
        case NEXT: {
            int length = frames.length;
            int idx = (dataIndex + 1) % length;
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt)) {
                    next = idx;
                    break;
                }

                idx = (idx + 1) % length;
            }
            break;
        }
        case PREVIOUS: {
            int idx = (dataIndex - 1);
            if (idx < 0) {
                idx = frames.length - 1;
            }
            DataTime dt;
            while (idx != dataIndex) {
                dt = frames[idx];
                if (validator.isValid(dt)) {
                    next = idx;
                    break;
                }

                idx--;
                if (idx < 0) {
                    idx = frames.length - 1;
                }
            }
            break;
        }
        }
        return next;
    }

    /**
     * Given the LoopMode, figure out the next time/level to display looping
     * through time and space
     * 
     * @param dataIndex
     *            index to start at
     * @param op
     *            operation to use
     * @return index to use
     */
    private int getNextDataTimeIndex(DataTime[] frames, int dataIndex,
            LoopMode op, IFrameValidator validator) {
        int next = dataIndex;
        switch (currentAnimationMode) {
        case Temporal: {
            next = getNextTimeIndex(frames, dataIndex, op, validator);
            break;
        }
        case Vertical: {
            next = getNextVerticalIndex(frames, dataIndex, op, validator);
            break;
        }
        case Latest: {
            switch (op) {
            case Backward: {
                loopDirection = -1;
                next = getNextDataTimeIndex(frames, dataIndex,
                        FrameChangeOperation.PREVIOUS, validator);
                break;
            }
            case Forward: {
                loopDirection = 1;
                next = getNextDataTimeIndex(frames, dataIndex,
                        FrameChangeOperation.NEXT, validator);
                break;
            }
            case Cycle: {
                if (dataIndex == 0) {
                    next = getNextDataTimeIndex(frames, dataIndex,
                            LoopMode.Forward, validator);
                } else if (dataIndex == frames.length - 1) {
                    next = getNextDataTimeIndex(frames, dataIndex,
                            LoopMode.Backward, validator);
                } else if (loopDirection > 0) {
                    next = getNextDataTimeIndex(frames, dataIndex,
                            LoopMode.Forward, validator);
                } else if (loopDirection < 1) {
                    next = getNextDataTimeIndex(frames, dataIndex,
                            LoopMode.Backward, validator);
                }
            }
            }

        }
        }
        return next;
    }

    /**
     * Determine if the frame time is valid and should be used
     * 
     * @param time
     * @return
     */
    protected boolean isValid(FramesInfo frameInfo, DataTime frame) {
        if (frame.isVisible()) {
            int frameIdx = -1;
            DataTime[] times = frameInfo.getFrameTimes();
            for (int i = 0; i < times.length; ++i) {
                if (times[i].equals(frame)) {
                    frameIdx = i;
                    break;
                }
            }
            if (frameIdx != -1) {
                if (thisDescriptorContainsValidResourcesForFrameIndex(
                        descriptor, frameIdx, frameInfo)) {
                    return true;
                }
                return containersOtherDescriptorsContainsValidResourcesForFrameIndex(frameIdx);
            }
        }
        return false;
    }

    // Determines if the descriptor has any resources painting at this index
    private boolean thisDescriptorContainsValidResourcesForFrameIndex(
            IDescriptor descriptor, int frameIdx, FramesInfo frameInfo) {
        for (ResourcePair rp : descriptor.getResourceList()) {
            DataTime time = getValidTimeForResource(rp, frameInfo, frameIdx);
            if (time != null) {
                return true;
            }
        }
        return false;
    }

    // Determines if the container has other descriptors with valid painting
    // resources at this index
    private boolean containersOtherDescriptorsContainsValidResourcesForFrameIndex(
            int frameIdx) {
        // TODO delete this method, it is dumb.
        IRenderableDisplay renderableDisplay = descriptor
                .getRenderableDisplay();
        if (renderableDisplay == null) {
            return false;
        }
        IDisplayPaneContainer container = renderableDisplay.getContainer();
        if (container == null) {
            return false;
        }
        for (IDisplayPane displayPane : container.getDisplayPanes()) {
            IRenderableDisplay otherRenderableDisplay = displayPane
                    .getRenderableDisplay();
            if (otherRenderableDisplay == renderableDisplay) {
                continue;
            }
            IDescriptor otherDescriptor = otherRenderableDisplay
                    .getDescriptor();
            if (thisDescriptorContainsValidResourcesForFrameIndex(
                    otherDescriptor, frameIdx, otherDescriptor.getFramesInfo())) {
                return true;
            }

        }
        return false;
    }

    private DataTime getValidTimeForResource(ResourcePair rp,
            FramesInfo frameInfo, int frameIdx) {
        AbstractVizResource<?, ?> rsc = rp.getResource();
        DataTime time = frameInfo.getTimeForResource(rsc, frameIdx);
        if (time == null && rsc != null) {
            if (rsc.getResourceData() instanceof IResourceGroup) {
                for (ResourcePair rp2 : ((IResourceGroup) rsc.getResourceData())
                        .getResourceList()) {
                    time = getValidTimeForResource(rp2, frameInfo, frameIdx);
                    if (time != null) {
                        break;
                    }
                }
            }
        } else if (rp.getProperties().isVisible() == false) {
            // Resource is not visible, set time to null
            time = null;
        }
        return time;
    }

    public AnimationMode getCurrentAnimationMode() {
        return currentAnimationMode;
    }

    public void setCurrentAnimationMode(AnimationMode currentAnimationMode) {
        this.currentAnimationMode = currentAnimationMode;
    }

    @Override
    public int determineFrameIndex(DataTime[] currentFrames, int currentIndex,
            DataTime[] newFrames) {
        if ((newFrames == null) || (newFrames.length == 0)) {
            return -1;
        }
        // Next try to get the closest time to
        if ((currentFrames != null) && (currentIndex >= 0)
                && (currentIndex < currentFrames.length)) {
            DataTime startTime = currentFrames[currentIndex];
            int dateIndex = Arrays.binarySearch(newFrames, startTime);
            if (dateIndex < 0) {
                long startValid = startTime.getMatchValid();
                if (newFrames[0].getMatchValid() > startValid) {
                    /*
                     * Previously viewed time was earlier than all current times
                     * so go as far back as possible.
                     */
                    return 0;
                } else {
                    /*
                     * Check if a new forecast time has replaced an old one so
                     * the valid time is the same even though the dataTimes
                     * aren't equal.
                     */
                    for (int i = 0; i < newFrames.length; i += 1) {
                        if (newFrames[i].getMatchValid() == startValid) {
                            dateIndex = indexToUpdateTo(currentFrames,
                                    currentIndex, newFrames, i);
                            if ((dateIndex >= 0)
                                    && (dateIndex < newFrames.length)) {
                                return dateIndex;
                            }
                        }
                    }
                }
            } else {
                dateIndex = indexToUpdateTo(currentFrames, currentIndex,
                        newFrames, dateIndex);
                if ((dateIndex >= 0) && (dateIndex < newFrames.length)) {
                    return dateIndex;
                }
            }
        }
        // if that didn't work just return the last frame
        return getLastDataTimeIndex(newFrames, -1, DEFAULT_VALIDATOR);
    }

    private int indexToUpdateTo(DataTime[] oldTimes, int oldIndex,
            DataTime[] frames, int startFrame) {
        int frameToUse = startFrame;
        IRenderableDisplay display = descriptor.getRenderableDisplay();
        if ((display != null) && (display.getContainer() != null)) {
            IDisplayPaneContainer container = display.getContainer();
            if (container.getLoopProperties().isLooping()) {
                return frameToUse;
            }
        }
        switch (currentAnimationMode) {
        case Latest: {
            if (oldIndex == getLastDataTimeIndex(oldTimes, oldIndex,
                    DEFAULT_VALIDATOR)) {
                frameToUse = getLastDataTimeIndex(frames, startFrame,
                        DEFAULT_VALIDATOR);
            }
            break;
        }
        case Temporal: {
            // was our old time the last frame for that time?
            boolean wasLastForTime = (oldIndex == getLastTimeIndex(oldTimes,
                    oldIndex, DEFAULT_VALIDATOR));
            if (wasLastForTime) {
                // check if a new time came in for our frame
                int latestForTime = getLastTimeIndex(frames, startFrame,
                        DEFAULT_VALIDATOR);
                if (latestForTime > startFrame) {
                    frameToUse = latestForTime;
                }
            }
            break;
        }
        case Vertical: {
            boolean wasLastForTime = (oldIndex == getLastVerticalIndex(
                    oldTimes, oldIndex, DEFAULT_VALIDATOR));
            if (wasLastForTime) {
                frameToUse = getLastVerticalIndex(frames, startFrame,
                        DEFAULT_VALIDATOR);
            }
        }
        }
        return frameToUse;
    }

}
