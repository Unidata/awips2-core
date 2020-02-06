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
package com.raytheon.uf.viz.core.sounds;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Utility class for playing sounds.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 24, 2014 2636       mpduff      Initial creation
 * Apr 29, 2019 7591       tgurney     Replace sun.audio with
 *                                     javax.sound.sampled. Stop catching
 *                                     exceptions
 *
 * </pre>
 *
 * @author mpduff
 */

public class SoundUtil {

    private SoundUtil() {
        // static methods only
    }

    /**
     * Play a sound from the file at the provided path. If the filename is null
     * or empty, no sound will be played
     *
     * @param filename
     *            The filename path
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws LineUnavailableException
     */
    public static void playSound(String filename) throws IOException,
            UnsupportedAudioFileException, LineUnavailableException {
        if (filename == null || filename.isEmpty()) {
            return;
        }
        File soundFile = new File(filename);
        try (AudioInputStream as = AudioSystem.getAudioInputStream(soundFile)) {
            @SuppressWarnings("resource")
            Clip clip = AudioSystem.getClip();
            clip.open(as);
            clip.addLineListener(e -> {
                if (e.getType().equals(LineEvent.Type.STOP)) {
                    clip.close();
                }
            });
            clip.start();
        }
    }
}
