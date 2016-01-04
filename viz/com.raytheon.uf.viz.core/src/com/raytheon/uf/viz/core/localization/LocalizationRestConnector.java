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
package com.raytheon.uf.viz.core.localization;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationAdapter;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.localization.exception.LocalizationPermissionDeniedException;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.viz.core.VizApp;

/**
 * Handles sending REST http requests to the localization REST service. In
 * general this is more efficient than going through dynamicSerialize and the
 * requestSrv (aka thriftSrv).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 16, 2015  3978      njensen     Initial creation
 * Dec 03, 2015  4834      njensen     Added PUT support
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class LocalizationRestConnector {

    private static final String DIRECTORY_SUFFIX = "/";

    private static final String SERVICE = "localization";

    private static final String ACCEPT = "Accept";

    private static final String DIR_FORMAT = "application/zip";

    private static final String IF_MATCH = "If-Match";

    private static final String CONTENT_MD5 = "Content-MD5";

    private static final String LAST_MODIFIED = "Last-Modified";

    private static final ThreadLocal<SimpleDateFormat> TIME_HEADER_FORMAT = TimeUtil
            .buildThreadLocalSimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z",
                    TimeUtil.GMT_TIME_ZONE);

    private final ILocalizationAdapter adapter;

    public LocalizationRestConnector(ILocalizationAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Builds a localization REST service address based on the parameters
     * 
     * @param context
     *            the localization context
     * @param filename
     *            the localization filename
     * @param isDirectory
     *            if the file is a directory
     * @return a String URL for the file
     * @throws CommunicationException
     */
    private String buildRestAddress(LocalizationContext context,
            String filename, boolean isDirectory) throws CommunicationException {
        StringBuilder path = new StringBuilder();

        path.append(DIRECTORY_SUFFIX);
        path.append(SERVICE);
        path.append(DIRECTORY_SUFFIX);
        /*
         * Localization type and level have to be lowercase cause that's the way
         * they are on the filesystem. The context name should not be altered
         * though. According to w3 spec, you cannot expect a URL to be
         * case-insensitive.
         */
        path.append(context.getLocalizationType().toString().toLowerCase());
        path.append(DIRECTORY_SUFFIX);
        path.append(context.getLocalizationLevel().toString().toLowerCase());
        path.append(DIRECTORY_SUFFIX);

        // only base files can have a null context name
        String contextName = context.getContextName();
        if (contextName == null
                && !context.getLocalizationLevel().equals(
                        LocalizationLevel.BASE)) {
            throw new IllegalArgumentException(
                    "LocalizationContext is missing context name!  Only BASE level contexts are allowed to not have a context name");
        }
        if (contextName != null) {
            path.append(contextName);
            path.append(DIRECTORY_SUFFIX);
        }
        path.append(filename);
        if (isDirectory && !filename.endsWith(DIRECTORY_SUFFIX)) {
            path.append(DIRECTORY_SUFFIX);
        }

        try {
            String serverAddress = VizApp.getHttpServer();
            if (serverAddress.endsWith(DIRECTORY_SUFFIX)) {
                serverAddress = serverAddress.substring(0,
                        serverAddress.length() - 1);
            }
            URI serverURI = new URI(serverAddress);
            /*
             * We have to use the URI constructor that takes multiple arguments
             * for it to properly replace path values such as replacing space
             * with %20.
             * 
             * TODO: If we have a newer version of Guava we should be able to
             * use UrlEscapers.urlPathSegementEscaper() and apply that to the
             * path portion, then concatenate the server string with the escaped
             * path string and skip URIs altogether.
             */
            URI fullURI = new URI(serverURI.getScheme(), null,
                    serverURI.getHost(), serverURI.getPort(),
                    serverURI.getPath() + path.toString(), null, null);
            return fullURI.toASCIIString();
        } catch (URISyntaxException e) {
            throw new CommunicationException(
                    "Error determining server's localization REST address", e);
        }

    }

    /**
     * Sends a GET request to the localization REST service for a directory.
     * This should only be used for directories and is more efficient than
     * getting each file individually. The directory will be downloaded
     * recursively, including all files and sub-directories.
     * 
     * @param context
     * @param dirname
     * @return the response
     * @throws CommunicationException
     *             if the http connection failed or the server returned a status
     *             code other than 200
     */
    public HttpClientResponse restGetDirectory(LocalizationContext context,
            String dirname) throws CommunicationException {
        String url = buildRestAddress(context, dirname, true);
        HttpGet request = new HttpGet(url);
        request.addHeader(ACCEPT, DIR_FORMAT);
        File outputDir = this.adapter.getPath(context, dirname);
        DownloadDirAsZipStreamHandler streamHandler = new DownloadDirAsZipStreamHandler(
                outputDir, context.getLocalizationLevel().isSystemLevel());
        HttpClientResponse resp = HttpClient.getInstance().executeRequest(
                request, streamHandler);
        return resp;
    }

    /**
     * Sends a GET request to the localization REST service for a file.
     * 
     * @param context
     * @param filename
     * @return the response
     * @throws CommunicationException
     *             if the http connection failed or the server returned a status
     *             code other than 200
     */
    public HttpClientResponse restGetFile(LocalizationContext context,
            String filename) throws CommunicationException {
        File outputFile = this.adapter.getPath(context, filename);
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        String url = buildRestAddress(context, filename, false);
        HttpGet request = new HttpGet(url);
        DownloadFileStreamHandler streamHandler = new DownloadFileStreamHandler(
                outputFile);
        HttpClientResponse resp = HttpClient.getInstance().executeRequest(
                request, streamHandler);
        return resp;
    }

    /**
     * Sends a PUT request to the localization REST service to upload a file.
     * 
     * @param lfile
     * @param fileToUpload
     * @return
     * @throws CommunicationException
     */
    public FileUpdatedMessage restPutFile(LocalizationFile lfile,
            File fileToUpload) throws LocalizationException,
            CommunicationException {
        String url = buildRestAddress(lfile.getContext(), lfile.getName(),
                false);
        HttpPut request = new HttpPut(url);
        request.setEntity(new FileEntity(fileToUpload));

        // add the checksum of the version we modified
        request.addHeader(IF_MATCH, lfile.getCheckSum());

        // add the checksum of the new contents
        request.addHeader(CONTENT_MD5,
                ChecksumIO.getFileChecksum(fileToUpload, false));

        // send the put request
        HttpClientResponse resp = HttpClient.getInstance().executeRequest(
                request);

        if (resp.code != 200) {
            String msg = new String(resp.data);
            switch (resp.code) {
            case 403:
                throw new LocalizationPermissionDeniedException(msg);
            case 409:
                throw new LocalizationFileVersionConflictException(msg);
            default:
                throw new LocalizationException("Error code " + resp.code
                        + ": " + msg);
            }
        }

        // build a FileUpdatedMessage
        FileUpdatedMessage fum = new FileUpdatedMessage();
        fum.setContext(lfile.getContext());
        fum.setFileName(lfile.getPath());
        if (ILocalizationFile.NON_EXISTENT_CHECKSUM.equals(lfile.getCheckSum())) {
            fum.setChangeType(FileChangeType.ADDED);
        } else {
            fum.setChangeType(FileChangeType.UPDATED);
        }
        Date time;
        try {
            time = TIME_HEADER_FORMAT.get().parse(
                    resp.headers.get(LAST_MODIFIED));
        } catch (ParseException e) {
            throw new LocalizationException(
                    "Error parsing last modified header from response: "
                            + resp.headers.get(LAST_MODIFIED), e);
        }
        fum.setTimeStamp(time.getTime());
        fum.setCheckSum(resp.headers.get(CONTENT_MD5));

        return fum;
    }
}
