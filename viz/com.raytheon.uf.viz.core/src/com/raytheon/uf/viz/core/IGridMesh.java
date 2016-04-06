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
package com.raytheon.uf.viz.core;

import org.opengis.coverage.grid.GridGeometry;

import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension;

/**
 * 
 * Optional interface which an {@link IMesh} can implement if it is able to
 * provide detailed geospatial information about the {@link GridGeometry}s that
 * it contains. Implementing this interface will allow generic
 * {@link GraphicsExtension}s to extract details from the mesh that may allow
 * them to do customize rendering beyond the capabilities of the target which
 * created the mesh.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------
 * Apr 05, 2016  5400     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public interface IGridMesh extends IMesh {

    public GridGeometry getSourceGeometry();

    public GridGeometry getTargetGeometry();

}
