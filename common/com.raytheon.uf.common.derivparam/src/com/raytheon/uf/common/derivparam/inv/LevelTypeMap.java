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
package com.raytheon.uf.common.derivparam.inv;

import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.derivparam.library.LevelType;

/**
 * 
 * When resolving the fields for a derived parameter, all fields with the same
 * {@link LevelType} should be resolved to the same level. This class is used to
 * hold values for all of the possible levels that could be used for a specific
 * field. After all fields have been processed then the results can be
 * normalized to ensure all fields have data on the same level.
 * 
 * For example, if a derived parameter contains a method like this:
 * 
 * <pre>
 * {@code
 *     <Method levels="MB" name="Add">
 *         <Field level="upper" abbreviation="T"/>
 *         <Field abbreviation="T"/>
 *         <Field level="upper" abbreviation="P"/>
 *         <Field abbreviation="P"/>
 *     </Method>
 * }
 * </pre>
 * 
 * Then the upper T and the upper P should resolve to the same level. P is
 * always constant for MB levels so it is available for any level, even levels
 * where a model does not normally produce data. T on the other hand may only be
 * available at specific levels. For this case both upper levels will resolve to
 * a LevelTypeMap containing all the levels above the target level but P will
 * contain many more levels than T. When these maps are normalized the extra
 * levels will be removed from P so both P and T will resolve to the value on
 * the same level.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Nov 30, 2015  5072     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class LevelTypeMap {

    private final LevelType type;

    private NavigableMap<Level, Object> map;

    /**
     * @param type
     *            the LevelType for this map, only maps with the same type will
     *            be normalized.
     * @param comparator
     *            Comparator for defining level ordering. This should be an
     *            ascending comparator for {@link LevelType#Upper} and a
     *            descending comparator for {@link LevelType#Lower}.
     */
    public LevelTypeMap(LevelType type, Comparator<? super Level> comparator) {
        this.type = type;
        this.map = new TreeMap<>(comparator);
    }

    public void add(Level level, Object value) {
        map.put(level, value);
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * If this map and the other map are the same level type then any levels not
     * in the other map will be removed from this map. If the types are
     * different then no change is performed. Other is not changed.
     */
    public void normalize(LevelTypeMap other) {
        if (this.type == other.type) {
            map.keySet().retainAll(other.map.keySet());
        }
    }

    /**
     * Get the best value from this map. This should be used after normalize has
     * removed any levels that should not be used. This will return the first
     * item in the map according to the comparator used during construction.
     * 
     * @return
     */
    public Object resolve() {
        return map.firstEntry().getValue();
    }

    /**
     * Normalize the levels in this map to contain only the levels that are
     * contained in all the passed in maps that are the same type.
     */
    public void normalize(List<LevelTypeMap> others) {
        for (LevelTypeMap other : others) {
            if (other != this) {
                this.normalize(other);
            }
        }
    }

}
