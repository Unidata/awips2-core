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
package com.raytheon.viz.ui.dialogs;

/**
 * Dialog interface used when a dialog has callbacks that should be called when
 * it is closed.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 22, 2017 4818       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */

public interface ICloseCallbackDialog {

    /**
     * Add a callback to the dialog. The callback will be called when the dialog
     * is disposed.
     *
     * @param callback
     *            Callback to be called when the dialog is disposed.
     */
    public void addCloseCallback(ICloseCallback callback);
}
