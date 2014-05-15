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

import java.util.Set;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;

/**
 * An object that can provide generic information on demand for a specific point
 * in space and time. An {@link InterrogationKey} is used for retrieving
 * information to interrogate any type of data while ensuring type safety.
 * {@link #interrogate(ReferencedCoordinate, DataTime, InterrogationKey...)
 * guarantees that the type of an interrogation value will always match the
 * generic type of the key so it is possible for interrogate to return arbitrary
 * Objects while still ensuring type safety.
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
 * @see InterrogationKey
 */
public interface Interrogatable {

    /**
     * Get all the keys supported for this Interrogatable. This should include
     * all keys possible for this object even if some keys may not be available
     * for all times/coordinates.
     * 
     * @return A set of keys that are available for this Interrogatable.
     */
    public Set<InterrogationKey<?>> getInterrogationKeys();

    /**
     * Retrieve values for multiple {@link InterrogationKey}s.
     * 
     * @param coordinate
     *            the coordinate of interest
     * @param time
     *            the time of interest. If an Interrogatable or a specific key
     *            is time agnostic then the time should be ignored and a null
     *            time is acceptable. If a null time is passed to a
     *            Interrogatable that is not time agnostic then the result
     *            should be null.
     * @param keys
     *            the keys for all required values.
     * @return an {@link InterrogateMap} which contains values for any keys that
     *         are available for the provided parameters.
     * @see InterrogationKey
     * @see InterrogateMap
     */
    public InterrogateMap interrogate(
            ReferencedCoordinate coordinate, DataTime time,
            InterrogationKey<?>... keys);

}
