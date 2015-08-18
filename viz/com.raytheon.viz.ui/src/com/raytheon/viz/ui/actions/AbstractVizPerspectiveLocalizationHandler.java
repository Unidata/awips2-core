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

import java.io.File;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Abstract Viz {@link AbstractHandler} that provides common methods for
 * interacting with the cave_static perspective localization.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 10, 2015 4401       bkowal      Initial creation
 * Jun 30, 2015 4401       bkowal      Perspectives are now stored in common static.
 * Aug 18, 2015 3806       njensen     Use SaveableOutputStream to save
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public abstract class AbstractVizPerspectiveLocalizationHandler extends
        AbstractHandler {

    protected final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    public static final String PERSPECTIVES_DIR = "/perspectives";

    /**
     * Saves a cave_static perspective localization file.
     * 
     * @param fileName
     *            the name of the localization file to save.
     * @param fileContents
     *            the contents of the localization file to save.
     */
    protected void savePerspectiveLocalization(final String fileName,
            final byte[] fileContents, final ExecutionEvent event) {
        this.savePerspectiveLocalization(fileName, fileContents, event, false);
    }

    /**
     * Saves a cave_static perspective localization file.
     * 
     * @param fileName
     *            the name of the localization file to save.
     * @param fileContents
     *            the contents of the localization file to save.
     * @param verifyOverwrite
     *            boolean indicating whether or not the user should be prompted
     *            when saving a localization file will overwrite an existing
     *            file. When true, the user will be prompted.
     */
    protected void savePerspectiveLocalization(final String fileName,
            final byte[] fileContents, final ExecutionEvent event,
            boolean verifyOverwrite) {
        IPathManager pm = PathManagerFactory.getPathManager();

        LocalizationContext context = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.USER);

        ILocalizationFile localizationFile = pm.getLocalizationFile(context,
                PERSPECTIVES_DIR + File.separator + fileName);
        if (verifyOverwrite && localizationFile.exists()) {
            boolean result = MessageDialog.openQuestion(
                    HandlerUtil.getActiveShell(event), "Confirm Overwrite",
                    "The file " + fileName + " already exists.  Overwrite?");
            if (result == false) {
                return;
            }
        }

        try (SaveableOutputStream sos = localizationFile.openOutputStream()) {
            sos.write(fileContents);
            sos.save();
        } catch (Exception e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Failed to write localization file: " + fileName + ".", e);
        }
    }
}