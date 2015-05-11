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
package com.raytheon.uf.viz.core.comm;

import java.security.KeyStore;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.comm.IHttpsHandler;
import com.raytheon.uf.viz.core.auth.UserController;

/**
 * Cave implementation of the IHttpsCredentialsHandler. Displays the Cave login
 * dialog to get the authorization credentials.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 04, 2013    1786    mpduff      Initial creation.
 * Jun 07, 2013    1981    mpduff      Save user's username in UserController.
 * Feb 10, 2014    2704    njensen     Added credentialsFailed()
 * Sep 03, 2014    3570    bclement    added host and port to getCredentials()
 * Nov 15, 2014    3757    dhladky     Added flag for certificate validation checks
 * May 10, 2015    4435    dhladky     Updated interface to allow for loading keyStores.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class CaveHttpsCredentialsHandler implements IHttpsHandler {

    public static final String CERT_HANDLE_FLAG = "https.certificate.check";
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getCredentials(String host, int port, String message) {
        // If message contains an "=" split and take the value of the pair
        if (message.contains("=")) {
            message = message.split("=")[1];
        }
        HttpsLoginDlg login = new HttpsLoginDlg(message);
        login.open();
        String[] credentials = login.getCredentials();
        // Save off the user's username in the UserController
        UserController.updateUserData(credentials[0]);
        return credentials;
    }

    @Override
    public void credentialsFailed() {
        MessageDialog.openError(new Shell(Display.getDefault()),
                "Login failed",
                "Invalid username and/or password.  Please try again.");
    }

    @Override
    public boolean isValidateCertificates() {
        return Boolean.getBoolean(CERT_HANDLE_FLAG);
    }

    @Override
    public KeyStore getTruststore() {
        /*
         * Intentionally not implemented. By returning null, Java will fall back
         * to the default which contains all the major certificate vendors.
         */
        return null;
    }

    @Override
    public KeyStore getKeystore() {
        /** 
         * Load the keyStore you wish to submit keys for certificates from.
         * If in the future CAVE needs to submit SSL cert keys to a server. This is
         * Where they would come from. 
         */
        return null;
    }

    @Override
    public char[] getKeystorePassword() {
        /** un-encrypted password to your keyStore */
        return null;
    }

}
