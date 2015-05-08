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
package com.raytheon.uf.common.dataaccess.impl;

import org.geotools.coverage.grid.GridGeometry2D;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.util.SubGridGeometryCalculator;

/**
 * Wrapper to collect the pdo, geometry, and subgrid.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 28, 2015 2866       nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 * @version 1.0
 */
public class CollectedGridGeometry {
    private PluginDataObject pdo;

    private GridGeometry2D gridGeometry;

    private SubGridGeometryCalculator subgrid;

    /**
     * Constructor.
     *
     * @param pdo
     * @param grid
     * @param sub
     */
    public CollectedGridGeometry(PluginDataObject pdo, GridGeometry2D grid,
            SubGridGeometryCalculator sub) {
        super();
        this.pdo = pdo;
        this.gridGeometry = grid;
        this.subgrid = sub;
    }

    /**
     * @return the pdo
     */
    public PluginDataObject getPdo() {
        return pdo;
    }

    /**
     * @return the gridGeometry
     */
    public GridGeometry2D getGridGeometry() {
        return gridGeometry;
    }

    /**
     * @return the subgrid
     */
    public SubGridGeometryCalculator getSubgrid() {
        return subgrid;
    }
}
