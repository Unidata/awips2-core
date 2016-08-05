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
package com.raytheon.uf.viz.personalities.cave.workbench;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.internal.workbench.E4Workbench;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;

import com.raytheon.uf.viz.core.ProgramArguments;

/**
 * 
 * Places the window on the same display as the cursor and attempts to scale the
 * window based off the monitor size and resolution. This runs as a processor
 * instead of running in the {@link VizWorkbenchWindowAdvisor} because the
 * advisor runs after the window has been created which can result in the window
 * temporarily appearing in the wrong spot. This processor runs before window
 * creation and ensures the window is initially placed correctly.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------------
 * Aug 05, 2016  5764     bsteffen  Extracted logic from VizWorkbenchWindowAdvisor
 *
 * </pre>
 *
 * @author bsteffen
 */
public class WindowPlacementProcessor {

    @Execute
    void placeApplicationWindow(MApplication app, Display display,
            @Optional @Named(E4Workbench.NO_SAVED_MODEL_FOUND) Boolean firstTime) {

        if (firstTime == null) {
            firstTime = false;
        }

        Monitor[] monitors = display.getMonitors();

        ProgramArguments args = ProgramArguments.getInstance();
        Integer monitor = args.getInteger("-monitor");
        if (monitor == null) {
            monitor = 0;

            Point cursor = display.getCursorLocation();
            for (int i = 0; i < monitors.length; i++) {
                if (monitors[i].getBounds().contains(cursor)) {
                    monitor = i;
                    break;
                }
            }
        }

        if (monitor >= monitors.length) {
            monitor = monitors.length - 1;
        }
        Rectangle bounds = monitors[monitor].getBounds();

        /* Only expecting one child. */
        for (MWindow window : app.getChildren()) {

            Integer width = args.getInteger("-width");
            if (width == null) {
                if (firstTime) {
                    width = Math.min(bounds.width - 80,
                            (bounds.height * 5) / 4);
                } else {
                    // use saved width unless greater than monitor width
                    width = Math.min(bounds.width, window.getWidth());
                }
            }

            Integer height = args.getInteger("-height");
            if (height == null) {
                if (firstTime) {
                    height = bounds.height;
                } else {
                    // use saved height unless greater than monitor height
                    height = Math.min(bounds.height, window.getHeight());
                }
            }
            window.setWidth(width);
            window.setHeight(height);

            Integer x = args.getInteger("-x");
            Integer y = args.getInteger("-y");
            if ((x != null) && (y != null)) {
                window.setX(x + bounds.x);
                window.setY(y + bounds.y);
            } else if (firstTime) {
                window.setX((bounds.x + bounds.width) - width);
                window.setY(bounds.y);
            } else {
                // scale saved location to selected monitor size
                int prevX = window.getX();
                int prevY = window.getY();
                for (Monitor m : monitors) {
                    Rectangle b = m.getBounds();
                    if (b.contains(prevX, prevY)) {
                        x = (((prevX - b.x) * bounds.width) / b.width)
                                + bounds.x;
                        y = (((prevY - b.y) * bounds.height) / b.width)
                                + bounds.y;
                        window.setX(x);
                        window.setY(y);
                        break;
                    }
                }
            }

        }
    }
}
