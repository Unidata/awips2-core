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
package com.raytheon.uf.common.localization.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * Accept or reject localization files based on glob-style pattern matching.
 * This is NOT regex; only the * is used and it means "zero or more of any
 * character". A file is accepted by the filter if it matches one or more of the
 * accept patterns and none of the reject patterns.
 *
 * FILE FORMAT: The file format is one pattern per line. It only contains level
 * and path, as you cannot match against context name. You may use the glob
 * character (*) in place of the level, type, or any part of the path. Comments
 * start with '#' and are only allowed at the start of a line. Example line:
 *
 * SITE:common_static/textdb/textCategoryClass.txt
 *
 * Example with glob:
 *
 * SITE:common_static/afos2awips/*
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 28, 2016 5937       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class LocalizationFileFilter {

    private List<Pattern> acceptPatterns = new ArrayList<>();

    private List<Pattern> rejectPatterns = new ArrayList<>();

    /** Patterns must match this format */
    private final Pattern patternFormat;

    public LocalizationFileFilter() {
        patternFormat = buildPatternFormat();
    }

    /**
     * @return Dynamically-created pattern used to validate filter lists. Each
     *         non-comment or non-empty line must match this pattern. Includes
     *         all localization levels and types that are available at the time
     *         this method is called.
     */
    private Pattern buildPatternFormat() {
        List<String> allowedLevels = Arrays.stream(LocalizationLevel.values())
                .map(LocalizationLevel::toString).map(String::toUpperCase)
                .collect(Collectors.toList());
        List<String> allowedTypes = Arrays.stream(LocalizationType.values())
                .map(LocalizationType::toString).map(String::toLowerCase)
                .collect(Collectors.toList());
        String levelsPattern = String.join("|", allowedLevels) + "|\\*";
        String typesPattern = String.join("|", allowedTypes) + "|\\*";
        return Pattern.compile(
                String.format("^(%s):(%s)(.*)?$", levelsPattern, typesPattern));
    }

    /**
     * Append the patterns from the provided input stream to the accept list.
     * One pattern per line
     *
     * @param is
     * @throws IOException
     *             If there was a problem with the stream
     * @throws LocalizationException
     *             If any pattern is invalid
     */
    public void addAcceptList(InputStream is)
            throws IOException, LocalizationException {
        acceptPatterns.addAll(parsePatternList(is));
    }

    /**
     * Append the patterns from the provided input stream to the reject list.
     * One pattern per line
     *
     * @param is
     * @throws IOException
     *             If there was a problem with the stream
     * @throws LocalizationException
     *             If any pattern is invalid
     */
    public void addRejectList(InputStream is)
            throws IOException, LocalizationException {
        rejectPatterns.addAll(parsePatternList(is));
    }

    /**
     * This format is taken directly from the protectedFiles.txt format.
     * Example: SITE:common_static/textdb/textCategoryClass.txt
     *
     * @param file
     * @return String containing localization file level and path, formatted for
     *         matching against filter lists. Note that this does not include
     *         the context name.
     */
    private String getFormattedPath(ILocalizationFile file) {
        return file.getContext().getLocalizationLevel().toString().toUpperCase()
                + ":" + file.getContext().getLocalizationType().toString()
                        .toLowerCase()
                + "/" + file.getPath();
    }

    /**
     * @param file
     * @return true if the file is accepted, false if it is not. The file is
     *         accepted if it matches one or more of the patterns in the accept
     *         list and none of the patterns in the reject list.
     */
    public boolean accept(ILocalizationFile file) {
        String filePath = getFormattedPath(file);
        boolean rval = false;
        for (Pattern p : acceptPatterns) {
            if (p.matcher(filePath).matches()) {
                rval = true;
                break;
            }
        }
        if (rval) {
            for (Pattern p : rejectPatterns) {
                if (p.matcher(filePath).matches()) {
                    rval = false;
                    break;
                }
            }
        }
        return rval;
    }

    private Pattern parsePattern(String line) throws LocalizationException {
        if (!patternFormat.matcher(line).matches()) {
            String msg = String.format(
                    "Pattern is not in the form LEVEL:type/path: \"%s\"", line);
            throw new LocalizationException(msg);
        }
        /*
         * Need \Q and \E to escape most of the string so that regex
         * metacharacters will not be interpreted.
         */
        String newLine = "\\Q"
                + line.replace("//", "/").replace("*", "\\E.*\\Q") + "\\E";
        try {
            return Pattern.compile(newLine);
        } catch (PatternSyntaxException e) {
            String msg = String.format("Glob pattern is invalid: \"%s\"", line);
            throw new LocalizationException(msg, e);
        }
    }

    private List<Pattern> parsePatternList(InputStream is)
            throws IOException, LocalizationException {
        List<Pattern> patterns = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(is))) {
            String line = null;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo += 1;
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    try {
                        patterns.add(parsePattern(line));
                    } catch (LocalizationException e) {
                        throw new LocalizationException(
                                "Failed to parse pattern at line " + lineNo, e);
                    }
                }
            }
        }
        return patterns;
    }

}
