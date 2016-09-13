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
package com.raytheon.viz.ui.simulatedtime;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.viz.core.mode.CAVEMode;

/**
 * Utility module that provides common methods for handling operations that
 * should not be permitted when SimulatedTime or DRT mode is enabled.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 21, 2015  #4858     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

public final class SimulatedTimeOperations {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(SimulatedTimeOperations.class);

    /**
     * We prohibit actions that transmit data to the external network (like text
     * product transmission) when CAVE is in OPERATIONAL or TEST mode and
     * SimulatedTime is enabled. For applications like WES, we've created this
     * system property so users can bypass these blocks.
     */
    private static final String TRANSMISSION_OVERRIDE_PROPERTY = "allow.transmit.in.simulated.time";

    private static final String PERSPECTIVE_WARNING_TITLE = "%s Perspective features disabled";

    private static final String PERSPECTIVE_MESSAGE = "The CAVE clock is not set to real time.\n\nThe following %s functions are disabled:\n\t%s\n\nPlease ensure \"Use current real time\" is enabled in the CAVE clock to proceed.";

    private static final String FEATURE_LIST_SEP = "\n\t";

    private static final String FEATURE_WARNING_TITLE = "%s feature disabled";

    private static final String FEATURE_MESSAGE = "The CAVE clock is not set to real time.\n\n%s is disabled.\n\nPlease ensure \"Use current real time\" is enabled in the CAVE clock to proceed.";

    private SimulatedTimeOperations() {
        throw new AssertionError();
    }

    /**
     * Checks the value of the System property
     * "allow.transmit.in.simulated.time" which allows certain features to be
     * enabled when CAVE is in SimulatedTime mode.
     * 
     * @return
     */
    public static boolean isTransmitAllowedinSimulatedTime() {
        return Boolean.getBoolean(TRANSMISSION_OVERRIDE_PROPERTY);
    }

    /**
     * Returns whether or not product transmission and other similar system
     * functions (like GFE ISC) is allowed with the current CAVE state. Reasons
     * transmission might be blocked is because CAVE is in OPERATIONAL or TEST
     * mode with SimulatedTime enabled.
     * 
     * @return Whether or not the current CAVE state allows product
     *         transmission.
     */
    public static boolean isTransmitAllowed() {
        return (CAVEMode.getMode() == CAVEMode.PRACTICE)
                || (SimulatedTime.getSystemTime().isRealTime())
                || (isTransmitAllowedinSimulatedTime());
    }

    /**
     * Displays a JFace {@code MessageDialog} instance that should be used when
     * a perspective enters SimulatedTime mode and the user needs to be informed
     * about a number of features that cannot be used while the perspective is
     * in SimulatedTime mode.
     * 
     * @param shell
     *            the parent shell of the dialog, or {@code null} if none.
     * @param perspectiveName
     *            The name of the perspective affected.
     * @param disabledFeatures
     *            A list of features that will be inaccessible while CAVE
     *            remains in SimulatedTime mode.
     */
    public static void displayPerspectiveLevelWarning(Shell shell,
            String perspectiveName, List<String> disabledFeatures) {
        String message = getPerspectiveLevelWarning(perspectiveName,
                disabledFeatures);
        String title = String
                .format(PERSPECTIVE_WARNING_TITLE, perspectiveName);
        statusHandler.debug("User enetered SimulatedTime mode.");
        MessageDialog.openWarning(shell, title, message);
    }

    /**
     * Displays a JFace {@code MessageDialog} instance that should be used when
     * the user attempts to use a CAVE feature/function that is disabled in
     * SimulatedTime mode.
     * 
     * @param shell
     *            the parent shell of the dialog, or {@code null} if none.
     * @param disabledFeature
     *            The restricted feature or function the user attempted to
     *            access.
     */
    public static void displayFeatureLevelWarning(Shell shell,
            String disabledFeature) {
        String message = getFeatureLevelWarning(disabledFeature);
        String title = String.format(FEATURE_WARNING_TITLE, disabledFeature);
        MessageDialog.openWarning(shell, title, message);
    }

    /**
     * Constructs a new {@code SimulatedTimeProhibitedOpException} instance for
     * the given prohibited operation.
     * 
     * @param prohibitedOperation
     *            The operation the system attempted that is prohibited when in
     *            SimulatedTime mode.
     * @return The exception with the standard boiler plate error message about
     *         the operation being disabled in SimulatedTime mode.
     */
    public static SimulatedTimeProhibitedOpException constructProhibitedOpException(
            String prohibitedOperation) {
        return new SimulatedTimeProhibitedOpException(
                getFeatureLevelWarning(prohibitedOperation));
    }

    private static String getPerspectiveLevelWarning(String perspectiveName,
            List<String> disabledFeatures) {
        return String.format(PERSPECTIVE_MESSAGE, perspectiveName,
                StringUtils.join(disabledFeatures, FEATURE_LIST_SEP));
    }

    private static String getFeatureLevelWarning(String disabledFeature) {
        return String.format(FEATURE_MESSAGE, disabledFeature);
    }
}
