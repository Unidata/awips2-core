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

package com.raytheon.uf.common.localization;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Checksum Utility
 * 
 * This class produces a checksum string from a file or input stream. The
 * checksum can be used to verify the validity of a file.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 19, 2007            chammack    Initial Creation.
 * Nov 17, 2015  4834      njensen     Moved CHECKSUM_FILE_EXTENSION here
 *                                      Support returning non-existent and directory checksums
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class Checksum {

    public static final String CHECKSUM_FILE_EXTENSION = ".md5";

    /**
     * Disallow instantiation
     */
    private Checksum() {

    }

    /**
     * Create an MD5 checksum from an input stream
     * 
     * @param is
     *            the input stream to checksum
     * @return the md5 checksum
     * @throws IOException
     *             if checksumming failed
     */
    public static String getMD5Checksum(InputStream is) throws IOException {
        String result;
        try {
            byte[] b = createChecksum(is);
            result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16)
                        .substring(1);
            }
        } catch (IOException e) {
            throw new IOException("Error generating checksum", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating checksum", e);
        }
        return result;
    }

    /**
     * Create an MD5 checksum from a file
     * 
     * @param file
     *            the input file to checksum
     * @return the md5 checksum
     * @throws IOException
     *             if checksumming failed
     */
    public static String getMD5Checksum(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                return ILocalizationFile.DIRECTORY_CHECKSUM;
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    return getMD5Checksum(fis);
                }
            }
        } else {
            return ILocalizationFile.NON_EXISTENT_CHECKSUM;
        }
    }

    private static byte[] createChecksum(InputStream is) throws IOException,
            NoSuchAlgorithmException {
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = is.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        return complete.digest();

    }

}
