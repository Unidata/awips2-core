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

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;

import javax.measure.UnitConverter;

import tech.units.indriya.function.AbstractConverter;

/**
 * This class is an exact copy of the corresponding indriya class, in order to
 * make it visible to the {@link CustomConverterCompositionHandler}
 * in this package.
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
final class CompositionTask {

    private final BiPredicate<AbstractConverter, AbstractConverter> isNormalFormOrderWhenIdentity;

    private final BiPredicate<AbstractConverter, AbstractConverter> isNormalFormOrderWhenCommutative;

    private final BiPredicate<AbstractConverter, AbstractConverter> canReduce;

    private final BinaryOperator<AbstractConverter> doReduce;

    private AbstractConverter[] arrayOfConverters;

    CompositionTask(
            BiPredicate<AbstractConverter, AbstractConverter> isNormalFormOrderWhenIdentity,
            BiPredicate<AbstractConverter, AbstractConverter> isNormalFormOrderWhenCommutative,
            BiPredicate<AbstractConverter, AbstractConverter> canReduce,
            BinaryOperator<AbstractConverter> doReduce) {

        this.isNormalFormOrderWhenIdentity = isNormalFormOrderWhenIdentity;
        this.isNormalFormOrderWhenCommutative = isNormalFormOrderWhenCommutative;
        this.canReduce = canReduce;
        this.doReduce = doReduce;
    }

    /**
     * Description of a brute-force approach:
     * <p>
     * Given 'conversionSteps' a list of converters, where order matters, we
     * form a permutation group of all allowed permutations 'reachable' through
     * 'allowed' swapping. Swapping is allowed for 2 consecutive converters that
     * are both multiply transformations (x.isLinear()==true). We search this
     * permutation group for any sequence of converters, that allows
     * simplification: <br>
     * <br>
     * For every pair of consecutive converters within a sequence, check whether
     * a simplification is possible (a.isSimpleCompositionWith(b)), then apply
     * simplification and start over. <br>
     * <br>
     * Finally sort according to normal-form order.
     * </p>
     *
     * @param conversionSteps
     * @return
     */
    public AbstractConverter reduceToNormalForm(
            List<? extends UnitConverter> conversionSteps) {

        arrayOfConverters = conversionSteps.toArray(new AbstractConverter[] {});

        sortToNormalFormOrder(arrayOfConverters);

        while (trySimplify() > 0) {
            sortToNormalFormOrder(arrayOfConverters);
        }

        return sequenceToConverter(arrayOfConverters);
    }

    // -- HELPER

    /**
     * @returns the number of simplifications that could be found and were
     *          applied
     */
    private int trySimplify() {

        final CompositionTaskArrayAdapter<AbstractConverter> adapter = CompositionTaskArrayAdapter
                .of(arrayOfConverters);
        int simplificationCount = adapter
                .visitSequentialPairsAndSimplify((a, b) -> {
                    if (a.isIdentity()) {
                        return b;
                    }
                    if (b.isIdentity()) {
                        return a;
                    }
                    return canReduce.test(a, b) ? doReduce.apply(a, b) : null;
                });
        if (simplificationCount > 0) {
            arrayOfConverters = adapter.removeNulls(simplificationCount);
        }
        return simplificationCount;
    }

    /** sorts an array of converters to normal-form order */
    private void sortToNormalFormOrder(AbstractConverter[] arrayOfConverters) {
        final CompositionTaskBitScanner bitScanner = CompositionTaskBitScanner
                .of(arrayOfConverters, UnitConverter::isLinear);
        // a bit-set 0-0-1-1-1-0 would indicate 3rd to 5th position are allowed
        // to be put in arbitrary order
        // we sort this sub-sequences of '1's according to normal-form-order
        bitScanner.visitBitSequences((fromIndex, toIndex) -> {
            Arrays.sort(arrayOfConverters, fromIndex, toIndex, (a, b) -> {

                if (a.isIdentity()) {
                    if (b.isIdentity()) {
                        return isNormalFormOrderWhenIdentity.test(a, b) ? -1
                                : 1;
                    }
                    return -1;
                }
                if (b.isIdentity()) {
                    return 1;
                }

                return isNormalFormOrderWhenCommutative.test(a, b) ? -1 : 1;

            });
        });
    }

    /** converts an array of converters to a single converter object */
    private static AbstractConverter sequenceToConverter(
            AbstractConverter[] sequence) {
        if (sequence == null || sequence.length == 0) {
            return AbstractConverter.IDENTITY;
        }
        if (sequence.length == 1) {
            final AbstractConverter singleton = sequence[0];
            return singleton;
        }
        // fold the sequence into a binary tree
        final AbstractConverter start = new AbstractConverter.Pair(sequence[0],
                sequence[1]);
        return Arrays.stream(sequence).skip(2).reduce(start,
                (tree, next) -> new AbstractConverter.Pair(tree, next));
    }

}
