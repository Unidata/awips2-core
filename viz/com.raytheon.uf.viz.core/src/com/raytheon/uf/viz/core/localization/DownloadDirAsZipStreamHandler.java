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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient.IStreamHandler;
import com.raytheon.uf.common.util.SizeUtil;

/**
 * IStreamHandler that expects a zip file in the InputStream and extracts it to
 * an output directory.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 16, 2015  3978      njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class DownloadDirAsZipStreamHandler implements IStreamHandler {

    protected static final int BUFFER_SIZE = (int) (8 * SizeUtil.BYTES_PER_KB);

    protected File localDir;

    protected boolean readOnly;

    public DownloadDirAsZipStreamHandler(File localDir, boolean readOnly) {
        this.localDir = localDir;
        this.readOnly = readOnly;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.comm.HttpClient.IStreamHandler#handleStream(java
     * .io.InputStream)
     */
    @Override
    public void handleStream(InputStream is) throws CommunicationException {
        try {
            unzipStreaming(localDir.getPath(), is);
        } catch (IOException e) {
            throw new CommunicationException(
                    "Error handling stream of directory to local filesystem", e);
        }
    }

    /**
     * Unzips the input stream to the specified output path.
     * 
     * @param outputPath
     * @param is
     * @throws IOException
     */
    protected void unzipStreaming(String outputPath, InputStream is)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        try (ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                String filename = ze.getName();
                File file = new File(outputPath + File.separator + filename);
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (ze.isDirectory()) {
                    file.mkdir();
                } else {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        int bytesRead = 0;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                long lastMod = ze.getTime();
                if (lastMod > -1) {
                    /*
                     * TODO Java 7's zip support only has time to a 2 second
                     * resolution instead of a millisecond resolution. Java 8
                     * and Apache Commons Codec have support to get a more
                     * precise time.
                     */
                    file.setLastModified(lastMod);
                }

                if (readOnly && !file.isDirectory()) {
                    file.setReadOnly();
                }

                zis.closeEntry();
                ze = zis.getNextEntry();
            }
        }
    }

}
