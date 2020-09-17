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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeEntryData;
import com.raytheon.uf.viz.localization.perspective.view.FileTreeView;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileGroupData;

/**
 *
 * Base {@link IStyledLabelProvider} for {@link LocalizationSearchResultPage}.
 * This contains any functionality which is shared between the label providers
 * of the tree and list views.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Apr 06, 2017  6188     bsteffen  Initial creation
 * Aug 17, 2017  6359     bsteffen  Remove unused page field.
 * Sep 17, 2020  8230     randerso  Fix NPE in getImageDescriptor. Issue was
 *                                  exposed by change of Eclipse supplied image
 *                                  changing from gif to png.
 *
 * </pre>
 *
 * @author bsteffen
 */
public abstract class LSRBaseLabelProvider extends LabelProvider
        implements ILabelProvider, IStyledLabelProvider {

    /*
     * Defined in org.eclipse.search plugin.xml but the id is not exported
     * publicly to java.
     */
    private static final String HIGHLIGHT_BG_COLOR_NAME = "org.eclipse.search.ui.match.highlight";

    protected static final Styler HIGHLIGHT_STYLE = StyledString
            .createColorRegistryStyler(null, HIGHLIGHT_BG_COLOR_NAME);

    protected final Map<ImageDescriptor, Image> imageMap = new HashMap<>();

    public LSRBaseLabelProvider() {

    }

    /**
     * Get the user friendly label that is used to identify the context of a
     * {@link LocalizationFileEntryData}. For example this will produce labels
     * like "BASE", "USER (bsteffen)" or "USER (bsteffen - common_static)".
     */
    protected String getContextLabel(LocalizationFileEntryData entryData) {
        ILocalizationFile file = entryData.getFile();
        LocalizationContext ctx = file.getContext();
        LocalizationLevel level = ctx.getLocalizationLevel();

        StringBuilder nameBuilder = new StringBuilder(level.toString());
        if (entryData.isMultipleTypes() || level != LocalizationLevel.BASE) {
            nameBuilder.append(" (");
            if (level != LocalizationLevel.BASE) {
                nameBuilder.append(ctx.getContextName());
            }
            if (entryData.isMultipleTypes()) {
                if (level != LocalizationLevel.BASE) {
                    nameBuilder.append(" - ");
                }
                nameBuilder
                        .append(ctx.getLocalizationType().name().toLowerCase());
            }
            nameBuilder.append(")");
        }
        return nameBuilder.toString();
    }

    @Override
    public String getText(Object element) {
        return getStyledText(element).getString();
    }

    @Override
    public Image getImage(Object element) {
        ImageDescriptor desc = getImageDescriptor(element);
        if (desc == null) {
            return null;
        }
        Image image = imageMap.get(desc);
        if (image == null) {
            image = desc.createImage();
            imageMap.put(desc, image);
        }
        return image;

    }

    public ImageDescriptor getImageDescriptor(Object element) {
        if (element instanceof LocalizationFileEntryData
                || element instanceof LocalizationFileGroupData) {
            String path = ((FileTreeEntryData) element).getPath();
            ImageDescriptor desc = LocalizationEditorUtils.getEditorRegistry()
                    .getImageDescriptor(path);
            return desc;
        } else if (element instanceof Match) {
            Bundle bundle = FrameworkUtil
                    .getBundle(AbstractTextSearchViewPage.class);
            Path path = Path.forPosix("icons/full/obj16/line_match.png");
            return getImageDescriptor(bundle, path);
        } else {
            Bundle bundle = FrameworkUtil.getBundle(FileTreeView.class);
            Path path = Path.forPosix("icons/directory.gif");
            return getImageDescriptor(bundle, path);
        }
    }

    private static ImageDescriptor getImageDescriptor(Bundle bundle,
            IPath path) {
        URL url = FileLocator.find(bundle, path, null);
        if (url == null) {
            url = FileLocator.find(bundle, new Path("..").append(path), null);
        }
        return ImageDescriptor.createFromURL(url);
    }

    @Override
    public void dispose() {
        for (Image image : imageMap.values()) {
            image.dispose();
        }
        imageMap.clear();
    }

}
