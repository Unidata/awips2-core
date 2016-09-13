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
package com.raytheon.viz.ui.input;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import com.raytheon.uf.viz.core.rsc.IInputHandler2;

/**
 * Abstract class for IInputHandler that provides default noop implementations
 * for all methods
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 08, 2009           mschenke  Initial creation
 * Jun 23, 2016  5674     randerso  Extend IInputHandler to pass raw event to
 *                                  handler
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public abstract class InputAdapter implements IInputHandler2 {

    @Override
    public boolean handleMouseDown(Event event) {
        return handleMouseDown(event.x, event.y, event.button);
    }

    @Override
    public boolean handleMouseDownMove(Event event) {
        int button = 0;
        if ((event.stateMask & SWT.BUTTON1) != 0) {
            button = 1;
        } else if ((event.stateMask & SWT.BUTTON2) != 0) {
            button = 2;
        } else if ((event.stateMask & SWT.BUTTON3) != 0) {
            button = 3;
        } else if ((event.stateMask & SWT.BUTTON4) != 0) {
            button = 4;
        } else if ((event.stateMask & SWT.BUTTON5) != 0) {
            button = 5;
        }
        return handleMouseDownMove(event.x, event.y, button);
    }

    @Override
    public boolean handleMouseUp(Event event) {
        return handleMouseUp(event.x, event.y, event.button);
    }

    @Override
    public boolean handleMouseHover(Event event) {
        return handleMouseHover(event.x, event.y);
    }

    @Override
    public boolean handleMouseMove(Event event) {
        return handleMouseMove(event.x, event.y);
    }

    @Override
    public boolean handleDoubleClick(Event event) {
        return handleDoubleClick(event.x, event.y, event.button);
    }

    @Override
    public boolean handleMouseWheel(Event event) {
        return handleMouseWheel(event, event.x, event.y);
    }

    @Override
    public boolean handleKeyDown(Event event) {
        return handleKeyDown(event.keyCode);
    }

    @Override
    public boolean handleKeyUp(Event event) {
        return handleKeyUp(event.keyCode);
    }

    @Override
    public boolean handleDoubleClick(int x, int y, int button) {
        return false;
    }

    @Override
    public boolean handleKeyDown(int keyCode) {
        return false;
    }

    @Override
    public boolean handleKeyUp(int keyCode) {
        return false;
    }

    @Override
    public boolean handleMouseDown(int x, int y, int mouseButton) {
        return false;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return false;
    }

    @Override
    public boolean handleMouseHover(int x, int y) {
        return false;
    }

    @Override
    public boolean handleMouseMove(int x, int y) {
        return false;
    }

    @Override
    public boolean handleMouseUp(int x, int y, int mouseButton) {
        return false;
    }

    @Override
    public boolean handleMouseWheel(Event event, int x, int y) {
        return false;
    }

    @Override
    public boolean handleMouseExit(Event event) {
        return false;
    }

    @Override
    public boolean handleMouseEnter(Event event) {
        return false;
    }

}
