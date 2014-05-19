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
 * An {@link InterrogationKey} that allows interrogation whenever a
 * {@link Interrogatable} and the calling code can agree on a specific
 * {@link String} and {@link Class} to interrogate. This is most useful when the
 * keys are not well defined at compile time but a unique String can be found at
 * runtime. For example an Interrogatable that can provide information on
 * different database tables with different columns could provide one key per
 * column using the column name as the string component of the key, the
 * available keys could change at runtime depending on the layout the
 * Interrogatable is representing.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * May 15, 2014  2820     bsteffen    Initial creation
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
public class StringInterrogationKey<T> extends ClassInterrogationKey<T> {

    private final String id;

    public StringInterrogationKey(String id, Class<T> keyClass) {
        super(keyClass);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public <S> StringInterrogationKey<S> asType(Class<S> clazz) {
        return (StringInterrogationKey<S>) super.asType(clazz);
    }

    @Override
    public <S> StringInterrogationKey<? extends S> asSubType(Class<S> clazz) {
        return (StringInterrogationKey<? extends S>) super.asSubType(clazz);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        StringInterrogationKey<?> other = (StringInterrogationKey<?>) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /**
     * Provides the same functionality as
     * {@link ClassInterrogationKey#findSubClassKey(Class, Collection)} but also
     * enforces that the id of the {@link StringInterrogationKey} must be the
     * same.
     * 
     * @param clazz
     *            the abse clazz for which
     * @param id
     * @param keys
     * @return
     * @see ClassInterrogationKey#findSubClassKey(Class, Collection)
     */
    public static <T> StringInterrogationKey<? extends T> findSubClassKey(
            Class<T> clazz, String id, Collection<InterrogationKey<?>> keys) {
        StringInterrogationKey<? extends T> result = null;
        for (InterrogationKey<?> key : keys) {
            if (key.getClass().equals(StringInterrogationKey.class)) {
                StringInterrogationKey<?> stringKey = (StringInterrogationKey<?>) key;
                if (clazz.isAssignableFrom(stringKey.getKeyClass())
                        && id.equals(stringKey.getId())) {
                    result = stringKey.asSubType(clazz);
                    if (clazz.equals(stringKey.getKeyClass())) {
                        break;
                    }
                }
            }
        }
        return result;
    }

}
