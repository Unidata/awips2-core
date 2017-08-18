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
package com.raytheon.uf.viz.localization.perspective.search.ui.result;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.viz.localization.perspective.search.ui.LocalizationResourceSelectionDialog;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.PathData;

/**
 * Provide labels for {@link LocalizationFileEntryData} objects in the status
 * area of the {@link LocalizationResourceSelectionDialog}
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 17, 2017  6359     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class ResourceSelectionDetailsLabelProvider
        extends LSRBaseLabelProvider {

    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData data = (LocalizationFileEntryData) element;
            ILocalizationFile file = data.getFile();
            PathData pathData = data.getPathData();
            IPath filePath = Path.forPosix(file.getPath());
            IPath dataPath = Path.forPosix(pathData.getPath());
            if (dataPath.isAbsolute() && dataPath.segmentCount() != 0) {
                dataPath = dataPath.makeRelative();
            }
            IPath path = Path.forPosix(pathData.getApplication());
            path = path.append(pathData.getName());
            if (dataPath.segmentCount() == 0) {
                path = path.append(filePath);
            } else {
                path = path.append(filePath.makeRelativeTo(dataPath));
            }
            path = path.removeLastSegments(0);

            return new StyledString(path.toString());
        } else if (element == null) {
            return new StyledString();
        }
        return new StyledString(element.toString());
    }

    @Override
    public Image getImage(Object element) {
        /*
         * This is relying on the behavior of super to return a directory
         * looking icon for anything it doesn't recognize as a file.
         */
        return super.getImage(null);

    }

}
