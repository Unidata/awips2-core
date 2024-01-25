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
package com.raytheon.uf.common.serialization.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Generates a python class and module that is equivalent to the Java class
 * passed in as an argument. This is meant to be used with DynamicSerialize to
 * generate the python side. Technically you could create the python object
 * definitions at runtime based on the decoding of the
 * SelfDescribingBinaryProtocol, but that could get messy.
 *
 * Usage: Run this tool as a java main() from within Eclipse. You should give it
 * two command line arguments: -d outputDirectory, typically should be an
 * absolute path to the pythonPackages/dynamicserialize/dstypes directory. -f
 * filename, a filename to use as input, where the file has the fully-qualified
 * classnames of the classes you want to generate python dynamicserialize
 * classes for. Each line of this file should have one FQN of a class.
 *
 * Double, short, and byte fields will be wrapped using numpy float64, int16,
 * and int8 (respectively) unless the --no-wrap argument is provided. This
 * affects both primitive and boxed types.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 14, 2010            njensen     Initial creation
 * Jul 31, 2012  #965      dgilling    Fix path to file header.
 * Jul 24, 2014   3185     njensen     Improved javadoc
 * Aug 19, 2014   3393     nabowle     Added numpy wrappers, software header.
 * Sep 23, 2019   7920     tgurney     Update for Python 3
 * </pre>
 *
 * @author njensen
 */

public class PythonFileGenerator {

    private static final String NEW_LINE = "\n";

    private static final String INDENT = "    ";

    private static final String COMMENT = "# ";

    private static final String INIT_FILE = "__init__.py";

    private static final Map<Class<?>, String> WRAPPERS;

    static {
        WRAPPERS = new HashMap<>();
        WRAPPERS.put(double.class, "numpy.float64");
        WRAPPERS.put(short.class, "numpy.int16");
        WRAPPERS.put(byte.class, "numpy.int8");
        WRAPPERS.put(Double.class, WRAPPERS.get(double.class));
        WRAPPERS.put(Short.class, WRAPPERS.get(short.class));
        WRAPPERS.put(Byte.class, WRAPPERS.get(byte.class));
    }

    public static void generateFile(File destDir, Class<?> clz, String header,
            boolean wrap) throws IOException {
        String name = clz.getName();
        String shortname = name.substring(name.lastIndexOf('.') + 1);
        System.out.println(shortname);
        if (name.contains("$")) {
            System.out.println("Inner classes not supported");
            System.exit(0);
        }

        File parentFile = destDir;

        String[] packages = name.split("[.]");

        for (int i = 0; i < packages.length - 1; ++i) {
            File packageFile = new File(parentFile, packages[i]);
            if (packageFile.exists() && !packageFile.isDirectory()) {
                packageFile.delete();
            }
            packageFile.mkdir();

            createInitFile(header, parentFile);

            parentFile = packageFile;
        }

        try (FileWriter fw = new FileWriter(
                new File(parentFile, shortname + ".py"), false)) {
            fw.write(header);
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write(
                    "File auto-generated against equivalent DynamicSerialize Java class");
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write("     SOFTWARE HISTORY");
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write(
                    "    Date            Ticket#       Engineer       Description");
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write(
                    "    ------------    ----------    -----------    --------------------------");
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write("    ");
            fw.write(new SimpleDateFormat("MMM dd, yyyy").format(new Date()));
            fw.write("                  ");
            String username = System.getProperty("user.name");
            String namespace = "              ";
            if (username.length() > namespace.length()) {
                // long-named users will have to fill it out themselves
                username = "";
            }
            fw.write(username);
            fw.write(namespace.substring(username.length()));
            fw.write(" Generated");
            fw.write(NEW_LINE);
            fw.write(NEW_LINE);

            Map<String, Class<?>> fields = getSerializedFields(clz);
            // only import numpy if used.
            if (wrap) {
                for (Class<?> clazz : fields.values()) {
                    if (WRAPPERS.containsKey(clazz)) {
                        fw.write("import numpy");
                        fw.write(NEW_LINE);
                        fw.write(NEW_LINE);
                        break;
                    }
                }
            }

            fw.write("class " + shortname + "(object):");
            fw.write(NEW_LINE);
            fw.write(NEW_LINE);

            fw.write(INDENT);
            fw.write("def __init__(self):");
            fw.write(NEW_LINE);
            for (String s : fields.keySet()) {
                fw.write(INDENT);
                fw.write(INDENT);
                fw.write("self.");
                fw.write(s);
                fw.write(" = None");
                fw.write(NEW_LINE);
            }

            fw.write(NEW_LINE);
            String s;
            Class<?> fieldClass;
            String title;
            for (Entry<String, Class<?>> entry : fields.entrySet()) {
                s = entry.getKey();
                fieldClass = entry.getValue();
                title = s.substring(0, 1).toUpperCase() + s.substring(1);
                fw.write(INDENT);
                fw.write("def get");
                fw.write(title);
                fw.write("(self):");
                fw.write(NEW_LINE);
                fw.write(INDENT);
                fw.write(INDENT);
                fw.write("return self.");
                fw.write(s);
                fw.write(NEW_LINE);
                fw.write(NEW_LINE);

                fw.write(INDENT);
                fw.write("def set");
                fw.write(title);
                fw.write("(self, ");
                fw.write(s);
                fw.write("):");
                fw.write(NEW_LINE);
                fw.write(INDENT);
                fw.write(INDENT);
                fw.write("self.");
                fw.write(s);
                fw.write(" = ");
                if (wrap && WRAPPERS.containsKey(fieldClass)) {
                    fw.write(WRAPPERS.get(fieldClass));
                    fw.write("(");
                    fw.write(s);
                    fw.write(")");
                } else {
                    fw.write(s);
                }
                fw.write(NEW_LINE);
                fw.write(NEW_LINE);
            }

            fw.flush();
        }

        createInitFile(header, parentFile);
    }

