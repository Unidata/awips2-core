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

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthenticationResponse;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.auth.resp.ResponseFactory;

/**
 * The server used for executing requests through handlers. Request canonical
 * name should be used to map requests to handlers (register in spring.xml
 * files)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 03, 2009            mschenke    Initial creation
 * May 28, 2014 3211       njensen     Refactored and improved error msgs
 * 
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class RemoteRequestServer {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(RemoteRequestServer.class);

    private static final RemoteRequestServer instance = new RemoteRequestServer();

    private HandlerRegistry registry;

    public static RemoteRequestServer getInstance() {
        return instance;
    }

    private RemoteRequestServer() {

    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object handleThriftRequest(IServerRequest request) throws Exception {
        String id = request.getClass().getCanonicalName();
        IRequestHandler handler = registry.getRequestHandler(id);

        if (request instanceof AbstractPrivilegedRequest) {
            AuthManager manager = AuthManagerFactory.getInstance().getManager();
            // Not the default role, attempt to cast handler and request
            try {
                AbstractPrivilegedRequest privReq = (AbstractPrivilegedRequest) request;
                AbstractPrivilegedRequestHandler privHandler = (AbstractPrivilegedRequestHandler) handler;

                IUser user = privReq.getUser();

                // Do not process request if user passed in is null
                if (user == null || user.uniqueId() == null) {
                    return ResponseFactory.constructNotAuthorized(privReq,
                            "Unable to process privileged request " + request
                                    + " for null user");
                }

                // check that user is who they claim to be (authentication)
                AuthenticationResponse resp = manager.getAuthenticator()
                        .authenticate(user);
                if (!resp.isAuthenticated()) {
                    return ResponseFactory.constructNotAuthenticated(privReq,
                            resp.getUpdatedData());
                }

                /*
                 * check handler that user is allowed to execute this request
                 * (authorization)
                 */
                AuthorizationResponse authResp = privHandler.authorized(user,
                        privReq);
                if (authResp != null && !authResp.isAuthorized()
                        && authResp.getResponseMessage() != null) {
                    return ResponseFactory.constructNotAuthorized(privReq,
                            authResp.getResponseMessage());
                }

                /*
                 * they've passed authentication and authorization, let the
                 * handler execute the request
                 */
                try {
                    return ResponseFactory.constructSuccessfulExecution(
                            privHandler.handleRequest(privReq),
                            resp.getUpdatedData());
                } catch (Throwable t) {
                    throw new AuthException(resp.getUpdatedData(), t);
                }
            } catch (ClassCastException e) {
                throw new AuthException(
                        "Roles can only be defined for requests/handlers of AbstractPrivilegedRequest/Handler, request was "
                                + request.getClass().getName(), e);

            } catch (Throwable t) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error occured while performing privileged request "
                                + request, t);
                throw new AuthException(
                        "Error occured while performing privileged request "
                                + request, t);
            }
        }

        return handler.handleRequest(request);
    }

    public void setRegistry(HandlerRegistry registry) {
        this.registry = registry;
    }

}
