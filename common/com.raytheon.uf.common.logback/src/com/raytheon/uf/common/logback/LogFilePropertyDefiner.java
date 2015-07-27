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
package com.raytheon.uf.common.logback;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.PropertyDefiner;
import ch.qos.logback.core.util.OptionHelper;

/**
 * A logback property for getting a fully qualified log file. The directory
 * property should be the absolute path to the directory containing the log
 * file. The name property should be the log file's name. The directory and/or
 * name value may contain the template %PID%. This will be replaced with Process
 * ID when getting the property's value. Limited validation checks are performed
 * on the directory and name values.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 17, 2015 4148       rferrel     Initial creation
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */

public class LogFilePropertyDefiner extends ContextAwareBase implements
        PropertyDefiner {

    private String directory;

    private String name;

    private String value;

    private boolean error = false;

    @Override
    public String getPropertyValue() {
        if (error) {
            return null;
        }

        if (value == null) {
            error = false;

            if (OptionHelper.isEmpty(directory)) {
                addError("The \"directory\" property must be set.");
                error = true;
            }
            if (OptionHelper.isEmpty(name)) {
                addError("The \"name\" property must be set.");
                error = true;
            }
            if (error) {
                return null;
            }
            name = LogbackUtil.replacePid(name);
            directory = LogbackUtil.replacePid(directory);
            Path p = FileSystems.getDefault().getPath(directory, name);
            value = p.normalize().toAbsolutePath().toString();
        }
        return value;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setName(String name) {
        this.name = name;
    }
}
