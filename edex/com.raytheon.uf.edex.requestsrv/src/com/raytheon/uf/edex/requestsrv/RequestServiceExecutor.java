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
package com.raytheon.uf.edex.requestsrv;

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.serialization.comm.RequestWrapper;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.AuthManager;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthenticationResponse;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.auth.resp.ResponseFactory;

/**
 * Class that handles the execution of {@link IServerRequest}s. Contains the
 * actual logic to lookup and execute the {@link IRequestHandler} registered for
 * the request passed in.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * Feb 27, 2015 4196       njensen     Null authentication data on responses
 *                                      for backwards compatibility
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class RequestServiceExecutor {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(RequestServiceExecutor.class);

    private static final RequestServiceExecutor instance = new RequestServiceExecutor();

    public static RequestServiceExecutor getInstance() {
        return instance;
    }

    private final HandlerRegistry registry;

    public RequestServiceExecutor() {
        this(HandlerRegistry.getInstance());
    }

    public RequestServiceExecutor(HandlerRegistry registry) {
        this.registry = registry;
    }

    /**
     * Executes the request passed in, delegates conversion to/from
     * {@link IServerRequest} to the {@link HandlerRegistry} set in the
     * constructor
     * 
     * @param request
     * @return The result of the service execution
     * @throws Exception
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object execute(IServerRequest request) throws Exception {
        if (request instanceof RequestWrapper) {
            // Check for wrapped request and get actual request to execute
            request = ((RequestWrapper) request).getRequest();
        }

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
                    /*
                     * TODO someday pass in updated IAuthenticationData if we
                     * have an actual implementation that uses it for security
                     */
                    return ResponseFactory.constructNotAuthenticated(privReq,
                            null);
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
                    /*
                     * TODO someday pass in updated IAuthenticationData if we
                     * have an actual implementation that uses it for security
                     */
                    return ResponseFactory.constructSuccessfulExecution(
                            privHandler.handleRequest(privReq), null);
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

}
