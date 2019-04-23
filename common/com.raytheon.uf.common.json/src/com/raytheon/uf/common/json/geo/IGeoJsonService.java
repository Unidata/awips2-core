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
package com.raytheon.uf.common.json.geo;

import java.io.InputStream;
import java.io.OutputStream;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.raytheon.uf.common.json.JsonException;
import org.locationtech.jts.geom.Geometry;

/**
 * Interface for an object the serializes/de-serializes geometries, features,
 * and feature collections in GeoJSON format.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            bclement     Initial creation
 * Apr 27, 2015  #4354     dgilling     Renamed to IGeoJsonService.
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public interface IGeoJsonService {

    /**
     * @param geom
     * @param out
     * @throws JsonException
     */
    public void serialize(Geometry geom, OutputStream out) throws JsonException;

    /**
     * @param geom
     * @return
     * @throws JsonException
     */
    public String serialize(Geometry geom) throws JsonException;

    /**
     * @param in
     * @return
     * @throws JsonException
     */
    public Geometry deserializeGeom(InputStream in) throws JsonException;

    /**
     * @param feature
     * @param out
     * @throws JsonException
     */
    public void serialize(SimpleFeature feature, OutputStream out)
            throws JsonException;

    /**
     * @param feature
     * @return
     * @throws JsonException
     */
    public String serialize(SimpleFeature feature) throws JsonException;

    /**
     * @param in
     * @return
     * @throws JsonException
     */
    public SimpleFeature deserializeFeature(InputStream in)
            throws JsonException;

    /**
     * @param coll
     * @param out
     * @throws JsonException
     */
    public void serialize(
            FeatureCollection<SimpleFeatureType, SimpleFeature> coll,
            OutputStream out) throws JsonException;

    /**
     * @param in
     * @return
     * @throws JsonException
     */
    public FeatureCollection<SimpleFeatureType, SimpleFeature> deserializeFeatureCollection(
            InputStream in) throws JsonException;

    /**
     * @param in
     * @param type
     * @return
     * @throws JsonException
     */
    public SimpleFeature deserializeFeature(InputStream in,
            SimpleFeatureType type) throws JsonException;

    /**
     * @param in
     * @param type
     * @return
     * @throws JsonException
     */
    public FeatureCollection<SimpleFeatureType, SimpleFeature> deserializeFeatureCollection(
            InputStream in, SimpleFeatureType type) throws JsonException;

}
