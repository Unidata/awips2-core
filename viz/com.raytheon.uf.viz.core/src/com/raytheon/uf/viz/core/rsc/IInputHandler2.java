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

import org.eclipse.swt.widgets.Event;

/**
 * Extended Input Handler to allow access to the raw event so things like the
 * stateMask can be examined.
 * <p>
 * When implementing this interface it is intended that the implementor not
 * attempt to implement the methods inherited from the {@link IInputHandler}
 * interface. It is not needed, as there is a parallel method here for every
 * method in that interface.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 23, 2016  5674     randerso  Initial Creation.
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */
public interface IInputHandler2 extends IInputHandler {

    /**
     * Handle a mouse down event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleMouseDown(Event event);

    /**
     * Handle a mouse down move event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleMouseDownMove(Event event);

    /**
     * Handle a mouse up event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleMouseUp(Event event);

    /**
     * Handle a mouse hover event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleMouseHover(Event event);

    /**
     * Handle a mouse move event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleMouseMove(Event event);

    /**
     * Handle a double click event
     * 
     * @param event
     *            the event
     * @return true if other handlers should be pre-empted
     */
    boolean handleDoubleClick(Event event);

    /**
     * Handle a mouse wheel event
     * 
     * @param event
     *            the event
     * @return true if the other handlers should be pre-empted
     */
    boolean handleMouseWheel(Event event);

    /**
     * Handle the mouse exit event
     * 
     * @param event
     *            the event
     * @return true if the other handlers should be pre-empted
     */
    @Override
    boolean handleMouseExit(Event event);

    /**
     * Handle the mouse enter event
     * 
     * @param event
     *            the event
     * @return true if the other handlers should be pre-empted
     */
    @Override
    boolean handleMouseEnter(Event event);

    /**
     * Handle a key down event
     * 
     * @param event
     *            the event
     * @return true if the other handlers should be pre-empted
     */
    boolean handleKeyDown(Event event);

    /**
     * Handle a key up event
     * 
     * @param event
     *            the event
     * @return true if the other handlers should be pre-empted
     */
    boolean handleKeyUp(Event event);

}
