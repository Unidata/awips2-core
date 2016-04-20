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
package com.raytheon.uf.edex.distribution;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container of regular expressions, both original strings and the compiled
 * patterns. Used by the DistributionSrv bean to store regex patterns for
 * plugins. It is important to note that no validation is done on duplicate
 * regex patterns.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 20, 2009            brockwoo     Initial creation
 * May 16, 2011 7317       cjeanbap     Added try-catch statement
 *                                      for PatternSyntaxException.
 * Mar 19, 2013 1794       djohnson     Add toString() for debugging.
 * Sep 10, 2013 2327       rjpeter      Sized ArrayList declarations.
 * Nov 21, 2013 2541       bgonzale     Exclusion patterns.
 * May 09, 2014 3151       bclement     added noPossibleMatch() removed ISerializableObject
 * Dec 11, 2015 5166       kbisanz      Update logging to use SLF4J
 * Apr 19, 2016 5450       nabowle      Add plugin attribute.
 * </pre>
 * 
 * @author brockwoo
 * @version 1.0
 */

@XmlRootElement(name = "requestPatterns")
@XmlAccessorType(XmlAccessType.NONE)
public class RequestPatterns {

    /**
     * List of patterns requested by a plugin.
     */
    @XmlElements({ @XmlElement(name = "regex", type = String.class) })
    private List<String> patterns = new ArrayList<String>(0);

    /**
     * List of patterns excluded by a plugin. Excludes takes precedence over
     * acceptance and is applied first.
     */
    @XmlElements({ @XmlElement(name = "regexExclude", type = String.class) })
    private List<String> exclusionPatterns = new ArrayList<String>(0);

    /** The plugin name to provide patterns, if different than the filename. */
    @XmlAttribute(required = false)
    private String plugin;

    private List<Pattern> compiledPatterns = new ArrayList<Pattern>(0);

    private List<Pattern> compiledExclusionPatterns = new ArrayList<Pattern>(0);

    protected Logger patternFailedLogger = LoggerFactory
            .getLogger("PatternFailedLog");

    /**
     * Creates a new instance of the container.
     */
    public RequestPatterns() {
    }

    /**
     * Returns a list of the stored patterns as a series of strings.
     *
     * @return a list of regex pattern strings
     */
    public List<String> getPatterns() {
        return patterns;
    }

    /**
     * Sets the list of regex strings for this container.
     *
     * @param patterns
     *            an arraylist of regex strings
     */
    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    /**
     * Inserts a single string into the list.
     *
     * @param pattern
     *            The regex string to insert
     */
    public void setPattern(String pattern) {
        this.patterns.add(pattern);
    }

    /**
     * Get the list of regex patterns to exclude.
     *
     * @return the exclusionPatterns
     */
    public List<String> getExclusionPatterns() {
        return exclusionPatterns;
    }

    /**
     * Set the list of exclusion regex patterns.
     *
     * @param exclusionPatterns
     *            the exclusionPatterns to set
     */
    public void setExclusionPatterns(List<String> exclusionPatterns) {
        this.exclusionPatterns = exclusionPatterns;
    }

    /**
     * Inserts a single string into the list.
     *
     * @param exclusionPatterns
     *            The regex string to insert
     */
    public void setExclusionPatterns(String exclusionPattern) {
        this.exclusionPatterns.add(exclusionPattern);
    }

    /**
     * Will compile the strings into Pattern objects.
     *
     */
    public void compilePatterns() {
        compiledPatterns = compilePatterns(patterns);
        compiledExclusionPatterns = compilePatterns(exclusionPatterns);
    }

    private List<Pattern> compilePatterns(List<String> patterns) {
        List<Pattern> compiledPatterns = new ArrayList<Pattern>(patterns.size());

        for (String pattern : patterns) {
            try {
                compiledPatterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to compile pattern: ");
                sb.append(pattern).append(".");
                patternFailedLogger.error(sb.toString(), e);
            }
        }
        return compiledPatterns;
    }

    /**
     * Takes a string and compares against the patterns in this container. The
     * first one that matches breaks the search and returns true.
     *
     * Check for exclusion first. It takes precedence over acceptance.
     *
     * @param header
     *            The string to search for
     * @return a boolean indicating success
     */
    public boolean isDesiredHeader(String header) {
        boolean isFound = false;
        boolean isExcluded = false;

        for (Pattern headerPattern : compiledExclusionPatterns) {
            if (headerPattern.matcher(header).find()) {
                isExcluded = true;
                break;
            }
        }
        if (!isExcluded) {
            for (Pattern headerPattern : compiledPatterns) {
                if (headerPattern.matcher(header).find()) {
                    isFound = true;
                    break;
                }
            }
        }
        return isFound;
    }

    @Override
    public String toString() {
        return patterns.toString();
    }

    /**
     * @return true if {@link #isDesiredHeader(String)} can't return true
     */
    public boolean noPossibleMatch() {
        return compiledPatterns.isEmpty();
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }
}
