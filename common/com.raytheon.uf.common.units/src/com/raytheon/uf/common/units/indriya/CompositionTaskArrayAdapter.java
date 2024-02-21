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
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;

/**
 * This class is an exact copy of the corresponding indriya class, in order to
 * make it visible to the {@link CompositionTask} copy in this package.
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
final class CompositionTaskArrayAdapter<T> {

    private final T[] array;

    public static <T> CompositionTaskArrayAdapter<T> of(T[] array) {
        return new CompositionTaskArrayAdapter<>(array);
    }

    private CompositionTaskArrayAdapter(T[] array) {
        this.array = array;
    }

    /**
     * For the underlying array visits all sequential pairs of elements.
     *
     * @param visitor
     */
    public void visitSequentialPairs(BiConsumer<T, T> visitor) {
        if (array.length < 2) {
            return;
        }
        for (int i = 1; i < array.length; ++i) {
            visitor.accept(array[i - 1], array[i]);
        }
    }

    /**
     * @param visitor
     *            must either return null (meaning no simplification found) or a
     *            simplification
     * @return the number of simplifications that could be found and were
     *         applied
     */
    public int visitSequentialPairsAndSimplify(BinaryOperator<T> visitor) {
        if (array.length < 2) {
            return 0;
        }
        int simplificationCount = 0;
        for (int i = 1; i < array.length; ++i) {
            if (array[i - 1] == null) {
                continue;
            }
            T simplification = visitor.apply(array[i - 1], array[i]);
            if (simplification != null) {
                array[i - 1] = simplification;
                array[i] = null;
                ++simplificationCount;
            }
        }
        return simplificationCount;
    }

    /**
     * @param nullCount
     *            since we know this number in advance, we use it to speed up
     *            this method
     * @return a new array with {@code nullCount} null-elements removed
     */
    public T[] removeNulls(int nullCount) {
        final T[] result = Arrays.copyOf(array, array.length - nullCount);
        int j = 0;
        for (T element : array) {
            if (element != null) {
                result[j++] = element;
            }
        }
        return result;
    }

}
