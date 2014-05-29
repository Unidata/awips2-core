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
package com.raytheon.uf.edex.auth;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.authentication.EmptyAuthenticationStorage;
import com.raytheon.uf.edex.auth.authentication.HonorSystemAuthenticator;
import com.raytheon.uf.edex.auth.authorization.AllowAllAuthorizer;

/**
 * Singleton class which plugins should register their authentication
 * implementations to.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 21, 2010            mschenke     Initial creation
 * May 28, 2014 3211       njensen      Added allowEverything()
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class AuthManagerFactory {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AuthManagerFactory.class);

    /** The instance for the factory */
    private static AuthManagerFactory instance = new AuthManagerFactory();

    /** The implementing auth manager */
    private volatile AuthManager manager;

    private AuthManagerFactory() {

    }

    public static AuthManagerFactory getInstance() {
        return instance;
    }

    public void setManager(AuthManager manager) {
        this.manager = manager;
    }

    public AuthManager getManager() {
        if (manager == null) {
            synchronized (this) {
                if (manager == null) {
                    manager = allowEverything();
                }
            }
        }
        return manager;
    }

    /**
     * Initializes an AuthManager which has members that allow everything
     * through. Also logs an error since this should not be encountered outside
     * of a development environment unless something is misconfigured.
     * 
     * @return an auth manager that allows everything
     */
    private AuthManager allowEverything() {
        IllegalStateException throwable = new IllegalStateException(
                "Unable to perform priviledged request validation, AuthManager not set. ALL REQUESTS WILL BE EXECUTED!");
        statusHandler.handle(Priority.PROBLEM, throwable.getLocalizedMessage(),
                throwable);

        AuthManager manager = new AuthManager();
        manager.setAuthenticationStorage(new EmptyAuthenticationStorage());
        manager.setAuthenticator(new HonorSystemAuthenticator());
        manager.setAuthorizer(new AllowAllAuthorizer());
        return manager;
    }
}
