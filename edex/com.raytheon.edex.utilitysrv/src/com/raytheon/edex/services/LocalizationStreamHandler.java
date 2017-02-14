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
package com.raytheon.edex.services;

import java.io.File;

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.stream.AbstractLocalizationStreamRequest;
import com.raytheon.uf.common.localization.stream.LocalizationStreamGetRequest;
import com.raytheon.uf.common.localization.stream.LocalizationStreamPutRequest;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;

/**
 * Base handler for localization streaming requests. Delegates work off to a
 * get/put handler
 * 
 * @deprecated Continues to exist to support older clients. Newer clients should
 *             use the Localization REST service.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2010            mschenke     Initial creation
 * Jul 14, 2014 3372       njensen      fileMap is ConcurrentHashMap for thread safety
 * Jul 16, 2014 3378       bclement     removed cache
 * Feb 17, 2015 4137       reblum       fixed timestamp on put requests
 * Nov 16, 2015 4834       njensen      Send updated checksum as part of notification after put
 * Dec 03, 2015 4834       njensen      Deprecated
 * Feb 14, 2017 6111       njensen      Split handling of get and put requests into subclasses
 * 
 * </pre>
 * 
 * @author mschenke
 */
@Deprecated
public abstract class LocalizationStreamHandler<T extends AbstractLocalizationStreamRequest>
        extends
        AbstractPrivilegedLocalizationRequestHandler<T> {

    public File validate(AbstractLocalizationStreamRequest request)
            throws LocalizationException {
        File file = PathManagerFactory.getPathManager().getFile(
                request.getContext(), request.getFileName());
        if (file == null) {
            throw new LocalizationException("File with name, "
                    + request.getFileName() + ", and context, "
                    + String.valueOf(request.getContext())
                    + ", could not be found");
        }
        return file;
    }

    @Override
    public AuthorizationResponse authorized(IUser user,
            AbstractLocalizationStreamRequest request)
            throws AuthorizationException {
        if (request instanceof LocalizationStreamGetRequest) {
            // All gets are authorized
            return new AuthorizationResponse(true);
        } else if (request instanceof LocalizationStreamPutRequest) {
            LocalizationContext context = request.getContext();
            String fileName = request.getFileName();
            return getAuthorizationResponse(user, context, fileName,
                    request.getMyContextName());
        }
        return new AuthorizationResponse(true);
    }

}
