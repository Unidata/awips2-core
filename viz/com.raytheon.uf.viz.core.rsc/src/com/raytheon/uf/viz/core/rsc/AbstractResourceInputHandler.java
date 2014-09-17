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
package com.raytheon.uf.viz.core.rsc;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.viz.ui.input.InputAdapter;

/**
 * Abstract class for {@link AbstractVizResource} {@link IInputHandler}s
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 4, 2014  3549      mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public abstract class AbstractResourceInputHandler<T extends AbstractVizResource<?, ?>>
        extends InputAdapter {

    protected static final int NORMAL_CURSOR = SWT.NONE;

    protected final T resource;

    protected final IDisplayPaneContainer resourceContainer;

    protected int cursorType = NORMAL_CURSOR;

    protected Shell previousShell;

    protected AbstractResourceInputHandler(T resource) {
        this(resource, InputPriority.RESOURCE);
    }

    protected AbstractResourceInputHandler(T resource, InputPriority priority) {
        this.resource = resource;
        this.resourceContainer = resource.getResourceContainer();
        if (resourceContainer != null) {
            resourceContainer.registerMouseHandler(this, priority);
        }
    }

    /**
     * Disposes the input handler and unregisters itself from the container
     */
    public void dispose() {
        if (resourceContainer != null) {
            resourceContainer.unregisterMouseHandler(this);
        }
    }

    /**
     * Changes the {@link Cursor} to the system cursor with the given Id
     * 
     * @param cursorId
     */
    protected void setCursor(int cursorId) {
        if (this.cursorType != cursorId) {
            this.cursorType = cursorId;
            Shell active = Display.getCurrent().getActiveShell();
            if (active == null) {
                active = previousShell;
            }
            if (active != null) {
                if (active != previousShell && previousShell != null
                        && previousShell.isDisposed() == false) {
                    previousShell.setCursor(null);
                }
                if (cursorId != SWT.NONE) {
                    active.setCursor(active.getDisplay().getSystemCursor(
                            cursorId));
                } else {
                    active.setCursor(null);
                }
                previousShell = active;
            }
        }
    }

}
