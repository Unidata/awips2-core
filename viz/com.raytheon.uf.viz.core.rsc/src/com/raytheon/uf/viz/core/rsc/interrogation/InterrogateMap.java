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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A map that is used for returning multiple interrogation values from an
 * {@link Interrogatable} while preserving type safety. This class provides most
 * of the methods available in the {@link Map} interface however it guarantees
 * the type safety of the {@link InterrogationKey} generic type and the value so
 * it cannot implement the Map interface. This implementation is not thread safe
 * and must be synchronized if it is accessed from multiple threads.
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
 * @author mnash
 * @version 1.0
 * @see Interrogatable
 * @see InterrogationKey
 */
public final class InterrogateMap {

    private final Map<InterrogationKey<?>, Object> map = new HashMap<InterrogationKey<?>, Object>();

    public InterrogateMap() {

    }

    public InterrogateMap(InterrogateMap original) {
        map.putAll(original.map);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean containsKey(InterrogationKey<?> t) {
        return map.containsKey(t);
    }

    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public <T> T get(InterrogationKey<T> key) {
        @SuppressWarnings("unchecked")
        T result = (T) map.get(key);
        return result;
    }


    public <T> T put(InterrogationKey<T> key, T value) {
        @SuppressWarnings("unchecked")
        T result = (T) map.put(key, value);
        return result;
    }

    public <T> T remove(InterrogationKey<T> key) {
        @SuppressWarnings("unchecked")
        T result = (T) map.remove(key);
        return result;
    }

    public void putAll(InterrogateMap m) {
        map.putAll(m.map);
    }

    public void clear() {
        map.clear();
    }

    public Set<InterrogationKey<?>> keySet() {
        return map.keySet();
    }

    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InterrogateMap) {
            InterrogateMap mp = (InterrogateMap) obj;
            return map.equals(mp.map);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

}
