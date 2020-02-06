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
package com.raytheon.uf.common.python;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import jep.Jep;
import jep.JepException;

/**
 * Methods for working with the pycache and compiled Python files
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 12, 2019 7917       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class PyCacheUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PyCacheUtil.class);

    /** Name of the pycache directory, where compiled Python files are stored */
    public static final String PYCACHE_DIR = "__pycache__";

    /**
     * File extension for compiled Python files. There is only one such
     * extension now that .pyo is not used anymore.
     */
    public static final String COMPILED_FILE_EXTENSION = ".pyc";

    private static String cacheTag;

    private static Pattern compiledFilePattern;

    static {
        try (Jep jep = new Jep()) {
            jep.eval("import sys");
            cacheTag = Objects
                    .toString(jep.getValue("sys.implementation.cache_tag"));
            if ("null".equals(cacheTag)) {
                statusHandler.warn("Python cache tag is null. "
                        + COMPILED_FILE_EXTENSION + " files will be ignored");
            } else {
                String compiledFileRegex = "(.+?)[.]" + Pattern.quote(cacheTag)
                        + "(?:[.]opt-[12])?"
                        + Pattern.quote(COMPILED_FILE_EXTENSION);
                compiledFilePattern = Pattern.compile(compiledFileRegex);
            }
        } catch (JepException e) {
            statusHandler.error("Failed to get the Python cache tag", e);
        }
    }

    private PyCacheUtil() {
        // static methods only
    }

    /**
     * @param sourceFile
     *            Name of a Python source file
     * @return All possible names of compiled Python files for the specified
     *         Python source file name. These are just the file name only, with
     *         no other path components.
     */
    private static String[] getPyCacheFileNames(String sourceFile) {
        if (!sourceFile.endsWith(".py")) {
            throw new IllegalArgumentException(
                    sourceFile + " is not a .py file");
        }
        if (cacheTag == null) {
            return new String[0];
        }
        String nameWithoutExt = sourceFile.substring(0,
                sourceFile.length() - 3);
        List<String> compiledFileNames = new ArrayList<>();

        // Files compiled without optimization turned on
        String compiledFileName = nameWithoutExt + "." + cacheTag
                + COMPILED_FILE_EXTENSION;
        compiledFileNames.add(compiledFileName);

        /// Files compiled with optimization level 1 or 2
        String opt1CompiledFileName = nameWithoutExt + "." + cacheTag + ".opt-1"
                + COMPILED_FILE_EXTENSION;
        String opt2CompiledFileName = nameWithoutExt + "." + cacheTag + ".opt-2"
                + COMPILED_FILE_EXTENSION;
        compiledFileNames.add(opt1CompiledFileName);
        compiledFileNames.add(opt2CompiledFileName);

        return compiledFileNames.toArray(new String[0]);
    }

    /**
     * Delete compiled Python files corresponding to the specified Python source
     * file, if they exist.
     *
     * @param sourceFilePath
     *            Path to the Python source file
     * @throws IOException
     *             If any of the compiled Python files exists but fails to
     *             delete
     */
    public static void clean(Path sourceFilePath) throws IOException {
        if (cacheTag == null) {
            return;
        }
        Path realPath = sourceFilePath.toRealPath();
        Path parentDir = realPath.getParent();
        Path cacheDir = parentDir.resolve(PYCACHE_DIR);
        String baseName = realPath.getFileName().toString();
        List<Path> compiledFilePaths = new ArrayList<>();
        for (String compiledFileName : getPyCacheFileNames(baseName)) {
            Path compiledFilePath = cacheDir.resolve(compiledFileName);
            compiledFilePaths.add(compiledFilePath);
        }
        List<String> failedFiles = new ArrayList<>();
        for (Path p : compiledFilePaths) {
            try {
                Files.delete(p);
            } catch (NoSuchFileException e) {
                // ignore
            } catch (IOException e) {
                statusHandler.warn("Failed to delete pycache file " + p, e);
                failedFiles.add(p.toString());
            }
        }
        try {
            if (Files.list(parentDir).count() == 0) {
                Files.delete(parentDir);
            }
        } catch (IOException e) {
            statusHandler.warn(e.getLocalizedMessage(), e);
        }
        if (!failedFiles.isEmpty()) {
            throw new IOException(
                    "Failed to delete the following pycache files: "
                            + String.join(",", failedFiles));
        }
    }

    /**
     * Delete compiled Python files corresponding to the specified Python source
     * file, if they exist. This deletes the files from the localization server
     * and from the local file system. If you just want to delete local copies,
     * use {@link #clean(Path)} instead
     *
     * @param sourceFile
     *            Path to the Python source file
     * @throws LocalizationException
     *             If any of the compiled Python files exists but fails to
     *             delete
     */
    public static void clean(ILocalizationFile sourceFile)
            throws LocalizationException {
        if (cacheTag == null) {
            return;
        }
        String baseName = LocalizationUtil.extractName(sourceFile.getPath());
        String sourceFilePath = sourceFile.getPath();
        String parentDirPath = LocalizationUtil.getParent(sourceFilePath);
        String cacheDirPath = LocalizationUtil.join(parentDirPath, PYCACHE_DIR);
        LocalizationContext ctx = sourceFile.getContext();
        IPathManager pathManager = PathManagerFactory.getPathManager();

        for (String compiledFileName : getPyCacheFileNames(baseName)) {
            String deletePath = LocalizationUtil.join(cacheDirPath,
                    compiledFileName);
            ILocalizationFile lfile = pathManager.getLocalizationFile(ctx,
                    deletePath);
            List<String> failedFiles = new ArrayList<>();
            if (lfile != null) {
                try {
                    lfile.delete();
                } catch (LocalizationException e) {
                    statusHandler.warn("Failed to delete pycache file " + lfile,
                            e);
                    failedFiles.add(lfile.toString());
                }
            }
            if (!failedFiles.isEmpty()) {
                throw new LocalizationException(
                        "Failed to delete the following pycache files: "
                                + String.join(",", failedFiles));
            }
        }
    }

    /**
     * @return true if the path points to a compiled Python file inside a
     *         pycache directory. Only the path string is checked, no checking
     *         is done on the actual file on the file system (it may not even
     *         exist). If caching is disabled or the cache tag could not be
     *         determined, this always returns false.
     */
    public static boolean isPyCacheFile(Path path) {
        return cacheTag != null && getBaseName(path).isPresent();
    }

    private static Optional<String> getBaseName(Path compiledFilePath) {
        if (cacheTag == null || !PYCACHE_DIR.equals(
                compiledFilePath.getParent().getFileName().toString())) {
            return Optional.empty();
        }
        Matcher m = compiledFilePattern
                .matcher(compiledFilePath.getFileName().toString());
        if (m.matches()) {
            return Optional.of(m.group(1));
        }
        return Optional.empty();
    }

    /**
     * @param compiledFilePath
     *            Path to compiled Python file
     * @return Path to the corresponding Python source file (that may or may not
     *         actually exist)
     */
    public static Path getSourceFilePath(Path compiledFilePath) {
        Optional<String> baseName = getBaseName(compiledFilePath);
        if (!baseName.isPresent()) {
            throw new IllegalArgumentException(
                    compiledFilePath + " is not a pycache file name");
        }
        Path pycacheFilePath = compiledFilePath.getParent();
        Path sourceDirPath = pycacheFilePath.getParent();
        return sourceDirPath.resolve(baseName.get() + ".py");

    }
}
