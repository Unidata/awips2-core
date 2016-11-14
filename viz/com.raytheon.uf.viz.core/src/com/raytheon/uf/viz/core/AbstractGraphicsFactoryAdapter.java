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

import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Graphics Factory adapter for constructing graphics types
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------
 * Jun 19, 2010  3190     mschenke  Initial creation
 * Nov 03, 2016  5976     bsteffen  Remove extent methods
 * 
 * </pre>
 * 
 * @author mschenke
 */
public abstract class AbstractGraphicsFactoryAdapter {

    public abstract IView constructView();

    public abstract IGraphicsTarget constructTarget(Canvas canvas, float width,
            float height) throws VizException;

    public abstract Canvas constrcutCanvas(Composite canvasComp)
            throws VizException;

}
