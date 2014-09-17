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

import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.util.registry.GenericRegistry;

/**
 * Class used for registering {@link IRequestHandler}s for the
 * {@link IServerRequest} objects they handle. Registry is used by service
 * executors to look up the handler to execute the request
 * 
 * TODO: Once {@link RemoteRequestServer} is deleted and replaced by
 * {@link RequestServiceExecutor}, no longer implement the
 * {@link IHandlerRegistry} interface. Only implement so legacy code will still
 * work with registry in this project
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug  6, 2009            mschenke    Initial creation
 * Aug 15, 2014 3541       mschenke    Moved from auth to services plugin
 * Sep 16, 2014 3356       njensen     DefaultHandler throws IllegalState, not ClassNotFound
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class HandlerRegistry extends GenericRegistry<String, Object> {

    private static final HandlerRegistry instance = new HandlerRegistry(
            new DefaultHandler());

    static class DefaultHandler implements IRequestHandler<IServerRequest> {
        @Override
        public Object handleRequest(IServerRequest request) throws Exception {
            throw new IllegalStateException("No handler registered for type: "
                    + request.getClass().getCanonicalName());
        }
    }

    /** Default request handler to use when non registered for a request */
    private final IRequestHandler<?> defaultHandler;

    public static HandlerRegistry getInstance() {
        return instance;
    }

    public HandlerRegistry(IRequestHandler<?> defaultHandler) {
        this.defaultHandler = defaultHandler;
    }

    /**
     * @param object
     * @return The {@link IRequestHandler} registered to handle requests with
     *         class name passed in
     */
    public IRequestHandler<?> getRequestHandler(String objectType) {
        Object obj = super.getRegisteredObject(objectType);
        if (obj instanceof IRequestHandler<?>) {
            return (IRequestHandler<?>) obj;
        }
        return defaultHandler;
    }

    /**
     * @return The default {@link IRequestHandler} that will be returned from
     *         {@link #getRequestHandler(String)} when no handler found in the
     *         registry
     */
    public IRequestHandler<?> getDefaultHandler() {
        return defaultHandler;
    }

}
