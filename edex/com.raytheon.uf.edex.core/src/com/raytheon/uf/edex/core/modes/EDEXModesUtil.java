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
package com.raytheon.uf.edex.core.modes;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.props.PropertiesFactory;

/**
 * EDEX utility class for accessing mode and mode configuration files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 5, 2013  2566       bgonzale     Initial creation.  Refactored from Executor.
 * May 21,2014  3195       bclement     changes to merge multiple modes files
 * 
 * </pre>
 * 
 * @author bgonzale
 * @version 1.0
 */

public class EDEXModesUtil {

    public static final String XML = ".xml";

    public static final Pattern XML_PATTERN = Pattern.compile("\\" + XML);

    public static final String RES_SPRING = "res/spring/";

    public static final Pattern RES_SPRING_PATTERN = Pattern
            .compile("res/spring/");

    public static final String CONF_DIR = EDEXUtil.EDEX_HOME + File.separator
            + "conf";

    public static final String MODES_DIR = CONF_DIR + File.separator + "modes";

    /**
     * Populates files with a list of files that match in the specified
     * directory
     * 
     * Returns a list of plugins, etc
     * 
     * @param jarDir
     * @param files
     * @return
     * @throws IOException
     * @throws ModesException
     */
    public static List<String> extractSpringXmlFiles(List<String> files,
            String modeName) throws IOException, ModesException {
        FilenameFilter filter = getModeFilter(modeName);
        String pluginDirStr = PropertiesFactory.getInstance()
                .getEnvProperties().getEnvValue("PLUGINDIR");

        List<String> retVal = new ArrayList<String>();
        File jarDirFile = new File(pluginDirStr);
        File[] jars = jarDirFile.listFiles();

        List<JarFile> jarList = new ArrayList<JarFile>(jars.length);
        for (File p : jars) {
            if (p.getName().endsWith(".jar")) {
                JarFile jar = new JarFile(p);
                jarList.add(jar);
            }
        }

        for (JarFile jar : jarList) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String name = e.getName();
                if (filter.accept(null, name)) {
                    files.add(name);
                    retVal.add(RES_SPRING_PATTERN.matcher(
                            XML_PATTERN.matcher(name).replaceAll(""))
                            .replaceAll(""));
                }
            }

        }

        return retVal;
    }

    /**
     * Get filename filter for mode.
     * 
     * @param modeName
     * @return
     * @throws IOException
     * @throws ModesException
     *             if mode is not found or is not bootable
     */
    private static FilenameFilter getModeFilter(String modeName)
            throws IOException, ModesException {
        Map<String, EdexMode> mergedModes = getMergedModes();
        EdexMode edexMode = mergedModes.get(modeName);

        if (edexMode == null) {
            throw new ModesException(
                    "No EDEX run configuration specified in modes files for "
                            + modeName + ". " + getModesList(mergedModes));
        } else if (edexMode.isTemplate()) {
            throw new ModesException(modeName
                    + " is a template mode, and is not bootable. "
                    + getModesList(mergedModes));
        }

        if (!edexMode.isInited()) {
            edexMode.init(mergedModes);
        }
        return edexMode;
    }

    /**
     * @see #getModesList(Map)
     * @see #getMergedModes()
     * @return
     * @throws ModesException
     * @throws IOException
     */
    public static String getModesList() throws ModesException, IOException {
        return getModesList(getMergedModes());
    }

    /**
     * Get formatted string of bootable modes in map
     * 
     * @param modes
     * @return
     */
    public static String getModesList(Map<String, EdexMode> modes) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Modes: ");
        for (Entry<String, EdexMode> e : modes.entrySet()) {
            EdexMode mode = e.getValue();
            if (!mode.isTemplate()) {
                sb.append("'").append(mode.getName()).append("' ");
            }
        }
        return sb.toString();
    }

    /**
     * Get a map of modes that are aggregates of all the modes files in the
     * modes directory
     * 
     * @return
     * @throws ModesException
     * @throws IOException
     */
    private static Map<String, EdexMode> getMergedModes()
            throws ModesException, IOException {
        File modesDir = new File(MODES_DIR);
        File[] modesFiles = modesDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(XML);
            }
        });

        Unmarshaller msh = null;
        try {
            JAXBContext jaxbContext = JAXBContext
                    .newInstance(EdexModesContainer.class);
            msh = jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new ModesException("Problem initializing modes JAXB context",
                    e);
        }
        Map<String, EdexMode> rval = new HashMap<String, EdexMode>();
        for (File modeFile : modesFiles) {
            EdexModesContainer container = readSingleModesFile(modeFile, msh);
            for (EdexMode mode : container.getModes()) {
                EdexMode aggregate = rval.get(mode.getName());
                if (aggregate == null) {
                    aggregate = new EdexMode();
                    aggregate.setName(mode.getName());
                    rval.put(mode.getName(), aggregate);
                }
                aggregate.merge(mode);
            }
        }
        return rval;
    }

    /**
     * Read and parse an individual modes file
     * 
     * @param modesFile
     * @param msh
     * @return
     * @throws ModesException
     * @throws IOException
     */
    private static EdexModesContainer readSingleModesFile(File modesFile,
            Unmarshaller msh) throws ModesException, IOException {
        FileReader reader = null;
        EdexModesContainer rval = null;
        try {
            reader = new FileReader(modesFile);
            rval = (EdexModesContainer) msh.unmarshal(reader);
        } catch (Exception e) {
            throw new ModesException("Unable to read modes file: "
                    + modesFile.getAbsolutePath(), e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return rval;
    }

}
