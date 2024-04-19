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
package com.raytheon.uf.common.style;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * Unit tests for {@link ParamLevelMatchCriteria}
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 22, 2024 2035776    bines       Initial creation
 *
 * </pre>
 *
 * @author bines
 */
class TestParamLevelMatchCriteria {

    private ParamLevelMatchCriteria paramCriteria;

    private List<String> itemsList;

    private String itemToMatch;

    @BeforeEach
    public void setup() {
        paramCriteria = new ParamLevelMatchCriteria();
        itemsList = new ArrayList<>();
        itemToMatch = "";
    }

    @Test
    public void testregexMatches1() {
        // No Regex values test
        itemsList.add("radar-one");
        itemsList.add("radar-two");
        itemsList.add("radar-three");

        // No Match
        itemToMatch = "radar-four";
        assertFalse(paramCriteria.regexMatches(itemsList, itemToMatch));

        // Match
        itemToMatch = "radar-one";
        assertTrue(paramCriteria.regexMatches(itemsList, itemToMatch));
    }

    @Test
    public void testregexMatches2() {
        // Regex value in itemToMatch
        itemToMatch = "radar-.*";

        // No Match
        itemsList.add("item1");
        itemsList.add("item2");
        itemsList.add("item3");
        assertFalse(paramCriteria.regexMatches(itemsList, itemToMatch));

        // Match
        itemsList.add("radar-one");
        assertTrue(paramCriteria.regexMatches(itemsList, itemToMatch));
    }

    @Test
    public void testregexMatches3() {
        // Edge case, Regex value in itemsList
        itemsList.add("item1");
        itemsList.add("item2");
        itemsList.add("item3");
        itemsList.add("radar-.*");

        // No Match
        itemToMatch = "radar";
        assertFalse(paramCriteria.regexMatches(itemsList, itemToMatch));

        // Match
        itemToMatch = "radar-one";
        assertTrue(paramCriteria.regexMatches(itemsList, itemToMatch));
    }

}
