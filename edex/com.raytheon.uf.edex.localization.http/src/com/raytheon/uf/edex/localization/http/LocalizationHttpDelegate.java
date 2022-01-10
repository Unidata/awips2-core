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
package com.raytheon.uf.edex.localization.http;

import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.http.ProtectiveHttpOutputStream;
import com.raytheon.uf.common.http.auth.BasicCredential;
import com.raytheon.uf.common.http.auth.BasicScheme;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.localization.http.scheme.LocalizationAuthorization;

/**
 * Base class for servicing http requests for localization, subclasses can be
 * created for each specific http method that needs to be processed.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 07, 2017  5731     bsteffen  Initial creation
 * Jan 06, 2022  8735     mapeters  Update {@link #sendError} to not use output
 *                                  stream so it works for HEAD
 *
 * </pre>
 *
 * @author bsteffen
 */
public abstract class LocalizationHttpDelegate {

    protected static final String AUTHORIZATION_HEADER = "Authorization";

    protected static final String SERVER_ERROR = "Internal Server Error.";

    protected static final String PARENT_PREFIX = "..";

    protected static final String DIRECTORY_SUFFIX = "/";

    protected static final String TEXT_CONTENT = "text/plain";

    protected static final String ACCEPT_ENC_HEADER = "Accept-Encoding";

    protected static final String CONTENT_MD5_HEADER = "Content-MD5";

    protected static final String LAST_MODIFIED_HEADER = "Last-Modified";

    protected static final ThreadLocal<SimpleDateFormat> TIME_HEADER_FORMAT = TimeUtil
            .buildThreadLocalSimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                    TimeUtil.GMT_TIME_ZONE);

    protected final IUFStatusHandler log = UFStatus.getHandler(getClass());

    protected final Path basePath;

    public LocalizationHttpDelegate(Path basePath) {
        this.basePath = basePath;
    }

    public abstract void handle(HttpServletRequest request,
            HttpServletResponse response) throws IOException;

    /**
     * @param resourcePath
     * @throws LocalizationHttpException
     *             if path refers to its parent
     */
    protected void assertNonParent(Path resourcePath)
            throws LocalizationHttpException {
        if (resourcePath.startsWith(PARENT_PREFIX)) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Unable to access resource at " + resourcePath);
        }
    }

    /**
     * Send error message to client
     *
     * @param e
     * @param out
     * @throws IOException
     */
    protected void sendError(LocalizationHttpException e,
            ProtectiveHttpOutputStream out) throws IOException {
        HttpServletResponse response = out.getResponse();
        if (e.getHeaders() != null) {
            for (Entry<String, String> entry : e.getHeaders().entrySet()) {
                response.addHeader(entry.getKey(), entry.getValue());
            }
        }
        response.sendError(e.getErrorCode(), e.getMessage());
    }

    /**
     * Validates a modification request. Will throw an exception if something is
     * not valid such as not authorized.
     *
     * @param request
     * @param relative
     * @return the localization file to be saved (presuming no exceptions were
     *         thrown)
     * @throws LocalizationHttpException
     */
    protected void validate(HttpServletRequest request, Path relative)
            throws LocalizationHttpException {
        String operation = getOperation();

        BasicCredential cred = null;
        String auth = request.getHeader(AUTHORIZATION_HEADER);
        if (auth != null) {
            // TODO support more schemes
            cred = BasicScheme.parseAuthHeader(auth);
            if (cred == null) {
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_BAD_REQUEST,
                        "Unrecognized authorization: " + auth);
            }
        }

        boolean authorized = false;

        if (LocalizationResolver.isContextQuery(relative)) {
            authorized = LocalizationAuthorization.isContextAuthorized(cred,
                    operation, relative);
        } else {
            LocalizationFile lfile = LocalizationResolver.getFile(relative);
            assertNonParent(relative);
            authorized = LocalizationAuthorization.isAuthorized(cred, operation,
                    lfile.getContext(), lfile.getPath());

        }

        if (!authorized) {
            if (auth == null) {
                Map<String, String> extraHeaders = new HashMap<>();
                /*
                 * TODO Return the supported schemes? Adding the
                 * www-authenticate header causes the apache httpclient on the
                 * client side to spit out a warning if the authentication
                 * hasn't been set up yet (which it hasn't, hence returning the
                 * 401 here).
                 */
                // extraHeaders.put("WWW-Authenticate", "basic");
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "Authorization header required for "
                                + request.getMethod() + " requests",
                        extraHeaders);
            } else {
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Insufficient permissions to " + getOperation() + " "
                                + relative);
            }
        }
    }

    /**
     * @return the type of operation for checking permissions(currently
     *         read/write/delete would be valid)
     */
    protected abstract String getOperation();
}
