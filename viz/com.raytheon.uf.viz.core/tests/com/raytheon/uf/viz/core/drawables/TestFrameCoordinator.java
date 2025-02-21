/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.viz.core.drawables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.IFrameCoordinator.AnimationMode;

/**
 * Unit tests for {@link FrameCoordinator}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 11, 2025 2038488    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
class TestFrameCoordinator {

    @Test
    void testMatchFrameChange() {
        IDescriptor desc1 = mock(IDescriptor.class);
        FrameCoordinator fc1 = new FrameCoordinator(desc1);
        fc1.currentAnimationMode = AnimationMode.Temporal;
        fc1.loopDirection = 1;
        IDescriptor desc2 = mock(IDescriptor.class);
        FramesInfo otherFramesInfo = new FramesInfo(2);
        when(desc2.getFramesInfo()).thenReturn(otherFramesInfo);
        FrameCoordinator fc2 = new FrameCoordinator(desc2);
        fc2.currentAnimationMode = AnimationMode.Vertical;
        fc2.loopDirection = -1;

        fc1.matchFrameChange(fc2);

        assertEquals(AnimationMode.Vertical, fc1.currentAnimationMode);
        assertEquals(-1, fc1.loopDirection);
        /*
         * Ensure the frame index and only the frame index is processed, so that
         * we don't copy over other things that we don't want like frame times
         */
        verify(desc1)
                .setFramesInfo(argThat(argument -> argument != otherFramesInfo
                        && argument.frameIndex == 2));
    }
}
