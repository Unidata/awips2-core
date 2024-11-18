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

import java.util.BitSet;
import java.util.function.Predicate;

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
final class CompositionTaskBitScanner {

    @FunctionalInterface
    public interface BiIntConsumer {
        void accept(int i, int j);
    }

    private final BitSet bitSet;

    private CompositionTaskBitScanner(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    /**
     * @param array
     * @param bitTest
     * @return BitScanner, that holds internally a BitSet, that represents with
     *         zeros and ones, whether the specified predicate {@code bitTest}
     *         is false or true with respect to the elements of the specified
     *         {@code array}
     */
    public static <T> CompositionTaskBitScanner of(T[] array,
            Predicate<T> bitTest) {
        final BitSet mask = new BitSet(array.length);
        int bitIndex = 0;
        for (T element : array) {
            if (bitTest.test(element)) {
                mask.set(bitIndex);
            }
            bitIndex++;
        }
        return new CompositionTaskBitScanner(mask);
    }

    /**
     * This BitScanner holds internally a BitSet. The specified {@code visitor}
     * is called for each sequence of consecutive ones, where each such call
     * passes over 2 int parameters i, j.
     * <p>
     * i .. zero based start index of the processed one-sequence<br/>
     * j .. length of the processed one-sequence<br/>
     * <p>
     * Eg. given an internal BitSet represented by eg. 0-0-1-0-1-1-1-0, the
     * resulting visitor calls would be:<br/>
     * visitor.accept(2, 1) - start at 2, length = 1<br/>
     * visitor.accept(4, 3) - start at 4, length = 3<br/>
     *
     * @param visitor
     */
    public void visitBitSequences(BiIntConsumer visitor) {
        int scanPointer = 0;
        int nextSetBit;
        while ((nextSetBit = bitSet.nextSetBit(scanPointer)) > -1) {
            int nextClearBit = bitSet.nextClearBit(nextSetBit);
            if (nextClearBit == -1) {
                // only '1's till the end
                visitor.accept(nextSetBit, bitSet.size());
                return;
            }
            scanPointer = nextClearBit;
            visitor.accept(nextSetBit, nextClearBit);
        }
    }
}
