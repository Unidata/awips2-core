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

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ContourSupport}.
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
@ExtendWith(MockitoExtension.class)
class TestContourSupport {

    @Mock
    private ContourGroup group1;

    @Mock
    private ContourGroup group2;

    @Test
    void testDisposeContourGroups1() {
        // Null groups -> nothing to do, no exceptions
        ContourGroup[] groups = null;

        ContourSupport.disposeContourGroups(groups);
    }

    @Test
    void testDisposeContourGroups2() {
        // Empty groups -> nothing to do, no exceptions
        ContourGroup[] groups = {};

        ContourSupport.disposeContourGroups(groups);
    }

    @Test
    void testDisposeContourGroups3() {
        /*
         * Pass in array including valid groups and null groups -> verify that
         * groups are disposed and nulls don't cause any issues
         */
        ContourGroup[] groups = { null, group1, null, group2, null };

        ContourSupport.disposeContourGroups(groups);

        verify(group1).dispose();
        verify(group2).dispose();
    }

}