    private static void createInitFile(String header, File dir)
            throws IOException {
        File initFile = new File(dir, INIT_FILE);
        initFile.delete();
        try (FileWriter fw = new FileWriter(initFile, false)) {
            fw.write(header);
            fw.write(NEW_LINE);
            fw.write(COMMENT);
            fw.write("File auto-generated by PythonFileGenerator");
            fw.write(NEW_LINE);
            fw.write(NEW_LINE);
            fw.write("__all__ = [");
            fw.write(NEW_LINE);

            File[] files = dir.listFiles();
            List<String> dirs = new ArrayList<>();
            List<String> pythonFiles = new ArrayList<>();
            // add all packages, then files
            for (int i = 0; i < 2; ++i) {
                for (File file : files) {
                    String fileName = file.getName();
                    if ("__pycache__".equals(fileName)) {
                        continue;
                    }
                    if (i == 0 && file.isDirectory() && !file.isHidden()) {
                        dirs.add(fileName);
                    } else if (i == 1 && fileName.endsWith(".py")
                            && !fileName.equals(INIT_FILE)) {
                        dirs.add(fileName.substring(0, fileName.length() - 3));
                        pythonFiles.add(
                                fileName.substring(0, fileName.length() - 3));
                    }
                }
            }

            Collections.sort(dirs);
            Collections.sort(pythonFiles);

            for (int j = 0; j < dirs.size(); ++j) {
                fw.write(INDENT + INDENT + INDENT);
                fw.write("'");
                fw.write(dirs.get(j));
                fw.write("'");
                if (j < dirs.size() - 1) {
                    fw.write(",");
                }
                fw.write(NEW_LINE);
            }

            fw.write("          ]");
            fw.write(NEW_LINE);
            fw.write(NEW_LINE);

            for (String pythonFile : pythonFiles) {
                fw.write("from ." + pythonFile + " import " + pythonFile);
                fw.write(NEW_LINE);
            }
            fw.write(NEW_LINE);

            fw.flush();
        }
    }

    public static Map<String, Class<?>> getSerializedFields(Class<?> clz) {
        Map<String, Class<?>> map = new HashMap<>();
        while (clz != null) {
            Field[] fields = clz.getDeclaredFields();
            for (Field f : fields) {
                Object ann = f.getAnnotation(DynamicSerializeElement.class);
                if (ann != null) {
                    map.put(f.getName(), f.getType());
                }
            }
            clz = clz.getSuperclass();
        }

        return map;
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String fileToRead = null;
        String destDir = null;
        boolean wrap = true;

        for (int i = 0; i < args.length; ++i) {
            if ("-f".equals(args[i]) && i < args.length - 1) {
                i++;
                fileToRead = args[i];
            } else if ("-d".equals(args[i]) && i < args.length - 1) {
                i++;
                destDir = args[i];
            } else if ("--no-wrap".equals(args[i])) {
                wrap = false;
            }
        }

        if (fileToRead == null) {
            System.err.println(
                    "Pass in file to read classes in from using -f <filepath> argument");
            System.exit(1);
        }

        File readFile = new File(fileToRead);
        File destFile = null;

        if (destDir == null) {
            System.out.println(
                    "No destination directory specified, specify with -d <dir> argument");

            File tmp = File.createTempFile("tmp", "");
            destFile = new File(tmp.getParentFile(), "python");
            tmp.delete();

            if (destFile.exists() && !destFile.isDirectory()) {
                destFile.delete();
            }
            destFile.mkdirs();
        } else {
            destFile = new File(destDir);
            if (destFile.exists() && !destFile.isDirectory()) {
                System.err.println(
                        "Can not write to destination directory, it is already a file");
                System.exit(2);
            }
            destFile.mkdirs();

        }

        System.out.println(
                "Reading class list file: " + readFile.getAbsolutePath());
        System.out.println(
                "Writing python file to " + destFile.getAbsolutePath());

        try (FileReader fr = new FileReader(readFile);
                BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Class<?> c = null;
                try {
                    c = Class.forName(line);
                } catch (ClassNotFoundException e1) {
                    e1.printStackTrace();
                    System.out.println("Class not found: " + line);
                    continue;
                }

                String header = null;
                try {
                    header = getHeaderInfo();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(0);
                }

                try {
                    generateFile(destFile, c, header, wrap);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
        }

        System.out.println("Classes generated");
    }

    public static String getHeaderInfo() throws IOException {
        File file = new File(
                "../../../AWIPS2_Dev_Baseline/cave/build/tools/headup/AWIPS/awipsHeader.txt");
        if (!file.exists()) {
            System.out.println(
                    "Unable to determine header information, skipping header");
            return "";
        }

        try (FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            sb.append("##" + NEW_LINE);
            String line = br.readLine();
            while (line != null) {
                sb.append(COMMENT);
                sb.append(line);
                sb.append(NEW_LINE);
                line = br.readLine();
            }
            sb.append("##" + NEW_LINE);
            return sb.toString();
        }

    }
}
