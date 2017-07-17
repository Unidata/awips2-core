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
package com.raytheon.uf.edex.localization.http.writer.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;

/**
 * Localization response writer that generates XML directory listings.
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
public class XmlDirectoryListingWriter implements IDirectoryListingWriter {

    public static final MimeType CONTENT_TYPE = new MimeType("application/xml");

    public static final MimeType ALT_CONTENT_TYPE = new MimeType("text/xml");

    @Override
    public boolean generates(MimeType contentType) {
        return CONTENT_TYPE.equalsIgnoreParams(contentType)
                || ALT_CONTENT_TYPE.equalsIgnoreParams(contentType);
    }

    private final JAXBManager jaxbManager;

    public XmlDirectoryListingWriter() throws JAXBException {
        jaxbManager = new JAXBManager(LocalizationFilesXml.class,
                DirectoryXml.class);
    }

    @Override
    public void write(MimeType contentType, List<String> entries,
            OutputStream out) throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: "
                            + contentType);
        }
        DirectoryXml result = new DirectoryXml(entries);
        try {
            jaxbManager.marshalToStream(result, out);
        } catch (SerializationException e) {
            throw new IOException("Failed to write xml.", e);
        }
    }

    @Override
    public void write(HttpServletRequest request, MimeType contentType,
            LocalizationContext context, String path, OutputStream out)
            throws IOException {
        int depth = 1;
        String depthStr = request.getParameter("depth");
        if (depthStr != null) {
            depth = Integer.parseInt(depthStr);
        }
        LocalizationFilesXml result = convert(context, path, depth);
        try {
            jaxbManager.marshalToStream(result, out);
        } catch (SerializationException e) {
            throw new IOException("Failed to write xml for " + path, e);
        }
    }

    private LocalizationFilesXml convert(LocalizationContext context,
            String path, int depth) {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        ILocalizationFile[] files = pathManager.listFiles(context, path, null,
                false, false);
        Arrays.sort(files, Comparator.comparing(ILocalizationFile::getPath));
        LocalizationFilesXml result = new LocalizationFilesXml();
        for (ILocalizationFile file : files) {
            if (depth > 1 && file.isDirectory()) {
                LocalizationFilesXml tmp = convert(context, file.getPath(),
                        depth - 1);
                LocalizationFileXml xml = new LocalizationFileXml(file);
                xml.setFiles(tmp.getFiles());
                result.add(xml);
            } else {
                result.add(file);
            }
        }
        return result;
    }

}
