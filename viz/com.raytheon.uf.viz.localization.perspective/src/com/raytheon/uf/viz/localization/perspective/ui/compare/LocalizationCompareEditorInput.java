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
package com.raytheon.uf.viz.localization.perspective.ui.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.Saveable;

import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.localization.perspective.LocalizationFileModificationValidator;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.view.actions.ResolveFileVersionConflictAction;

/**
 * Comparing editor input for localization files
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 24, 2011           mschenke  Initial creation
 * Jan 22, 2015  4108     randerso  Allow editing in the compare editor
 * Aug 18, 2015  3806     njensen   Use SaveableOutputStream to save
 * May 23, 2016  4907     mapeters  Added save validation, refresh input on
 *                                  save, fix saving with read-only files,
 *                                  abstracted out most of saveable class
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class LocalizationCompareEditorInput extends CompareEditorInput
        implements ISaveablesSource {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationCompareEditorInput.class);

    private LocalizationEditorInput left;

    private LocalizationEditorInput right;

    private ResourceNode leftNode;

    private ResourceNode rightNode;

    private Saveable[] saveables;

    public LocalizationCompareEditorInput(LocalizationEditorInput left,
            LocalizationEditorInput right) {
        super(new CompareConfiguration());

        boolean leftReadOnly = left.getFile().isReadOnly();
        boolean rightReadOnly = right.getFile().isReadOnly();

        CompareConfiguration config = getCompareConfiguration();
        config.setLeftEditable(!leftReadOnly);
        config.setRightEditable(!rightReadOnly);
        config.setLeftLabel(left.getName());
        config.setRightLabel(right.getName());

        this.left = left;
        this.right = right;

        this.leftNode = new ResourceNode(left.getFile());
        this.rightNode = new ResourceNode(right.getFile());

        List<Saveable> saveablesList = new ArrayList<>();
        if (!leftReadOnly) {
            saveablesList.add(new LocalizationCompareSaveable(this, true));
        }
        if (!rightReadOnly) {
            saveablesList.add(new LocalizationCompareSaveable(this, false));
        }
        this.saveables = saveablesList.toArray(new Saveable[0]);
    }

    @Override
    protected Object prepareInput(IProgressMonitor pm)
            throws InvocationTargetException, InterruptedException {
        return new Differencer().findDifferences(false, pm, null, null,
                leftNode, rightNode);
    }

    @Override
    public Saveable[] getSaveables() {
        return this.saveables;
    }

    @Override
    public Saveable[] getActiveSaveables() {
        return getSaveables();
    }

    private static class LocalizationCompareSaveable extends
            AbstractLocalizationSaveable {

        private LocalizationCompareEditorInput parent;

        private boolean left;

        public LocalizationCompareSaveable(
                LocalizationCompareEditorInput parent, boolean left) {
            super(left ? parent.left : parent.right, left ? parent.leftNode
                    : parent.rightNode);
            this.parent = parent;
            this.left = left;
        }

        @Override
        public void doSave(IProgressMonitor monitor) throws CoreException {
            // flush changes from the viewer into the node
            if (left) {
                parent.flushLeftViewers(monitor);
            } else {
                parent.flushRightViewers(monitor);
            }

            try {
                validateAndPerformSave(monitor);
            } catch (LocalizationFileVersionConflictException e) {
                IDocument doc = CompareUI.getDocument(node);
                new ResolveFileVersionConflictAction(input, doc, null).run();
            }
        }

        @Override
        protected boolean validateSave(IProgressMonitor monitor) {
            IStatus status = LocalizationFileModificationValidator
                    .validateSave(input, CompareUI.getDocument(node));
            return status == Status.OK_STATUS;
        }

        @Override
        public boolean isDirty() {
            return (left ? parent.isLeftSaveNeeded() : parent
                    .isRightSaveNeeded());
        }
    }
}
