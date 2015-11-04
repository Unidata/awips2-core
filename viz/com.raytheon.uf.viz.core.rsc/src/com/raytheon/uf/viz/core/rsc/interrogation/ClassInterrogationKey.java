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
package com.raytheon.uf.viz.core.rsc.interrogation;

import java.util.Collection;

/**
 * 
 * An {@link InterrogationKey} that allows interrogation whenever the
 * {@link Class} of the value being requested is known. This type of key is most
 * useful when an {@link Interrogatable} can only be expected to provide one
 * value at a given coordinate/time.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 15, 2014  2820     bsteffen  Initial creation
 * Oct 27, 2015  5018     bsteffen  Do not use super in hashCode.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 * @param <T>
 *            the type of the Object that will be returned by an
 *            {@link Interrogatable} when using this key.
 * @see Interrogatable
 * @see InterrogationKey
 */
public class ClassInterrogationKey<T> extends InterrogationKey<T> {

    private final Class<T> keyClass;

    public ClassInterrogationKey(Class<T> keyClass) {
        this.keyClass = keyClass;
    }

    public Class<T> getKeyClass() {
        return keyClass;
    }

    /**
     * Safely cast this to a ClassInterrogationKey<S>. This method verifies the
     * compatibility of clazz and keyClass before casting.
     * 
     * 
     * @param clazz
     *            the type of keyClass that this can represent.
     * @return this cast to a ClassInterrogationKey<S>
     * @throws ClassCastException
     *             if clazz is not the same as keyClass
     */
    public <S> ClassInterrogationKey<S> asType(Class<S> clazz)
            throws ClassCastException {
        if (clazz.equals(keyClass)) {
            @SuppressWarnings("unchecked")
            ClassInterrogationKey<S> result = (ClassInterrogationKey<S>) this;
            return result;
        } else {
            throw new ClassCastException(clazz.toString()
                    + " is not the same as " + keyClass.toString());
        }
    }

    /**
     * Safely cast this to a ClassInterrogationKey<? extends S>. This method
     * verifies the compatibility of clazz and keyClass before casting.
     * 
     * 
     * @param clazz
     *            the type of keyClass that this can represent.
     * @return this cast to a ClassInterrogationKey<S>
     * @throws ClassCastException
     *             if clazz is not the same as keyClass
     */
    public <S> ClassInterrogationKey<? extends S> asSubType(Class<S> clazz)
            throws ClassCastException {
        if (clazz.isAssignableFrom(keyClass)) {
            @SuppressWarnings("unchecked")
            ClassInterrogationKey<? extends S> result = (ClassInterrogationKey<? extends S>) this;
            return result;
        } else {
            throw new ClassCastException(clazz.toString()
                    + " is not the same as " + keyClass.toString());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((keyClass == null) ? 0 : keyClass.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClassInterrogationKey<?> other = (ClassInterrogationKey<?>) obj;
        if (keyClass == null) {
            if (other.keyClass != null)
                return false;
        } else if (!keyClass.equals(other.keyClass))
            return false;
        return true;
    }

    /**
     * Find a key in the provided set of keys that is a
     * {@link ClassInterrogationKey} with a generic type that is a subclass of
     * the provided clazz.
     * 
     * This is useful if an {@link Interrogatable} might be providing a subclass
     * of the Class that a caller is looking for. For example the following
     * snippet will get a Number out of an Interrogatable regardless of whether
     * the Interrogatable is able to provide a ClassInterrogationKey<Double>, a
     * ClassInterrogationKey<Float>, or even a ClassInterrogationKey<Number>
     * 
     * <pre>
     * ClassInterrogationKey&lt;? extends Number&gt; e = ClassInterrogationKey
     *         .findSubClassKey(Number.class, interrogatable.getInterrogationKeys());
     * if (e != null) {
     *     Number theNumber = interrogatable.interrogate(date, coordinate, e).get(e);
     * }
     * </pre>
     * 
     * @param clazz
     *            the base class of the key type desired
     * @param keys
     *            the set of keys to search for a compatible key
     * @return a ClassInterrogationKey with the clazz type if it exists, or a
     *         ClassInterrogationKey of a subtype of clazz if any are found(if
     *         mroe than one exists an arbitrary key is chosen), or null if no
     *         compatible keys are in the set.
     */
    public static <T> ClassInterrogationKey<? extends T> findSubClassKey(
            Class<T> clazz, Collection<InterrogationKey<?>> keys) {
        ClassInterrogationKey<? extends T> result = null;
        for (InterrogationKey<?> key : keys) {
            if (key.getClass().equals(ClassInterrogationKey.class)) {
                ClassInterrogationKey<?> classKey = (ClassInterrogationKey<?>) key;
                if (clazz.isAssignableFrom(classKey.getKeyClass())) {
                    result = classKey.asSubType(clazz);
                    if (clazz.equals(classKey.getKeyClass())) {
                        break;
                    }
                }
            }
        }
        return result;
    }

}
