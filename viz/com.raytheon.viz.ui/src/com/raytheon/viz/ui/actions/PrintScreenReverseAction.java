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

package com.raytheon.viz.ui.actions;

import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.printing.PrintJob;
import com.raytheon.uf.viz.core.printing.PrintJob.PrintingException;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * Print the current map
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 26, 2006           chammack  Initial Creation.
 * Aug 08, 2008  703      randerso  fixed bug, changed to scale to fit paper and
 *                                  rotate if necessary
 * Mar 23, 2017  6117     bsteffen  Workaround crash when printing images.
 * Apr 13, 2020  8120     randerso  Use PrintJob
 *
 * </pre>
 *
 * @author chammack
 */
public class PrintScreenReverseAction extends AbstractHandler {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PrintScreenReverseAction.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        AbstractEditor editor = null;
        IEditorPart part = EditorUtil.getActiveEditor();
        if (part instanceof AbstractEditor) {
            editor = (AbstractEditor) part;
        }
        if (editor == null) {
            return null;
        }
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        // display the printer dialog to get print options
        PrintDialog pd = new PrintDialog(shell);
        PrinterData printerData = pd.open();
        if (printerData == null) {
            return null;
        }

        BufferedImage bi = editor.screenshot();
        applyFilter(bi);

        Display display = editor.getActiveDisplayPane().getDisplay();
        try (PrintJob printJob = new PrintJob(printerData)) {
            printJob.printImage(display, bi, false, true);
        } catch (PrintingException e) {
            statusHandler.error(e.getLocalizedMessage(), e);
        }

        return null;

    }

    /*
     * Create reverse lookup table for image
     */
    private ByteLookupTable reverseLUT() {
        byte reverse[] = new byte[256];
        for (int i = 0; i < 256; i++) {
            reverse[i] = (byte) (255 - i);
        }
        return new ByteLookupTable(0, reverse);
    }

    /*
     * Apply lookup table filter to image
     */
    private void applyFilter(BufferedImage bi) {
        LookupOp lop = new LookupOp(reverseLUT(), null);
        lop.filter(bi, bi);
    }

    @Override
    public void dispose() {

    }

}
