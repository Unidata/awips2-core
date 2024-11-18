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
package com.raytheon.viz.core.contours;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.raytheon.uf.viz.core.drawables.IWireframeShape;

/**
 * Unit tests for {@link ContourGroup}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 16, 2024 2037631    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
class TestContourGroup {

    @Test
    void testDispose1() {
        // No shapes -> nothing to do, no exceptions
        ContourGroup group = new ContourGroup();

        group.dispose();
    }

    @Test
    void testDispose2() {
        // Only pos shape -> it's disposed
        ContourGroup group = new ContourGroup();
        group.posValueShape = mock(IWireframeShape.class);

        group.dispose();

        verify(group.posValueShape).dispose();
    }

    @Test
    void testDispose3() {
        // Only neg shape -> it's disposed
        ContourGroup group = new ContourGroup();
        group.negValueShape = mock(IWireframeShape.class);

        group.dispose();

        verify(group.negValueShape).dispose();
    }

    @Test
    void testDispose4() {
        // Both pos and neg shape -> both are disposed
        ContourGroup group = new ContourGroup();
        group.posValueShape = mock(IWireframeShape.class);
        group.negValueShape = mock(IWireframeShape.class);

        group.dispose();

        verify(group.posValueShape).dispose();
        verify(group.negValueShape).dispose();
    }
}
