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
package com.raytheon.uf.viz.core.rsc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Unit tests for {@link AbstractRequestableResourceData}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 9, 2024  2036517    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
class TestAbstractRequestableResourceData {

    private static final String KEY1 = "key1";

    private static final String KEY2 = "key2";

    private static final String VAL1 = "val1";

    private static final String VAL2 = "val2";

    private static final RequestConstraint RC1 = new RequestConstraint(VAL1);

    private static final RequestConstraint RC2 = new RequestConstraint(VAL2);

    private TestConcreteRequestableResourceData rscData;

    @BeforeEach
    void setupBeforeEach() {
        rscData = new TestConcreteRequestableResourceData();
    }

    static Stream<Arguments> provideParamsForGetConstraint() {
        return Stream.of(
                // Null map -> null constraint
                Arguments.of(null, KEY1, null),
                // Empty map -> null constraint
                Arguments.of(new HashMap<>(), KEY1, null),
                // Request null key -> null constraint
                Arguments.of(new HashMap<>(Map.of(KEY2, RC2)), null, null),
                // Map without requested key -> null constraint
                Arguments.of(new HashMap<>(Map.of(KEY2, RC2)), KEY1, null),
                // Map with requested key -> that key's constraint
                Arguments.of(new HashMap<>(Map.of(KEY1, RC1)), KEY1, RC1));
    }

    @ParameterizedTest
    @MethodSource("provideParamsForGetConstraint")
    void testGetConstraint(HashMap<String, RequestConstraint> metadataMap,
            String key, RequestConstraint expectedConstraint) {
        rscData.setMetadataMap(metadataMap);

        RequestConstraint actualConstraint = rscData.getConstraint(key);

        assertEquals(expectedConstraint, actualConstraint);
    }

    static Stream<Arguments> provideParamsForGetConstraintValue() {
        return Stream.of(
                // Null map -> null value
                Arguments.of(null, KEY1, null),
                // Empty map -> null value
                Arguments.of(new HashMap<>(), KEY1, null),
                // Request null key -> null value
                Arguments.of(new HashMap<>(Map.of(KEY2, RC2)), null, null),
                // Map without requested key -> null value
                Arguments.of(new HashMap<>(Map.of(KEY2, RC2)), KEY1, null),
                // Map with requested key -> that key's constraint value
                Arguments.of(new HashMap<>(Map.of(KEY1, RC1)), KEY1, VAL1));
    }

    @ParameterizedTest
    @MethodSource("provideParamsForGetConstraintValue")
    void testGetConstraintValue(HashMap<String, RequestConstraint> metadataMap,
            String key, String expectedVal) {
        rscData.setMetadataMap(metadataMap);

        String actualVal = rscData.getConstraintValue(key);

        assertEquals(expectedVal, actualVal);
    }

    private static class TestConcreteRequestableResourceData
            extends AbstractRequestableResourceData {

        @Override
        protected AbstractVizResource<?, ?> constructResource(
                LoadProperties loadProperties, PluginDataObject[] objects)
                throws VizException {
            return null;
        }
    }
}
