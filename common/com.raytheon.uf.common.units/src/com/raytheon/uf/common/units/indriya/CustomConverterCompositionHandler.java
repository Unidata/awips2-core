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
package com.raytheon.uf.common.units.indriya;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import com.raytheon.uf.common.units.PrimitiveAddConverter;
import com.raytheon.uf.common.units.PrimitiveMultiplyConverter;

import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.Calculus;
import tech.units.indriya.function.ConverterCompositionHandler;
import tech.units.indriya.function.PowerOfIntConverter;

/**
 * {@link ConverterCompositionHandler} yielding a normal-form.
 *
 * This class is a copy of indriya's internal
 * UnitCompositionHandlerYieldingNormalForm, with normalFormOrder updated to
 * include custom AWIPS converters. This is primarily to prevent exceptions
 * caused by missing entries (specifically noticed when loading goes-r Day
 * Convection data).
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 19, 2024 2036778    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class CustomConverterCompositionHandler
        implements ConverterCompositionHandler {

    private final Map<Class<? extends AbstractConverter>, Integer> normalFormOrder;

    public CustomConverterCompositionHandler() {
        Map<Class<? extends AbstractConverter>, Integer> tempNormalFormOrder = new HashMap<>(
                Calculus.getNormalFormOrder());

        // Map custom converters to the first available integers
        int i = 0;
        for (Class<? extends AbstractConverter> converterClass : List
                .of(PrimitiveMultiplyConverter.class, PrimitiveAddConverter.class)) {
            while (true) {
                if (!tempNormalFormOrder.containsValue(i)) {
                    tempNormalFormOrder.put(converterClass, i);
                    break;
                }
                ++i;
            }
        }
        normalFormOrder = Collections.unmodifiableMap(tempNormalFormOrder);
    }

    @Override
    public AbstractConverter compose(AbstractConverter a, AbstractConverter b,
            BiPredicate<AbstractConverter, AbstractConverter> canReduce,
            BinaryOperator<AbstractConverter> doReduce) {

        if (a.isIdentity()) {
            if (b.isIdentity()) {
                return isNormalFormOrderWhenIdentity(a, b) ? a : b;
            }
            return b;
        }
        if (b.isIdentity()) {
            return a;
        }

        if (canReduce.test(a, b)) {
            return doReduce.apply(a, b);
        }

        final boolean commutative = a.isLinear() && b.isLinear();
        final boolean swap = commutative
                && !isNormalFormOrderWhenCommutative(a, b);

        final AbstractConverter.Pair nonSimplifiedForm = swap
                ? new AbstractConverter.Pair(b, a)
                : new AbstractConverter.Pair(a, b);

        return new CompositionTask(this::isNormalFormOrderWhenIdentity,
                this::isNormalFormOrderWhenCommutative, canReduce, doReduce)
                        .reduceToNormalForm(
                                nonSimplifiedForm.getConversionSteps());

    }

    // -- HELPER

    private boolean isNormalFormOrderWhenIdentity(AbstractConverter a,
            AbstractConverter b) {
        if (a.getClass().equals(b.getClass())) {
            return true;
        }
        return normalFormOrder.get(a.getClass()) <= normalFormOrder
                .get(b.getClass());
    }

    private boolean isNormalFormOrderWhenCommutative(AbstractConverter a,
            AbstractConverter b) {
        if (a.getClass().equals(b.getClass())) {
            if (a instanceof PowerOfIntConverter) {
                return ((PowerOfIntConverter) a)
                        .getBase() <= ((PowerOfIntConverter) b).getBase();
            }
            return true;
        }

        Integer orderA = Objects.requireNonNull(
                normalFormOrder.get(a.getClass()),
                () -> String.format(
                        "no normal-form order defined for class '%s'",
                        a.getClass().getName()));
        Integer orderB = Objects.requireNonNull(
                normalFormOrder.get(b.getClass()),
                () -> String.format(
                        "no normal-form order defined for class '%s'",
                        b.getClass().getName()));

        return orderA <= orderB;
    }

    /**
     * Set an instance of this converter composition handler as the current
     * handler.
     *
     * @return null (has to be non-void to call from spring)
     */
    public static Object setAsConverterCompositionHandler() {
        AbstractConverter.UNIT_COMPOSITION_HANDLER = new CustomConverterCompositionHandler();
        return null;
    }
}
