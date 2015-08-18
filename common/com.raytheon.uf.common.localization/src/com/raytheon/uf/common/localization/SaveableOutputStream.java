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
import java.io.OutputStream;

/**
 * An OutputStream that allows content to be saved. For example, subclass
 * implementations of save() could send a file to a server, commit it to version
 * control, write the contents to a destination, rename a temp file to the real
 * file name, etc.
 * 
 * The save operation can conceptually be thought of as a quasi-transactional
 * commit; once you have finished writing the contents, you MUST save/commit the
 * output. However, it is not guaranteed to be transactional. The implementation
 * of the subclass determines how transactional a save() call is and if the
 * output can be rolled back, discarded, etc on errors.
 * 
 * For further information about why close() does not auto-save and you must
 * explicitly call save() to commit the content, please view the comments at the
 * bottom of the source code of this class.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 18, 2015  3806       njensen    Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public abstract class SaveableOutputStream extends OutputStream {

    /**
     * Saves the content of the output stream. If the save method is not called
     * then any output in the stream may be lost when it is closed. Similar to a
     * closed stream, a SaveableOutputStream cannot perform output operations
     * after save() is called. The stream must be closed immediately after
     * saving.
     * 
     * When implementing this method it may be necessary to close or flush the
     * stream before saving. If an implementation needs to close during save,
     * then it must support calling close repeatedly without adverse effects.
     * This behavior allows the stream to work nicely with the
     * {@link AutoCloseable} interface.
     */
    public abstract void save() throws IOException;

    /**
     * Developer Notes:
     * 
     * It has been brought up repeatedly that close() should auto-save, then we
     * have no need for save() or this class. Great idea, the one flaw is the
     * stream may not know about an exception that came from a wrapping stream.
     * For example,
     * 
     * <pre>
     * try(OutputStream os = localizationFile.openOutputStream()){
     *     JAXB.marshal(new Object(), os);
     * }
     * </pre>
     * 
     * If JAXB throws the error, the stream wouldn't know about it, and
     * therefore auto-close would save invalid contents and a bad file. This
     * then led in turn to the idea of a TransactionalOutputStream where close()
     * would auto-save, but there would be a method of rollback() that could be
     * used in catch blocks to handle this scenario. Unfortunately, that does
     * not compile cleanly.
     * 
     * Here are some examples:
     * 
     * <pre>
        // conceptual clean rollback refuses to compile
        try (TransactionalOutputStream sos = localizationFile
                .openOutputStream()) {
            sos.write(fileContents);
        } catch (Exception e) {
            sos.rollback(); // this line does not compile, variable sos is unresolved
            logger.error("Failed to write localization file: " + fileName + ".", e);
        }
        
        // conceptual unclean variation refuses to compile
        TransactionalOutputStream sos1 = null;
        try (sos1 = localizationFile  // this line does not compile, sos1 needs declaration of type
                .openOutputStream()) {
            sos1.write(fileContents);
        } catch (Exception e) {
            if (sos1 != null) {
                sos1.rollback();
            }
            logger.error("Failed to write localization file: " + fileName + ".", e);
        }
        
        // conceptual unclean variation refuses to compile
        TransactionalOutputStream sos2 = null;
        try (TransactionalOutputStream sos2 = localizationFile // this line does not compile, sos2 is declared twice
                .openOutputStream()) {            
            sos2.write(fileContents);
        } catch (Exception e) {
            if (sos2 != null) {
                sos2.rollback();
            }
            logger.error("Failed to write localization file: " + fileName + ".", e);
        }

        // conceptual unclean second reference does compile but is dangerous
        TransactionalOutputStream sosRef = null;
        try (TransactionalOutputStream sos = localizationFile
                .openOutputStream()) {
            sosRef = sos;
            sos.write(fileContents);
        } catch (Exception e) {
            if (sosRef != null) {
                sosRef.rollback();
            }
            logger.error("Failed to write localization file: " + fileName + ".", e);
        }
        
        // double try blocks does compile but is less clean than save()
        try(TransactionalOutputStream tos = localizationFile
                .openOutputStream()) {
            try {
                tos.write(fileContents);
            } catch(Exception e) {
                tos.rollback();
                logger.error("Failed to write localization file: " + fileName + ".", e);
            }
        }     
     * </pre>
     */

}
