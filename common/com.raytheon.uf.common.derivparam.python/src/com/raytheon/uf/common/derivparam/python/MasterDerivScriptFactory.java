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
package com.raytheon.uf.common.derivparam.python;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.derivparam.library.DerivedParameterGenerator;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.python.concurrent.PythonInterpreterFactory;
import com.raytheon.uf.common.util.FileUtil;

import jep.JepException;

/**
 * Factory for creating and initializing MasterDerivScript.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 04, 2013 2041       bsteffen    Initial creation
 * Aug 26, 2013 2289       bsteffen    Make number of deriv param threads
 *                                     configurable.
 * Dec 14, 2015 4816       dgilling    Support refactored PythonJobCoordinator API.
 * Jun 17, 2016 5439       bsteffen    use pathManager within DerivParamImporter
 * Aug 13. 2019 7880       tgurney     Python 3 fixes
 *
 * </pre>
 *
 * @author bsteffen
 */
public class MasterDerivScriptFactory
        implements PythonInterpreterFactory<MasterDerivScript> {

    private static final String INTERFACE_SCRIPT = DerivedParameterGenerator.DERIV_PARAM_DIR
            + File.separator + "python" + File.separator
            + "DerivParamImporter.py";

    @Override
    public MasterDerivScript createPythonScript() throws JepException {
        List<String> preEvals = new ArrayList<>(2);
        IPathManager pm = PathManagerFactory.getPathManager();

        try {
            ILocalizationFile importInterface = pm
                    .getStaticLocalizationFile(INTERFACE_SCRIPT);
            String script = readLocalizationFile(importInterface);
            /*
             * Eval will only execute one command but the script has several
             * commands so work around it by execing an eval of a multiline
             * string.
             */
            script = "exec(\"\"\"" + script + "\"\"\", globals(), globals())";
            preEvals.add(script);
            preEvals.add("sys.meta_path.append(DerivParamImporter())");
        } catch (IOException | LocalizationException e) {
            throw new JepException("Error setting up python environment.", e);
        }
        return new MasterDerivScript(null,
                MasterDerivScript.class.getClassLoader(), preEvals);

    }

    /**
     * Read the entire contents of a localization file into a String.
     *
     * @param file
     *            the file to read
     * @return the contents of the file
     * @throws IOException
     * @throws LocalizationException
     */
    public static String readLocalizationFile(ILocalizationFile file)
            throws IOException, LocalizationException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(8192);
        try (InputStream stream = file.openInputStream()) {
            FileUtil.copy(stream, bos);
        }
        return new String(bos.toByteArray());
    }

}
