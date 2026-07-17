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
package com.raytheon.uf.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CollectionUtil}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 25, 2025 2038488    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
class TestCollectionUtil {

    @Test
    void testGetMostCommonElement1() {
        // Empty stream -> null
        Stream<Object> stream = Stream.of();

        Object result = CollectionUtil.getMostCommonElement(stream);

        assertNull(result);
    }

    @Test
    void testGetMostCommonElement2() {
        // Stream without nulls -> most common element
        Stream<Integer> stream = Stream.of(1, 2, 3, 3);

        Integer result = CollectionUtil.getMostCommonElement(stream);

        assertEquals(3, result);
    }

    @Test
    void testGetMostCommonElement3() {
        // Stream with nulls -> most common non-null element
        Stream<Integer> stream = Stream.of(null, 1, null, 1, 2, 3, null);

        Integer result = CollectionUtil.getMostCommonElement(stream);

        assertEquals(1, result);
    }
}
