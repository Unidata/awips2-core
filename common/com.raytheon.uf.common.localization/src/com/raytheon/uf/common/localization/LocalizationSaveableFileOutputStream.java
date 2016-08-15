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

import java.io.IOException;

import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * A SaveableOutputStream combined with FileOutputStream for use when the
 * localization store is the local filesystem and remote utility/localization
 * service.
 * 
 * In general this class exists solely to try and maintain clean API.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 18, 2015  3806      njensen     Initial creation
 * Nov 12, 2015  4834      njensen     Removed LocalizationOpFailedException
 * Aug 15, 2016  5834      njensen     Added flush() call to save()
 *
 * </pre>
 * 
 * @author njensen
 */

public class LocalizationSaveableFileOutputStream extends SaveableOutputStream {

    private final LocalizationFileOutputStream stream;

    public LocalizationSaveableFileOutputStream(
            LocalizationFileOutputStream outputStream) {
        this.stream = outputStream;
    }

    @Override
    public void flush() throws IOException {
        stream.flush();
    }

    @Override
    public void write(int b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        stream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        stream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }

    @Override
    public void save() throws IOException {
        try {
            this.flush();
            stream.closeAndSave();
        } catch (LocalizationException e) {
            throw new IOException("Error saving output stream", e);
        }
    }

}
