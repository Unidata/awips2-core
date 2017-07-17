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
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;
import com.raytheon.uf.edex.localization.http.writer.ILocalizationResponseWriter;
import com.raytheon.uf.edex.localization.http.writer.html.HtmlDirectoryListingWriter;

/**
 * Handles GET http method for localization files. The actual generation of
 * content is further handed off th various {@link ILocalizationResponseWriter}s
 * and {@link IDirectoryListingWriter}s based off the mime type specified in the
 * request.
 *
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 07, 2017  5731     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class LocalizationHttpGetDelegate extends LocalizationHttpDelegate {

    protected static final String DEFAULT_RESPONSE_TYPE = "application/binary";

    protected static final String ACCEPT_CONTENT_HEADER = "Accept";

    protected static final String VARY_HEADER = "Vary";

    protected static final String REDIRECT_HEADER = "Location";

    protected static final String CACHE_CONTROL_HEADER = "Cache-Control";

    protected static final String NO_CACHE = "no-cache";

    private static final Comparator<AcceptHeaderValue> ACCEPT_COMP = Comparator
            .comparingDouble(AcceptHeaderValue::getQvalue).reversed();

    private final List<ILocalizationResponseWriter> writers = new ArrayList<>();

    private final List<IDirectoryListingWriter> listingWriters = new ArrayList<>();

    private final HtmlDirectoryListingWriter defaultListingWriter = new HtmlDirectoryListingWriter();

    public LocalizationHttpGetDelegate(Path basePath) {
        super(basePath);
    }

    /**
     * Handle HTTP GET requests for localization files and directories
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
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
            validate(request, relative);
            if (LocalizationResolver.isContextQuery(relative)) {
                if (!rawPath.endsWith(DIRECTORY_SUFFIX)) {
                    sendRedirect(out, rawPath + DIRECTORY_SUFFIX,
                            request.getQueryString());

                } else {
                    handleContextQuery(relative, responseTypes, out);
                }
            } else {

                if (rawPath.endsWith(DIRECTORY_SUFFIX)) {
                    LocalizationContext context = LocalizationResolver
                            .getContext(relative);
                    Path afterContext = LocalizationResolver.relativize(context,
                            relative);
                    if (afterContext.toString().isEmpty()) {
                        ResponsePair<ILocalizationResponseWriter> writerPair = findWriter(
                                responseTypes);
                        handleDirectory(request, context, "", writerPair, out);
                    } else {
                        handleFile(request, rawPath, relative, responseTypes,
                                out);
                    }
                } else {
                    handleFile(request, rawPath, relative, responseTypes, out);
                }
            }
        } catch (LocalizationHttpException e) {
            sendError(e, out);
        } catch (Throwable t) {
            log.error("Problem handling localization get request: " + fullPath,
                    t);
            sendError(new LocalizationHttpException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR),
                    out);
        } finally {
            out.flush();
            out.setAllowClose(true);
            out.close();
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
     * Redirect client to URL
     *
     * @param out
     * @param url
     * @param string
     * @param map
     * @throws IOException
     */
    private void sendRedirect(ProtectiveHttpOutputStream out, String url,
            String queryString) throws IOException {
        if (!out.used()) {
            HttpServletResponse response = out.getResponse();
            response.setContentType(TEXT_CONTENT);
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            if (queryString != null) {
                url = url + '?' + queryString;
            }
            response.setHeader(REDIRECT_HEADER, url);
            out.write(("Resource moved permanently to " + url).getBytes());
        } else {
            log.error("Unable to send redirect message to client"
                    + ", stream already used");
        }
    }

    /**
     * Generate directory listing for context parts such as localization type,
     * level, and context name for non-base contexts.
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
        } else if (nameCount == 2) {
            String typeString = resourcePath.getName(0).toString();
            if (LocalizationResolver.findType(typeString) == null) {
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_NOT_FOUND,
                        "No such localization type: " + typeString);
            }
            String levelString = resourcePath.getName(1).toString();
            LocalizationLevel level = LocalizationResolver
                    .findLevel(levelString);
            IPathManager pathManager = PathManagerFactory.getPathManager();
            String[] contexts = pathManager.getContextList(level);
            results = new ArrayList<>(contexts.length);
            for (String context : contexts) {
                results.add(context + DIRECTORY_SUFFIX);
            }
        } else {
            log.error("Unexpected resource path for context query: "
                    + resourcePath);
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR);
        }
        Collections.sort(results);
        ResponsePair<IDirectoryListingWriter> responsePair = findListingWriter(
                responseTypes);
        IDirectoryListingWriter writer = responsePair.getWriter();
        HttpServletResponse response = out.getResponse();
        response.setContentType(
                responsePair.getResponseType().toStringWithoutParams());
        /* Control caching by web browsers. */
        response.setHeader(VARY_HEADER, ACCEPT_CONTENT_HEADER);
        response.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        writer.write(responsePair.getResponseType(), results, out);
    }

    /**
     * Handle generating a response for a localization directory
     */
    private void handleDirectory(HttpServletRequest request,
            LocalizationContext context, String path,
            ResponsePair<ILocalizationResponseWriter> writerPair,
            ProtectiveHttpOutputStream out) throws IOException {
        /*
         * Disable response encoding for directories so we don't potentially
         * gzip a large zip file. Missing the opportunity to gzip the directory
         * listing isn't too high a price to pay.
         */
        out.setAcceptEncoding(null);

        IPathManager pathManager = PathManagerFactory.getPathManager();

        LocalizationFile lfile = pathManager.getLocalizationFile(context, path);
        HttpServletResponse response = out.getResponse();
        response.setContentType(
                writerPair.getResponseType().toStringWithoutParams());
        Date timeStamp = lfile.getTimeStamp();
        if (timeStamp != null) {
            SimpleDateFormat format = TIME_HEADER_FORMAT.get();
            response.setHeader(LAST_MODIFIED_HEADER, format.format(timeStamp));
        }
        response.setHeader(VARY_HEADER, ACCEPT_CONTENT_HEADER);
        response.setHeader(CACHE_CONTROL_HEADER, NO_CACHE);
        writerPair.write(request, context, path, out);
    }

    /**
     * Handle GET request for generic file
     *
     * @param request,
     *            the originating http request
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
    private void handleFile(HttpServletRequest request, String rawPath,
            Path relative, List<MimeType> responseTypes,
            ProtectiveHttpOutputStream out) throws LocalizationHttpException,
            IOException, LocalizationException {
        LocalizationFile lfile = LocalizationResolver.getFile(relative);
        /*
         * don't use exists() on the localization file since that has a side
         * effect of the parent directories being created
         */
        File file = lfile.getFile(false);
        if (!file.exists()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_NOT_FOUND,
                    "Resource not found: " + rawPath);
        }
        if (!file.canRead()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Unable to read resource: " + rawPath);
        }
        if (file.isDirectory()) {
            if (!rawPath.endsWith(DIRECTORY_SUFFIX)) {
                sendRedirect(out, rawPath + DIRECTORY_SUFFIX,
                        request.getQueryString());
            } else {
                ResponsePair<ILocalizationResponseWriter> writerPair = findWriter(
                        responseTypes);
                LocalizationContext context = lfile.getContext();
                handleDirectory(request, context, lfile.getName(), writerPair,
                        out);
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
            ProtectiveHttpOutputStream out)
            throws IOException, LocalizationException {
        File file = lfile.getFile();
        String typeString = java.nio.file.Files.probeContentType(file.toPath());
        String responseType = DEFAULT_RESPONSE_TYPE;
        if (typeString != null) {
            responseType = typeString;
        }
        HttpServletResponse response = out.getResponse();
        response.setContentType(responseType);

        Date timeStamp = new Date(file.lastModified());
        /* Use the checksum already in memory if possible */
        if (timeStamp.equals(lfile.getTimeStamp())) {
            response.setHeader(CONTENT_MD5_HEADER, lfile.getCheckSum());
        } else {
            /*
             * The PathManager does not necessarily update if a file changes so
             * must get it from the file directly.
             */
            response.setHeader(CONTENT_MD5_HEADER,
                    ChecksumIO.getFileChecksum(file));
        }

        SimpleDateFormat format = TIME_HEADER_FORMAT.get();
        response.setHeader(LAST_MODIFIED_HEADER, format.format(timeStamp));
        copy(lfile, out);
    }

    /**
     * Copies the contents of the localization file to the output stream.
     * Provided so HEAD can override and not send the contents.
     */
    protected void copy(LocalizationFile lfile, OutputStream out)
            throws LocalizationException, IOException {
        LocalizationHttpDataTransfer.copy(lfile, out);
    }

    /**
     * @see #findWriter(List, List, ResponsePair)
     *
     * @param responseTypes
     * @return
     * @throws LocalizationHttpException
     */
    private ResponsePair<ILocalizationResponseWriter> findWriter(
            List<MimeType> responseTypes) throws LocalizationHttpException {
        ResponsePair<ILocalizationResponseWriter> defaultResult = new ResponsePair<>(
                HtmlDirectoryListingWriter.CONTENT_TYPE, defaultListingWriter);
        return findWriter(responseTypes, writers, defaultResult);
    }

    /**
     * @see #findWriter(List, List, ResponsePair)
     *
     * @param responseTypes
     * @return
     * @throws LocalizationHttpException
     */
    private ResponsePair<IDirectoryListingWriter> findListingWriter(
            List<MimeType> responseTypes) throws LocalizationHttpException {
        ResponsePair<IDirectoryListingWriter> defaultResult = new ResponsePair<>(
                HtmlDirectoryListingWriter.CONTENT_TYPE, defaultListingWriter);
        return findWriter(responseTypes, listingWriters, defaultResult);
    }

    /**
     * Find the best match for a preferred response type
     *
     * @param responseTypes
     * @param writers
     * @param defaultResult
     * @return default listing result if no match is found
     * @throws LocalizationHttpException
     */
    private <T extends ILocalizationResponseWriter> ResponsePair<T> findWriter(
            List<MimeType> responseTypes, List<T> writers,
            ResponsePair<T> defaultResult) throws LocalizationHttpException {
        ResponsePair<T> rval = defaultResult;
        T defaultWriter = defaultResult.getWriter();
        if (!responseTypes.isEmpty()) {
            /* If an accept header was specified then it must be matched */
            rval = null;
            for (MimeType responseType : responseTypes) {
                if (defaultWriter.generates(responseType)) {
                    rval = defaultResult;
                    break;
                }
                synchronized (this.writers) {
                    for (T writer : writers) {
                        if (writer.generates(responseType)) {
                            rval = new ResponsePair<>(responseType, writer);
                            break;
                        }
                    }
                }
                if (rval != null) {
                    break;
                }
            }
            if (rval == null) {
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_NOT_ACCEPTABLE,
                        "Unable to generate acceptable response.");
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
    public void register(ILocalizationResponseWriter... writers) {
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
    }

    @Override
    protected String getOperation() {
        return "read";
    }
}
