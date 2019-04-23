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
package com.raytheon.uf.viz.core.maps.jobs;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.maps.rsc.AbstractMapResource;
import org.locationtech.jts.geom.Geometry;

/**
 * Base map query job request
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2014  #3459     randerso     Initial creation
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public abstract class AbstractMapRequest<RESOURCE extends AbstractMapResource<?, ?>> {
    private static int requestCounter = 0;

    private int number;

    private RESOURCE resource;

    private Geometry boundingGeom;

    private IGraphicsTarget target;

    private long queuedTime;

    public AbstractMapRequest(IGraphicsTarget target, RESOURCE resource,
            Geometry boundingGeom) {
        this.number = requestCounter++;
        this.target = target;
        this.resource = resource;
        this.boundingGeom = boundingGeom;
    }

    /**
     * @return the requestCounter
     */
    public static int getRequestCounter() {
        return requestCounter;
    }

    /**
     * @return the number
     */
    public int getNumber() {
        return number;
    }

    /**
     * @return the resource
     */
    public RESOURCE getResource() {
        return resource;
    }

    /**
     * @return the boundingGeom
     */
    public Geometry getBoundingGeom() {
        return boundingGeom;
    }

    /**
     * @return the target
     */
    public IGraphicsTarget getTarget() {
        return target;
    }

    void setQueuedTime(long timeInMillis) {
        this.queuedTime = timeInMillis;
    }

    long getQueuedTime() {
        return this.queuedTime;
    }
}
