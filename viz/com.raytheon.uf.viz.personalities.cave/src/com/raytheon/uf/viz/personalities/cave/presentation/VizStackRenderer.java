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
package com.raytheon.uf.viz.personalities.cave.presentation;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import com.raytheon.uf.viz.personalities.cave.menu.VizEditorSystemMenu;

/**
 * Custom version of {@link StackRenderer} that allows the
 * {@link VizEditorSystemMenu} to add menu items whenever the right click menu
 * is opened for a tab with a {@link CompatibilityEditor}.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Dec 23, 2015  5189     bsteffen    Initial Creation.
 * 
 * </pre>
 * 
 * @author bsteffen
 */
@SuppressWarnings("restriction")
public class VizStackRenderer extends StackRenderer {

    private VizEditorSystemMenu editorMenu = null;

    @Override
    protected void populateTabMenu(Menu menu, MPart part) {
        super.populateTabMenu(menu, part);
        if (part.getObject() instanceof CompatibilityEditor) {
            if (editorMenu == null) {
                editorMenu = new VizEditorSystemMenu();
            }
            CompatibilityEditor editor = (CompatibilityEditor) part.getObject();
            editorMenu.show(menu, editor.getEditor());
        }
    }

}
