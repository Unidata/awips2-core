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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.http.AcceptHeaderParser;
import com.raytheon.uf.common.http.AcceptHeaderValue;
import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.http.ProtectiveHttpOutputStream;
import com.raytheon.uf.common.localization.FileLocker;
import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.localization.http.scheme.BasicScheme;
import com.raytheon.uf.edex.localization.http.scheme.LocalizationAuthorization;
import com.raytheon.uf.edex.localization.http.writer.HtmlDirectoryListingWriter;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;
import com.raytheon.uf.edex.localization.http.writer.ILocalizationResponseWriter;

/**
 * REST service for handling localization file requests over HTTP.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 12, 2015 3978       bclement    Initial creation
 * Dec 02, 2015 4834       njensen     Added support for PUT requests
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class LocalizationHttpService {

    public static final String SERVER_ERROR = "Internal Server Error.";

    private static final String PARENT_PREFIX = "..";

    private static final String DIRECTORY_SUFFIX = "/";

    private static final String TEXT_CONTENT = "text/plain";

    private static final String DEFAULT_RESPONSE_TYPE = "application/binary";

    private static final String ACCEPT_ENC_HEADER = "accept-encoding";

    private static final String ACCEPT_CONTENT_HEADER = "accept";

    private static final String REDIRECT_HEADER = "location";

    private static final String CONTENT_MD5_HEADER = "content-md5";

    private static final String LAST_MODIFIED_HEADER = "last-modified";

    private static final String IF_MATCH_HEADER = "if-match";

    private static final String AUTHORIZATION_HEADER = "authorization";

    private static final String BASIC_SCHEME = "basic";

    private static final IUFStatusHandler log = UFStatus
            .getHandler(LocalizationHttpService.class);

    private static final Comparator<AcceptHeaderValue> ACCEPT_COMP = new Comparator<AcceptHeaderValue>() {
        @Override
        public int compare(AcceptHeaderValue o1, AcceptHeaderValue o2) {
            /* largest first ordering */
            return Double.compare(o2.getQvalue(), o1.getQvalue());
        }
    };

    private static final ThreadLocal<SimpleDateFormat> TIME_HEADER_FORMAT = TimeUtil
            .buildThreadLocalSimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                    TimeUtil.GMT_TIME_ZONE);

    private final Path basePath;

    private final List<ILocalizationResponseWriter> writers = new ArrayList<>();

    private final List<IDirectoryListingWriter> listingWriters = new ArrayList<>();

    private final HtmlDirectoryListingWriter defaultListingWriter = new HtmlDirectoryListingWriter();

    /**
     * @param base
     *            portion of URL that is used for routing to this service
     */
    public LocalizationHttpService(String base) {
        this.basePath = Paths.get(base);
    }

    /**
     * Handle HTTP GET requests for localization files and directories
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void handleGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String rawPath = request.getPathInfo();
        Path fullPath = Paths.get(rawPath);

        Path relative = basePath.relativize(fullPath);
        if (!relative.toString().isEmpty()) {
            relative = relative.normalize();
        }

        List<MimeType> responseTypes = getAcceptableResponses(request);
        String acceptEncoding = request.getHeader(ACCEPT_ENC_HEADER);

        ProtectiveHttpOutputStream out = new ProtectiveHttpOutputStream(
                response, acceptEncoding, false);

        try {
            assertNonParent(relative);
            if (LocalizationResolver.isContextQuery(relative)) {
                if (!rawPath.endsWith(DIRECTORY_SUFFIX)) {
                    sendRedirect(out, rawPath + DIRECTORY_SUFFIX);
                } else {
                    handleContextQuery(relative, responseTypes, out);
                }
            } else {
                if (rawPath.endsWith(DIRECTORY_SUFFIX)) {
                    LocalizationContext context = LocalizationResolver
                            .getContext(relative);
                    Path afterContext = LocalizationResolver.relativize(
                            context, relative);
                    if (afterContext.toString().isEmpty()) {
                        ResponsePair<ILocalizationResponseWriter> writerPair = findWriter(responseTypes);
                        handleDirectory(context, "", writerPair, out);
                    } else {
                        handleFile(rawPath, relative, responseTypes, out);
                    }
                } else {
                    handleFile(rawPath, relative, responseTypes, out);
                }
            }
        } catch (LocalizationHttpException e) {
            sendError(e, out);
        } catch (Throwable t) {
            log.error("Problem handling localization get request: " + fullPath,
                    t);
            sendError(
                    new LocalizationHttpException(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            SERVER_ERROR), out);
        } finally {
            out.flush();
            out.setAllowClose(true);
            out.close();
        }
    }

    /**
     * Handle HTTP PUT requests for localization files
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void handlePut(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        String rawPath = request.getPathInfo();
        Path fullPath = Paths.get(rawPath);

        Path relative = basePath.relativize(fullPath);
        if (!relative.toString().isEmpty()) {
            relative = relative.normalize();
        }

        String acceptEncoding = request.getHeader(ACCEPT_ENC_HEADER);
        ProtectiveHttpOutputStream out = new ProtectiveHttpOutputStream(
                response, acceptEncoding, false);

        try {
            LocalizationFile lfile = validatePutRequest(request, relative);

            /*
             * Passed all the initial checks, acquire the lock and verify
             * another process did not alter the file in the meantime. getFile()
             * will also create parent directories as necessary.
             */
            File file = lfile.getFile();
            try {
                FileLocker.lock(this, file, Type.WRITE);

                /*
                 * The LocalizationFile instance may have come from an in-memory
                 * cache, in which case we can't be 100% positive that the
                 * checksum in memory is up-to-date. An outside process that
                 * didn't go through normal routes may have modified the file,
                 * so we need to get the checksum from the file on the
                 * filesystem to be safe.
                 */
                String currentChecksum = ChecksumIO.getFileChecksum(file);
                String preModChecksum = request.getHeader(IF_MATCH_HEADER);
                if (!currentChecksum.equals(preModChecksum)) {
                    StringBuilder sb = new StringBuilder(256);
                    sb.append("The file ");
                    sb.append(lfile.getPath());
                    sb.append(" as version ");
                    String newContentMd5 = request
                            .getHeader(CONTENT_MD5_HEADER);
                    if (newContentMd5 == null) {
                        newContentMd5 = "**No Content-MD5 Header Supplied**";
                    }
                    sb.append(newContentMd5);
                    sb.append(" has not been saved because it has been changed by another process. ");
                    sb.append("The client modified the file based on version ");
                    sb.append(preModChecksum);
                    sb.append(" but the localization service's latest version is ");
                    sb.append(currentChecksum);
                    sb.append(". Please consider updating to the latest version of the ");
                    sb.append("file and merging the changes.");
                    throw new LocalizationHttpException(
                            HttpServletResponse.SC_CONFLICT, sb.toString());
                }

                FileChangeType changeType = FileChangeType.UPDATED;
                if (!file.exists()) {
                    changeType = FileChangeType.ADDED;
                }

                /*
                 * Proceed with the put by writing it to a temporary file and
                 * then replacing the original file.
                 */
                Path parentPath = file.toPath().getParent();
                Path tmpFile = null;
                try {
                    tmpFile = Files.createTempFile(parentPath, file.getName(),
                            ".tmp");
                    try (FileOutputStream fos = new FileOutputStream(
                            tmpFile.toFile())) {
                        try (InputStream is = request.getInputStream()) {
                            byte[] buf = new byte[8192];
                            int n = 0;
                            while ((n = is.read(buf)) > 0) {
                                fos.write(buf, 0, n);
                            }
                        }
                    }
                } catch (Throwable t) {
                    if (tmpFile != null) {
                        Files.deleteIfExists(tmpFile);
                    }
                    throw t;
                }

                Files.move(tmpFile, file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                // generate the new checksum after the change
                String checksum = ChecksumIO.writeChecksum(file);
                long timeStamp = file.lastModified();

                // notify topic the file has changed
                EDEXUtil.getMessageProducer().sendAsync(
                        "utilityNotify",
                        new FileUpdatedMessage(lfile.getContext(), lfile
                                .getPath(), changeType, timeStamp, checksum));

                response.setStatus(HttpServletResponse.SC_OK);
                SimpleDateFormat format = TIME_HEADER_FORMAT.get();
                response.setHeader(LAST_MODIFIED_HEADER,
                        format.format(timeStamp));
                response.setHeader(CONTENT_MD5_HEADER, checksum);
            } finally {
                FileLocker.unlock(this, file);
            }
        } catch (LocalizationHttpException e) {
            sendError(e, out);
        } catch (Throwable t) {
            log.error("Problem handling localization put request: " + fullPath,
                    t);
            sendError(
                    new LocalizationHttpException(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            SERVER_ERROR), out);
        } finally {
            out.flush();
            out.setAllowClose(true);
            out.close();
        }
    }

    /**
     * Validates a put request. Will throw an exception if something is not
     * valid such as not authorized.
     * 
     * @param request
     * @param relative
     * @return the localization file to be saved (presuming no exceptions were
     *         thrown)
     * @throws LocalizationHttpException
     */
    private LocalizationFile validatePutRequest(HttpServletRequest request,
            Path relative) throws LocalizationHttpException {
        LocalizationFile lfile = LocalizationResolver.getFile(relative);
        assertNonParent(relative);

        String auth = request.getHeader(AUTHORIZATION_HEADER);
        if (auth == null) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization header required for PUT requests");
        }

        int index = auth.indexOf(" ");
        if (index < 1) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Unrecognized authorization scheme");
        }

        // TODO support more schemes
        String scheme = auth.substring(0, index);
        String key = auth.substring(index + 1);
        String username = null;
        if (scheme.equalsIgnoreCase(BASIC_SCHEME)) {
            username = BasicScheme.authenticate(key);
        } else {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unsupported authorization scheme: " + scheme);
        }

        /*
         * Got the username, validate that they have permissions to put against
         * this localization context and path.
         */
        if (!LocalizationAuthorization.isPutAuthorized(username,
                lfile.getContext(), lfile.getPath())) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Insufficient permissions to modify " + lfile);
        }

        if (lfile.isDirectory()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "Put requests not allowed on directories");
        }

        return lfile;
    }

    /**
     * Redirect client to URL
     * 
     * @param out
     * @param url
     * @throws IOException
     */
    private void sendRedirect(ProtectiveHttpOutputStream out, String url)
            throws IOException {
        if (!out.used()) {
            HttpServletResponse response = out.getResponse();
            response.setContentType(TEXT_CONTENT);
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader(REDIRECT_HEADER, url);
            out.write(("Resource moved permanently to " + url).getBytes());
        } else {
            log.error("Unable to send redirect message to client"
                    + ", stream already used");
        }
    }

    /**
     * Send error message to client
     * 
     * @param e
     * @param out
     * @throws IOException
     */
    private void sendError(LocalizationHttpException e,
            ProtectiveHttpOutputStream out) throws IOException {
        if (!out.used()) {
            HttpServletResponse response = out.getResponse();
            response.setContentType(TEXT_CONTENT);
            response.setStatus(e.getErrorCode());
            out.write(e.getMessage().getBytes());
        } else {
            log.error("Unable to send error message to client"
                    + ", stream already used", e);
        }
    }

    /**
     * @param resourcePath
     * @throws LocalizationHttpException
     *             if path refers to its parent
     */
    private void assertNonParent(Path resourcePath)
            throws LocalizationHttpException {
        if (resourcePath.startsWith(PARENT_PREFIX)) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Unable to access resource at " + resourcePath);
        }
    }

    /**
     * @param request
     * @return list of desired response content types in descending order or
     *         preference
     */
    private List<MimeType> getAcceptableResponses(HttpServletRequest request) {
        String acceptContentString = request.getHeader(ACCEPT_CONTENT_HEADER);
        List<MimeType> rval;
        if (acceptContentString != null) {
            AcceptHeaderParser parser = new AcceptHeaderParser(
                    acceptContentString);
            List<AcceptHeaderValue> values = parser.getValues();
            Collections.sort(values, ACCEPT_COMP);
            rval = new ArrayList<>(values.size());
            for (AcceptHeaderValue value : values) {
                /*
                 * TODO if the type is unacceptable, the service shouldn't
                 * generate it. An HttpServletResponse.SC_NOT_ACCEPTABLE should
                 * be returned if we can't match anything the client accepts.
                 */
                if (value.isAcceptable()) {
                    rval.add(new MimeType(value.getEncoding()));
                }
            }
        } else {
            rval = Collections.emptyList();
        }
        return rval;
    }

    /**
     * Generate directory listing for context parts (localization type or level)
     * 
     * @param resourcePath
     * @param responseTypes
     * @param out
     * @throws LocalizationHttpException
     * @throws IOException
     */
    private void handleContextQuery(Path resourcePath,
            List<MimeType> responseTypes, ProtectiveHttpOutputStream out)
            throws LocalizationHttpException, IOException {
        int nameCount = resourcePath.getNameCount();
        List<String> results;
        if (resourcePath.toString().isEmpty()) {
            LocalizationType[] types = LocalizationType.values();
            results = new ArrayList<>(types.length);
            for (LocalizationType type : types) {
                results.add(type.name().toLowerCase() + DIRECTORY_SUFFIX);
            }
        } else if (nameCount == 1) {
            /*
             * have to check if type exists for REST compatibility, but we don't
             * need it to list the levels since each type has the same levels
             */
            String typeString = resourcePath.getName(0).toString();
            if (LocalizationResolver.findType(typeString) == null) {
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_NOT_FOUND,
                        "No such localization type: " + typeString);
            }
            LocalizationLevel[] levels = LocalizationLevel.values();
            results = new ArrayList<>(levels.length);
            for (LocalizationLevel level : levels) {
                results.add(level.name().toLowerCase() + DIRECTORY_SUFFIX);
            }
        } else {
            log.error("Unexpected resource path for context query: "
                    + resourcePath);
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR);
        }
        ResponsePair<IDirectoryListingWriter> responsePair = findListingWriter(responseTypes);
        IDirectoryListingWriter writer = responsePair.getWriter();
        writer.write(responsePair.getResponseType(), results, out);
    }

    /**
     * Handle generating a response for a localization directory
     * 
     * @param context
     * @param path
     * @param writerPair
     * @param out
     * @throws IOException
     */
    private void handleDirectory(LocalizationContext context, String path,
            ResponsePair<ILocalizationResponseWriter> writerPair,
            ProtectiveHttpOutputStream out) throws IOException {
        /*
         * Disable response encoding for directories so we don't potentially
         * gzip a large zip file. Missing the opportunity to gzip the directory
         * listing isn't too high a price to pay.
         */
        out.setAcceptEncoding(null);
        writerPair.write(context, path, out);
    }

    /**
     * Handle GET request for generic file
     * 
     * @param rawPath
     *            full URL path
     * @param relative
     *            portion of URL after base path
     * @param responseTypes
     *            desired response content types
     * @param out
     * @throws LocalizationHttpException
     * @throws IOException
     * @throws LocalizationException
     */
    private void handleFile(String rawPath, Path relative,
            List<MimeType> responseTypes, ProtectiveHttpOutputStream out)
            throws LocalizationHttpException, IOException,
            LocalizationException {
        LocalizationFile lfile = LocalizationResolver.getFile(relative);
        /*
         * don't use exists() on the localization file since that has a side
         * effect of the parent directories being created
         */
        File file = lfile.getFile(false);
        if (!file.exists()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_NOT_FOUND, "Resource not found: "
                            + rawPath);
        }
        if (!file.canRead()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Unable to read resource: " + rawPath);
        }
        if (file.isDirectory()) {
            if (!rawPath.endsWith(DIRECTORY_SUFFIX)) {
                sendRedirect(out, rawPath + DIRECTORY_SUFFIX);
            } else {
                ResponsePair<ILocalizationResponseWriter> writerPair = findWriter(responseTypes);
                LocalizationContext context = lfile.getContext();
                handleDirectory(context, lfile.getName(), writerPair, out);
            }
        } else {
            handleRegularFile(lfile, out);
        }
    }

    /**
     * Handle GET requests for regular (non-directory) files
     * 
     * @param lfile
     * @param out
     * @throws IOException
     * @throws LocalizationException
     */
    private void handleRegularFile(LocalizationFile lfile,
            ProtectiveHttpOutputStream out) throws IOException,
            LocalizationException {
        String typeString = Files.probeContentType(lfile.getFile().toPath());
        String responseType = DEFAULT_RESPONSE_TYPE;
        if (typeString != null) {
            responseType = typeString;
        }
        HttpServletResponse response = out.getResponse();
        response.setContentType(responseType);

        String checkSum = lfile.getCheckSum();
        if (checkSum != null) {
            response.setHeader(CONTENT_MD5_HEADER, lfile.getCheckSum());
        }

        Date timeStamp = lfile.getTimeStamp();
        if (timeStamp != null) {
            SimpleDateFormat format = TIME_HEADER_FORMAT.get();
            response.setHeader(LAST_MODIFIED_HEADER, format.format(timeStamp));
        }

        LocalizationHttpDataTransfer.copy(lfile, out);
    }

    /**
     * @see #findWriter(List, List, ResponsePair)
     * 
     * @param responseTypes
     * @return
     */
    private ResponsePair<IDirectoryListingWriter> findListingWriter(
            List<MimeType> responseTypes) {
        ResponsePair<IDirectoryListingWriter> defaultResult = new ResponsePair<IDirectoryListingWriter>(
                HtmlDirectoryListingWriter.CONTENT_TYPE, defaultListingWriter);
        return findWriter(responseTypes, listingWriters, defaultResult);
    }

    /**
     * @see #findWriter(List, List, ResponsePair)
     * 
     * @param responseTypes
     * @return
     */
    private ResponsePair<ILocalizationResponseWriter> findWriter(
            List<MimeType> responseTypes) {
        ResponsePair<ILocalizationResponseWriter> defaultResult = new ResponsePair<ILocalizationResponseWriter>(
                HtmlDirectoryListingWriter.CONTENT_TYPE, defaultListingWriter);
        return findWriter(responseTypes, writers, defaultResult);
    }

    /**
     * Find the best match for a preferred response type
     * 
     * @param responseTypes
     * @param writers
     * @param defaultResult
     * @return default listing result if no match is found
     */
    private <T extends ILocalizationResponseWriter> ResponsePair<T> findWriter(
            List<MimeType> responseTypes, List<T> writers,
            ResponsePair<T> defaultResult) {
        ResponsePair<T> rval = defaultResult;
        T defaultWriter = defaultResult.getWriter();
        if (!responseTypes.isEmpty()) {
            for (MimeType responseType : responseTypes) {
                if (defaultWriter.generates(responseType)) {
                    break;
                }
                synchronized (writers) {
                    for (T writer : writers) {
                        if (writer.generates(responseType)) {
                            rval = new ResponsePair<T>(responseType, writer);
                            break;
                        }
                    }
                }
            }
        }
        return rval;
    }

    /**
     * Register response writers with service
     * 
     * @param writers
     * @return self for spring compatibility
     */
    public LocalizationHttpService register(
            ILocalizationResponseWriter... writers) {
        synchronized (this.writers) {
            this.writers.addAll(Arrays.asList(writers));
        }
        synchronized (this.listingWriters) {
            for (ILocalizationResponseWriter writer : writers) {
                // TODO avoid using instanceof
                if (writer instanceof IDirectoryListingWriter) {
                    this.listingWriters.add((IDirectoryListingWriter) writer);
                }
            }
        }
        return this;
    }

}
