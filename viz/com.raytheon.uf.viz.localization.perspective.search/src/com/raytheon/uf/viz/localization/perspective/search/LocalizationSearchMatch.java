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

import org.eclipse.search.ui.text.Match;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * A {@link Match} for a {@link LocalizationSearchResult} that provides all the
 * information necessary to uniquely identify a search result and display it to
 * the user. The element of the match is the {@link ILocalizationFile} that was
 * searched. Additionally this provides the PathData containing the file and the
 * line containing the result for display in the results view.
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
public class LocalizationSearchMatch extends Match {

    private final PathData pathData;

    private final String line;

    private final int lineNumber;

    private final int lineOffset;

    public LocalizationSearchMatch(PathData pathData, String line,
            ILocalizationFile file, int offset, int lineNumber, int lineOffset,
            int length) {
        super(file, offset, length);
        this.pathData = pathData;
        this.line = line;
        this.lineNumber = lineNumber;
        this.lineOffset = lineOffset;
    }

    public PathData getPathData() {
        return pathData;
    }

    public String getLine() {
        return line;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getLineOffset() {
        return lineOffset;
    }

    /**
     * Since the constructor guarantees that the element is an
     * {@link ILocalizationFile} this method is overriden to return an
     * ILocalizationFIle to save callers from needing a cast.
     */
    @Override
    public ILocalizationFile getElement() {
        return (ILocalizationFile) super.getElement();
    }

    @Override
    public String toString() {
        return "LocalizationSearchMatch [file=" + getElement() + ", lineNumber="
                + lineNumber + ", lineOffset=" + lineOffset + "]";
    }

}
