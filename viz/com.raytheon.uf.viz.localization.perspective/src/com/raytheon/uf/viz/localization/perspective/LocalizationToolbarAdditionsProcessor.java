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
package com.raytheon.uf.viz.localization.perspective;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarSeparator;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

/**
 * This is a processor to help ensure that the localization toolbar gets
 * positioned correctly in the localization perspective.
 * 
 * The localization perspective contributes a toolbar to provide simple editor
 * actions like save, print, copy and undo. This toolbar is meant to replace an
 * eclipse toolbar with similar functionality but provide custom behavior for
 * localization file saving. The localization toolbar should be placed further
 * to the left than any other toolbar, this matches the behavior of the eclipse
 * toolbar.
 * 
 * The problem is that eclipse does not provide a way to specifically order
 * toolbars. The only toolbar item that can be referenced in a position is the
 * "additions" separator, and normally all toolbars are placed after additions
 * but the actual order of all the contributions that are after additions is not
 * customizable. To force the localization toolbar to be the farthest to the
 * left it is the only toolbar that is positioned before additions.
 * 
 * Positioning the localization toolbar before additions works as long as the
 * additions separator is present in the model, however when the workspace has
 * not been created (typically only the first time the application is started)
 * then the additions separator does not exist. The eclipse framework has
 * specialized code to position toolbars that are relative to additions when
 * additions does not yet exist but it is not honoring the before/after
 * positioning so the localization toolbar is positioned incorrectly(In Eclipse
 * 4.5.1 this handling happens in the final return statement of
 * ContributionsAnalyzer.getIndex())
 * 
 * This processor will ensure that the additions separator exists so that the
 * specialized handling of additions is not needed and the before position is
 * handled correctly.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Mar 18, 2016  5330     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class LocalizationToolbarAdditionsProcessor {

    @Execute
    void checkAdditionsSeparator(MApplication app, EModelService modelService) {
        List<MTrimBar> trimBars = modelService.findElements(app,
                "org.eclipse.ui.main.toolbar", MTrimBar.class, null);
        if (trimBars.size() != 1) {
            return;
        }
        MTrimBar trimBar = trimBars.get(0);
        for (MTrimElement child : trimBar.getChildren()) {
            if (child.getElementId().equals("additions")) {
                return;
            }
        }

        MToolBar toolBar = modelService.createModelElement(MToolBar.class);
        toolBar.setElementId("additions");
        toolBar.setToBeRendered(false);
        MToolBarSeparator sep = modelService
                .createModelElement(MToolBarSeparator.class);
        sep.setElementId("additions");
        sep.setToBeRendered(false);

        toolBar.getChildren().add(sep);
        trimBar.getChildren().add(toolBar);
    }

}
