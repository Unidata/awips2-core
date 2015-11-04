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
package com.raytheon.uf.viz.core.point.svg;

import java.io.File;

import org.apache.batik.util.ParsedURL;
import org.apache.batik.util.ParsedURLData;
import org.apache.batik.util.ParsedURLDefaultProtocolHandler;
import org.apache.batik.util.ParsedURLProtocolHandler;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;

/**
 * An SVG file may include fonts and other resources that are loaded from
 * separate SVG files. When an SVG file is loaded through the
 * {@link IPathManager} API then any other separate files may not be available
 * because the PathManager is not notified that it should download the files.
 * This class implements a custom {@link ParsedURLProtocolHandler} which can be
 * injected into the batik to intercept any url loading and ensure that the file
 * content has been downloaded from the PathManager.
 * 
 * Before loading an SVG file from an {@link ILocalizationFile} the
 * {@link #register()} method should be called to ensure that an instance of
 * this class has been injected into batik. It is safe to call
 * {@link #register()} multiple times.
 * 
 * TODO This class relies on the specific IPathManager implementation that is
 * storing cached file contents on disk and it does not take into account
 * localization file locks. To guarantee safety of the file contents a custom
 * {@link ParsedURLData} class needs to be implemented which uses the streaming
 * API from a specific ILocalizationFile. Rather than hacking this functionality
 * into the file protocol it should be implemented as a new protocol with a
 * custom handler.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 26, 2010           mschenke  Initial creation
 * Oct 27, 2015  4798     bsteffen  Move to core.point
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class LocalizationParsedURLHandler extends
        ParsedURLDefaultProtocolHandler {

    private IPathManager pathManager;

    public LocalizationParsedURLHandler() {
        super("file");
        pathManager = PathManagerFactory.getPathManager();
    }

    @Override
    public ParsedURLData parseURL(String urlStr) {
        ParsedURLData data = null;
        if (urlStr != null && urlStr.startsWith("file:") == false
                && urlStr.startsWith("#") == false
                && urlStr.startsWith(IPathManager.SEPARATOR) == false) { // Win32
            String name = urlStr;
            String endName = "";
            int idx = name.indexOf("#");
            if (idx > -1) {
                endName = name.substring(idx);
                name = name.substring(0, idx);
            }
            File file = pathManager
                    .getStaticFile(SVGImageFactory.PLOT_MODEL_DIR
                            + IPathManager.SEPARATOR
                    + name);
            if (file != null) {
                // Win32: Change to convert both Linux and Win32 paths
                // Win32 path -> URL needs separator changed and "/" pre-pended
                String absPath = file.getAbsolutePath();
                absPath = absPath.replace(File.separator,
                        IPathManager.SEPARATOR);
                if (absPath.startsWith(IPathManager.SEPARATOR) == false)
                    absPath = IPathManager.SEPARATOR + absPath;
                data = super.parseURL("file:" + absPath + endName);
            }
        }

        if (data == null) {
            data = super.parseURL(urlStr);
        }
        return data;
    }

    @Override
    public ParsedURLData parseURL(ParsedURL baseURL, String urlStr) {
        ParsedURLData data = null;
        if (urlStr != null && urlStr.startsWith("file:") == false
                && urlStr.startsWith("#") == false
                && urlStr.startsWith(IPathManager.SEPARATOR) == false) { // Win32
            String name = urlStr;
            String endName = "";
            int idx = name.indexOf("#");
            if (idx > -1) {
                endName = name.substring(idx);
                name = name.substring(0, idx);
            }
            File file = pathManager
                    .getStaticFile(SVGImageFactory.PLOT_MODEL_DIR
                            + IPathManager.SEPARATOR
                    + name);
            if (file != null) {
                // Win32: Change to convert both Linux and Win32 paths
                // Win32 path -> URL needs separator changed and "/" pre-pended
                String absPath = file.getAbsolutePath();
                absPath = absPath.replace(File.separator,
                        IPathManager.SEPARATOR);
                if (absPath.startsWith(IPathManager.SEPARATOR) == false)
                    absPath = IPathManager.SEPARATOR + absPath;
                data = super.parseURL("file:" + absPath + endName);
            }
        }
        if (data == null) {
            data = super.parseURL(baseURL, urlStr);
        }
        return data;
    }

    private static boolean registered = false;

    public static synchronized void register() {
        if (!registered) {
            ParsedURL.registerHandler(new LocalizationParsedURLHandler());
            registered = true;
        }
    }
}
