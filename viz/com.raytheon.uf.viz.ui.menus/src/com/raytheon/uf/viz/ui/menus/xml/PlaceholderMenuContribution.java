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
package com.raytheon.uf.viz.ui.menus.xml;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;

import com.raytheon.uf.common.menus.xml.CommonAbstractMenuContribution;
import com.raytheon.uf.common.menus.xml.CommonPlaceholderMenuContribution;
import com.raytheon.uf.common.menus.xml.VariableSubstitution;
import com.raytheon.uf.common.util.VariableSubstitutor;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.actions.NotImplementedAction;

/**
 * Describes a placeholder (not implemented) menu item
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 2, 2009            chammack     Initial creation
 * Nov 08, 2016 5976       bsteffen    Use VariableSubstitutor directly
 * 
 * </pre>
 * 
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
public class PlaceholderMenuContribution extends
        AbstractMenuContributionItem<CommonPlaceholderMenuContribution> {

    @Override
    public IContributionItem[] getContributionItems(
            CommonAbstractMenuContribution items, VariableSubstitution[] subs,
            Set<String> removals) throws VizException {
        CommonPlaceholderMenuContribution item = (CommonPlaceholderMenuContribution) items;
        if (removals.contains(item.id))
            return new IContributionItem[0];

        String text = item.menuText;
        if (subs != null && subs.length > 0) {
            Map<String, String> s = VariableSubstitution.toMap(subs);
            try {
                text = VariableSubstitutor.processVariables(text, s);
            } catch (ParseException e) {
                throw new VizException("Error processing variable substitution",
                        e);
            }
        }

        final String fText = text;

        ActionContributionItem aci = new ActionContributionItem(new Action() {

            @Override
            public void run() {
                NotImplementedAction.displayNotImplemented();
            }

            @Override
            public String getText() {
                return fText;
            }

        });
        aci.setVisible(true);
        return new IContributionItem[] { aci };
    }
}
