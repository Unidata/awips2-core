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
package com.raytheon.viz.core.gl;

import java.nio.IntBuffer;

import com.jogamp.opengl.GL2;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.core.gl.internal.GLTarget;

/**
 * Holds the results of several queries into GL to determine what level of
 * capabilities are supported.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jun 06, 2011           mschenke    Initial creation
 * Apr 08, 2014  2950     bsteffen    Add max texture size.
 * May 07, 2015           mjames      Refactor GL for jogamp 2
 * Oct 25, 2017  6387     bsteffen    Use IUFStatusHandler instead of System.out
 *                                    and disable shaders when float textures
 *                                    aren't available.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class GLCapabilities {

    private static GLCapabilities caps = null;

    public static synchronized GLCapabilities getInstance(GL2 gl) {
        if (caps == null) {
            caps = new GLCapabilities(gl);
        }
        return caps;
    }

    /** Does the video card support high end features */
    public boolean cardSupportsHighEndFeatures = false;

    /** Does the video card support shaders */
    public boolean cardSupportsShaders = false;

    public final int maxTextureSize;

    private GLCapabilities(GL2 gl) {
        IUFStatusHandler logger = UFStatus.getHandler(GLCapabilities.class);
        String openGlVersion = gl.glGetString(GL2.GL_VERSION);
        float glVersion = Float.parseFloat(openGlVersion.substring(0, 3));

        if (glVersion >= 1.4f) {
            logger.debug("Enabling high end GL features");
            cardSupportsHighEndFeatures = true;

        }
        boolean imagingAvailable = gl.isExtensionAvailable("GL_ARB_imaging");
        logger.debug("Imaging is available: " + imagingAvailable);

        if (glVersion >= 2.0f && !GLTarget.FORCE_NO_SHADER) {
            boolean floatAvailable = gl
                    .isExtensionAvailable("GL_ARB_texture_float");
            if (!floatAvailable) {
                logger.info(
                        "Shader disabled because support for float textures is missing");
            } else {
                cardSupportsShaders = true;
            }
        }
        logger.debug("Shader supported: " + cardSupportsShaders);

        IntBuffer ib = IntBuffer.allocate(1);
        gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, ib);
        ib.rewind();
        maxTextureSize = ib.get();
    }
}
