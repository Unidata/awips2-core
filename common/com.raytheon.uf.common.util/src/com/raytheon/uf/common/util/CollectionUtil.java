/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for collection types.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 13, 2012 740        djohnson     Initial creation
 * Feb 12, 2013 1543       djohnson     Add combining of arrays.
 * 10/8/2013    1682       bphillip     Added subList, removeNulls, and removeAllInstancesOfObject methods
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public final class CollectionUtil {

    private CollectionUtil() {

    }

    /**
     * Check whether an array is null or empty.
     * 
     * @param array
     *            the array
     * @return true if the array is null or empty
     */
    public static <T> boolean isNullOrEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Check whether a collection is null or empty.
     * 
     * @param coll
     *            the collection
     * @return true if the collection is null or empty
     */
    public static boolean isNullOrEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * Create a set from the specified items. Assumes the items can be stored in
     * a {@link HashSet}.
     * 
     * @param items
     *            the items
     * @return the set
     */
    public static <T> Set<T> asSet(T... items) {
        return new HashSet<T>(Arrays.asList(items));
    }

    /**
     * Combine two arrays into a single array.
     * 
     * @param objArray1
     *            the first array
     * @param objArray2
     *            the second array
     * @return the combined array, or null if both array references are null
     */
    public static <T> T[] combine(Class<T> elementClass, T[] objArray1,
            T[] objArray2) {
        if (objArray1 == null && objArray2 != null) {
            return objArray2;
        } else if (objArray2 == null && objArray1 != null) {
            return objArray1;
        } else if (objArray1 == null && objArray2 == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(elementClass, objArray1.length
                + objArray2.length);

        System.arraycopy(objArray1, 0, array, 0, objArray1.length);
        System.arraycopy(objArray2, 0, array, objArray1.length,
                objArray2.length);
        return array;
    }

    /**
     * Utility function to convert an array of objects into a list. The array
     * may contain collections. Each member of the collection is added to the
     * returned list.
     * 
     * @param objects
     *            The objects to be moved to a list
     * @return The compiled list of objects
     */
    @SuppressWarnings("unchecked")
    public static <T extends Object> List<T> asUnembeddedList(T... objects) {
        List<T> retVal = new ArrayList<T>();
        for (T obj : objects) {
            if (obj instanceof Collection<?>) {
                Collection<?> coll = (Collection<?>) obj;
                for (Object obj2 : coll) {
                    retVal.add((T) obj2);
                }
            } else {
                retVal.add(obj);
            }
        }
        return retVal;
    }

    /**
     * Extracts a sub-list of the given from the given list starting at
     * startIndex.
     * 
     * @param list
     *            The list to get the sub-list from
     * @param startIndex
     *            The index to start at for extracting the sub-list
     * @param size
     *            The desired size of the sublist
     * @return The sublist
     */
    public static <T extends Object> List<T> subList(List<T> list,
            int startIndex, int size) {
        int endIndex = startIndex + size;
        if (startIndex + size > list.size()) {
            endIndex = list.size();
        }
        return list.subList(startIndex, endIndex);
    }

    /**
     * Removes null values from the given collection
     * 
     * @param collection
     *            The collection to remove nulls from
     * @return The updated collection
     */
    public static <T extends Object> Collection<T> removeNulls(
            Collection<T> collection) {
        return removeAllInstancesOfObject(collection, null);
    }

    /**
     * Removes all values equal to the given object from the collection
     * 
     * @param collection
     *            The collection to remove the values from
     * @param objToRemove
     *            The object to remove from the collection
     * @return The updated collection
     */
    public static <T extends Object> Collection<T> removeAllInstancesOfObject(
            Collection<T> collection, Object objToRemove) {
        while (collection.remove(objToRemove)) {

        }
        return collection;
    }
}
