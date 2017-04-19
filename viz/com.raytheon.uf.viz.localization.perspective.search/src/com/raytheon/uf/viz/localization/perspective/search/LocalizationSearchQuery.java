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
package com.raytheon.uf.viz.localization.perspective.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.CharBuffer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.eclipse.search.ui.text.Match;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * This class searches localization files for a specific search term and can
 * provide a {@link ISearchResult} containing all the {@link Match}s.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 06, 2017  6188     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class LocalizationSearchQuery implements ISearchQuery {

    private final LocalizationSearchFileProvider fileProvider;

    private final String searchTerm;

    private final boolean caseSensitive;

    private final LocalizationSearchResult result;

    public LocalizationSearchQuery(LocalizationSearchFileProvider fileProvider,
            String searchTerm, boolean caseSensitive) {
        this.fileProvider = fileProvider;
        this.searchTerm = searchTerm;
        this.caseSensitive = caseSensitive;
        this.result = new LocalizationSearchResult(this);
    }

    @Override
    public IStatus run(IProgressMonitor monitor)
            throws OperationCanceledException {
        SubMonitor masterMonitor = SubMonitor.convert(monitor, 100);
        masterMonitor.setTaskName("Searching for '" + searchTerm + "' ...");
        result.removeAll();
        fileProvider.run(masterMonitor.split(10));
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        query(masterMonitor.split(90));
        if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

    /**
     * Actually search through all the files. The fileProvider must be populated
     * before calling this method.
     */
    private void query(IProgressMonitor monitor) {
        monitor.beginTask("Searching for '" + searchTerm + "' ...",
                fileProvider.getFileCount());
        int worked = 1;
        for (PathData pathData : fileProvider.getPaths()) {
            for (ILocalizationFile file : fileProvider.getFiles(pathData)) {
                if (monitor.isCanceled()) {
                    return;
                }
                Path path = Path.forPosix(file.getPath());
                monitor.subTask("Scanning file " + worked + "  of "
                        + fileProvider.getFileCount() + ":"
                        + path.lastSegment());
                query(pathData, file);
                monitor.worked(1);
                worked += 1;
            }
        }
    }

    /**
     * Actually look in a single localization file.
     */
    private void query(PathData pathData, ILocalizationFile file) {
        String searchTerm = this.searchTerm;
        if (!caseSensitive) {
            searchTerm = searchTerm.toLowerCase();
        }
        char firstChar = searchTerm.charAt(0);
        /* buffer used to track current position in partial matches. */
        CharBuffer searchBuffer = CharBuffer.wrap(searchTerm);

        int lineCount = 1;
        int offset = 0;

        /*
         * For most reasonable lines the lineBuffer will contain the entire
         * line. If the line is longer than the buffer then the buffer will only
         * contain some of the characters around the match. The exact number of
         * characters kept in the buffer before and after the match is inexact,
         * it will be at least lineBufferMargin but it may be more. The buffer
         * management is optimized to avoid keeping the entire line in memory
         * and to reduce the amount of copying.
         */
        int lineBufferLength = Math.max(1000, searchTerm.length() * 4);
        int lineBufferMargin = lineBufferLength / 4;
        CharBuffer lineBuffer = CharBuffer.allocate(lineBufferLength);

        try (InputStream in = file.openInputStream();
                Reader inReader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(inReader)) {
            /*
             * This is a naive search algorithm. It checks every position in the
             * reader to determine if the search term matches that position. For
             * matches or partial matches it reads ahead to check the match so
             * it must backtrack and resume at the next position after testing a
             * match.
             * 
             * The search is implemented as a few loops, The main loop reads
             * every character in the file and determines if it matches the
             * first search character. This loop also maintains the lineBuffer,
             * lineCount, and offset. When the first character matches then a
             * second loop will compare the rest of the search term. When the
             * second loop finds a match there is a third loop to read in the
             * rest of a line to gather context for the search result. After the
             * second loop (and possibly the third loop) are done then the main
             * loop resets the reader and resumes the search.
             */
            int c = reader.read();
            while (c != -1) {
                if (!caseSensitive) {
                    c = Character.toLowerCase(c);
                }

                if (firstChar == c) {
                    /*
                     * Potential match, mark the reader and the buffer so that
                     * after testing the match it is simple to reset and resume
                     * searching from the current location.
                     */
                    reader.mark(lineBuffer.remaining());
                    lineBuffer.mark();
                    int lineOffset = lineBuffer.position();
                    searchBuffer.rewind();

                    /*
                     * Loop over remaining characters to check if this is an
                     * actual match.
                     */
                    while (c != -1) {
                        lineBuffer.put((char) c);
                        if (!caseSensitive) {
                            c = Character.toLowerCase(c);
                        }
                        if (c != searchBuffer.get()) {
                            break;
                        }
                        if (searchBuffer.remaining() == 0) {
                            /* This is a match */

                            /*
                             * Loop to load the entire line, or as much as fits
                             * in the buffer, so that the user can see the
                             * context of the match.
                             */
                            while (lineBuffer.remaining() > 0) {
                                c = reader.read();
                                if (c == -1 || c == '\n') {
                                    break;
                                }
                                lineBuffer.put((char) c);
                            }
                            String line = new String(lineBuffer.array(), 0,
                                    lineBuffer.position());
                            Match match = new LocalizationSearchMatch(pathData,
                                    line, file, offset, lineCount, lineOffset,
                                    searchTerm.length());
                            result.addMatch(match);
                            break;
                        }
                        c = reader.read();

                    }

                    /*
                     * Reset the state to resume searching for new matches.
                     */
                    reader.reset();
                    lineBuffer.reset();
                    c = firstChar;
                }

                lineBuffer.put((char) c);
                if (c == '\n') {
                    lineCount += 1;
                    lineBuffer.position(0);
                } else if (lineBuffer.remaining() < lineBufferMargin) {
                    /* Really long line, cut off the beginning. */
                    CharBuffer tmp = lineBuffer.duplicate();
                    tmp.flip();
                    tmp.position(tmp.limit() - lineBufferMargin);
                    lineBuffer.position(0);
                    lineBuffer.put(tmp);
                }

                c = reader.read();
                offset += 1;
            }
        } catch (IOException | LocalizationException e) {
            UFStatus.getHandler(LocalizationSearchQuery.class)
                    .error("Error searching in " + file.getPath(), e);
        }
    }

    @Override
    public String getLabel() {
        return "Localization Query for '" + searchTerm + "'";
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public LocalizationSearchFileProvider getFileProvider() {
        return fileProvider;
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        return result;
    }

    @Override
    public String toString() {
        return "LocalizationSearchQuery [ searchTerm=" + searchTerm
                + ", caseSensitive=" + caseSensitive + ", fileProvider="
                + fileProvider + "]";
    }

}
