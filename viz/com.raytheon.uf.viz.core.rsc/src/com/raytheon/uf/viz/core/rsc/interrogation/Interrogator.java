package com.raytheon.uf.viz.core.rsc.interrogation;

import javax.measure.Quantity;

import org.locationtech.jts.geom.Geometry;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;

/**
 *
 * Provide static helper methods for assisting in interrogation and also
 * contains several common keys that can be referenced for guaranteed
 * compatibility between callers and Interrogatables.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * May 19, 2014  2820     bsteffen    Initial creation
 * Sep 21, 2016  3239     nabowle     Add getTypedMeasureClass()
 * Apr 15, 2019  7596     lsingh      Updated units framework to JSR-363.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class Interrogator {
    /**
     * Key to be used when a {@link Interrogatable} is able to provide
     * information about a geometry for a specific coordinate and time. One
     * example would be information on maps.
     */
    public static final InterrogationKey<Geometry> GEOMETRY = new ClassInterrogationKey<>(
            Geometry.class);

    /**
     * Key to be used when a {@link Interrogatable} is able to provide a single
     * distinct value at a specific coordinate and time. An
     * {@link Interrogatable} representing multiple values should not use this
     * key but should somehow provide a unique key for each value.
     */
    public static final InterrogationKey<Quantity<?>> VALUE = new ClassInterrogationKey<>(
            getTypedQuantityClass());

    /**
     * Retrieve a single value for a specific {@link InterrogationKey}. If
     * values are needed for several different keys then
     * {@link Interrogatable#interrogate(ReferencedCoordinate, DataTime, InterrogationKey...)}
     * should be called instead of calling this method multiple times because
     * some Interrogatable implementations may be able to optimize multiple
     * keys.
     *
     * @param coordinate
     *            the coordinate of interest
     * @param time
     *            the time of interest. If an Interrogatable or a specific key
     *            is time agnostic then the time should be ignored and a null
     *            time is acceptable. If a null time is passed to a
     *            Interrogatable that is not time agnostic then the result
     *            should be null.
     * @param key
     *            the key for the desired value.
     * @return the desired value or null if there is no data available for the
     *         specified parameters.
     * @see #interrogate(ReferencedCoordinate, DataTime, InterrogationKey...)
     * @see InterrogationKey
     */
    public static <T> T interrogateSingle(Interrogatable interrogatable,
            ReferencedCoordinate coordinate, DataTime time,
            InterrogationKey<T> key) {
        return interrogatable.interrogate(coordinate, time, key).get(key);
    }

    /**
     * Determine if an interrogatable contains the specified key.
     *
     * @param interrogatable
     *            an interrogatable
     * @param key
     *            in interrogation key.
     * @return true if the interrogatable supports the key.
     */
    public static boolean hasKey(Interrogatable interrogatable,
            InterrogationKey<?> key) {
        return interrogatable.getInterrogationKeys().contains(key);
    }

    /**
     * Get the typed Quantity class.
     */
    @SuppressWarnings("unchecked")
    public static <Q extends Quantity<?>> Class<Q> getTypedQuantityClass() {
        return (Class<Q>) Quantity.class;
    }
}
