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
package com.raytheon.viz.ui.dialogs.localization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.viz.ui.widgets.IFilterInput;

/**
 * 
 * Displays a list of localization files in a tree view. This class was formerly
 * com.raytheon.uf.viz.d2d.ui.dialogs.procedures.ProcedureTree.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ???                                 Initial creation
 * 02 Jun 2015  4401       bkowal      Re-factored for reuse.
 * 10 Jun 2015  4401       bkowal      Update {@link #hasChildren()} to use
 *                                     {@link List#isEmpty()}.
 * 16 Jun 2015  4401       bkowal      Updated so that it could provide input to a filter.
 * 13 Jan 2016  5242       kbisanz     Replaced calls to deprecated LocalizationFile methods
 * 
 * </pre>
 * 
 * @author unknown
 * @version 1.0
 */

public class VizLocalizationFileTree implements IFilterInput {
    private LinkedList<VizLocalizationFileTree> children = null;

    private String text = null;

    private ILocalizationFile file = null;

    public VizLocalizationFileTree(String text, ILocalizationFile file) {
        this.setText(text);
        this.setFile(file);
    }

    public VizLocalizationFileTree addChild(String text, ILocalizationFile file) {
        VizLocalizationFileTree child = new VizLocalizationFileTree(text, file);
        this.addChild(child);
        return child;
    }

    public void addChild(VizLocalizationFileTree child) {
        if (children == null) {
            children = new LinkedList<VizLocalizationFileTree>();
        }
        children.add(child);
    }

    public boolean isFileNode() {
        return (this.file != null);
    }

    public VizLocalizationFileTree findChildByText(String text) {
        if (hasChildren()) {
            Iterator<VizLocalizationFileTree> iter = children.iterator();
            while (iter.hasNext()) {
                VizLocalizationFileTree child = iter.next();
                if (child.getText().equals(text)) {
                    return child;
                }
            }
            // if we didn't find it travers the children
            iter = children.iterator();
            while (iter.hasNext()) {
                VizLocalizationFileTree child = iter.next().findChildByText(
                        text);
                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }

    public List<VizLocalizationFileTree> getChildren() {
        if (this.hasChildren()) {
            return this.children;
        } else {
            return null;
        }
    }

    public boolean hasChildren() {
        return (this.children != null && this.children.isEmpty() == false);
    }

    /**
     * @param text
     *            the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param file
     *            the file to set
     */
    public void setFile(ILocalizationFile file) {
        this.file = file;
    }

    /**
     * @return the file
     */
    public ILocalizationFile getFile() {
        return file;
    }

    @Override
    public List<Object> getObjects() {
        if (this.hasChildren()) {
            return new ArrayList<Object>(this.children);
        }
        return Collections.emptyList();
    }
}
