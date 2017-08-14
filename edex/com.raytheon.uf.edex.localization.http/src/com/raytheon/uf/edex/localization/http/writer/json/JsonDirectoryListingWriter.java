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
package com.raytheon.uf.edex.localization.http.writer.json;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;

/**
 * Localization response writer that generates JSON directory listings.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jul 17, 2017  5731     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class JsonDirectoryListingWriter implements IDirectoryListingWriter {

    public static final MimeType CONTENT_TYPE = new MimeType(
            "application/json");

    public static final MimeType ALT_CONTENT_TYPE = new MimeType("text/json");

    private final ObjectMapper mapper;

    public JsonDirectoryListingWriter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setDateFormat(new ISO8601DateFormat());
        mapper.setSerializationInclusion(Include.NON_NULL);
        this.mapper = mapper;
    }

    @Override
    public boolean generates(MimeType contentType) {
        return contentType.accept(CONTENT_TYPE)
                || contentType.accept(ALT_CONTENT_TYPE);
    }

    @Override
    public void write(MimeType contentType, List<String> entries,
            OutputStream out) throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: "
                            + contentType);
        }
        /*
         * Mapping to an empty dict makes it possible to add additional fields
         * in the future without breaking compatibility.
         */
        Map<String, Map<?, ?>> result = entries.stream()
                .collect(Collectors.toMap(e -> e, e -> Collections.emptyMap()));
        mapper.writer().writeValue(out, result);

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
        Map<String, LocalizationFileJson> result = convert(context, path,
                depth);
        mapper.writer().writeValue(out, result);
    }

    private Map<String, LocalizationFileJson> convert(
            LocalizationContext context, String path, int depth) {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        ILocalizationFile[] files = pathManager.listFiles(context, path, null,
                false, false);
        Arrays.sort(files,
                Comparator.comparing(IDirectoryListingWriter::getBaseName));
        Map<String, LocalizationFileJson> result = new HashMap<>();
        for (ILocalizationFile file : files) {
            LocalizationFileJson json = new LocalizationFileJson(file);
            if (depth > 1 && file.isDirectory()) {
                json.setChildren(convert(context, file.getPath(), depth - 1));
            }
            result.put(IDirectoryListingWriter.getBaseName(file), json);
        }
        return result;
    }

}
