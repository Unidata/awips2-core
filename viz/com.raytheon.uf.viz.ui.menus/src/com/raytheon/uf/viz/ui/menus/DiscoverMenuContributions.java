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
package com.raytheon.uf.viz.ui.menus;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.AbstractContributionFactory;
import org.eclipse.ui.menus.IContributionRoot;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.menus.MenuSerialization;
import com.raytheon.uf.common.menus.xml.CommonIncludeMenuItem;
import com.raytheon.uf.common.menus.xml.CommonMenuContributionFile;
import com.raytheon.uf.common.menus.xml.VariableSubstitution;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.ui.menus.xml.IncludeMenuItem;

/**
 * Discover the menu contributions present in localization.
 *
 * This will check several locations:
 * <UL>
 * <LI>The plugin localization directory
 * <LI>The cave static directory
 * <LI>The user and site localization directory
 * </UL>
 *
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 12, 2009  2214     chammack  Initial creation
 * Apr 27, 2012  562      dgilling  Ensure call to MenuCreationJob uses proper
 *                                  method to retrieve localized site.
 * Mar 20, 2013  1638     mschenke  Removed menu creation job use
 * May 04, 2015  4284     bsteffen  Copy subMenuId
 * Dec 10, 2015  5193     bsteffen  Eclipse 4 Upgrade: Workaround 48143
 * Jan 15, 2016  5242     kbisanz   Replaced LocalizationFile with
 *                                  ILocalizationFile
 * Jan 28, 2016  5294     bsteffen  Substitute when combining substitutions
 * Dec 06, 2017  6355     nabowle   Observe path changes to re-discover menu
 *                                  contributions.
 * Sep 10, 2020  8213     bsteffen  Fix visibleWhen for Eclipse 4.16
 * Oct 12, 2020  8241     randerso  Updated for Eclipse 4.17
 * 
 * </pre>
 *
 * @author chammack
 */

public class DiscoverMenuContributions {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DiscoverMenuContributions.class);

    private static boolean ran = false;

    private static List<AbstractContributionFactory> factories = new ArrayList<>();

    private static MenuPathObserver observer;

    public static final Schema schema;

    static {
        Schema sch = null;
        try {
            URL url = FileLocator.find(Activator.getDefault().getBundle(),
                    new Path("menus.xsd"), null);
            if (url == null) {
                statusHandler.handle(Priority.CRITICAL,
                        "Unable to load menu schema, menus will not operate properly");
            }
            url = FileLocator.resolve(url);
            InputStream is = (InputStream) url.getContent();
            StreamSource ss = new StreamSource(is);
            sch = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(ss);
        } catch (Exception e1) {
            statusHandler.handle(Priority.CRITICAL,
                    "Error reading menu schema, menus will not operate properly",
                    e1);
        }
        schema = sch;
    }

    public static void discoverContributions() {
        discoverContributions("menus");
    }

    public static synchronized void discoverContributions(
            final String... menus) {
        if (ran) {
            return;
        }

        ran = true;

        if (observer == null) {
            observer = new MenuPathObserver();
            for (String menu : menus) {
                PathManagerFactory.getPathManager()
                        .addLocalizationPathObserver(menu, observer);
            }
        }

        ILocalizationFile[] file = null;

        if (menus.length == 1) {
            file = PathManagerFactory.getPathManager().listStaticFiles(menus[0],
                    new String[] { "index.xml" }, true, true);
        } else {
            List<ILocalizationFile> fileList = new ArrayList<>();

            for (String menu : menus) {
                ILocalizationFile[] files = PathManagerFactory.getPathManager()
                        .listStaticFiles(menu, new String[] { "index.xml" },
                                true, true);
                for (ILocalizationFile lf : files) {
                    fileList.add(lf);
                }
            }
            file = new ILocalizationFile[fileList.size()];
            fileList.toArray(file);
        }

        Unmarshaller um = null;
        try {
            JAXBContext c = MenuSerialization.getJaxbContext();
            um = c.createUnmarshaller();
            um.setSchema(schema);
        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error creating jaxb context", e);
            return;
        }

        List<AbstractContributionFactory> newFactories = new ArrayList<>();
        IMenuService menuService = PlatformUI.getWorkbench()
                .getService(IMenuService.class);
        for (ILocalizationFile lf : file) {
            try (InputStream is = lf.openInputStream()) {
                final CommonMenuContributionFile mcf = (CommonMenuContributionFile) um
                        .unmarshal(is);
                if (mcf.contribution != null) {
                    for (final CommonIncludeMenuItem im : mcf.contribution) {
                        final IncludeMenuItem imc = new IncludeMenuItem();
                        imc.fileName = im.fileName;
                        imc.installationLocation = im.installationLocation;
                        imc.removals = im.removals;
                        imc.subMenuName = im.subMenuName;
                        imc.subMenuId = im.subMenuId;
                        imc.substitutions = VariableSubstitution
                                .substituteAndCombine(mcf.substitutions,
                                        im.substitutions);
                        imc.visibleOnActionSet = im.visibleOnActionSet;
                        AbstractContributionFactory viewMenuAddition = new AbstractContributionFactory(
                                imc.installationLocation, Activator.PLUGIN_ID) {
                            @SuppressWarnings("restriction")
                            @Override
                            public void createContributionItems(
                                    IServiceLocator serviceLocator,
                                    IContributionRoot additions) {
                                try {
                                    IContributionItem[] items = imc
                                            .getContributionItems(null,
                                                    new VariableSubstitution[0],
                                                    new HashSet<String>());
                                    Expression exp = null;
                                    if (imc.visibleOnActionSet != null) {
                                        org.eclipse.core.expressions.WithExpression we = new VisibleWithExpression(
                                                "activeContexts", items);

                                        org.eclipse.core.internal.expressions.IterateExpression oe = null;
                                        try {
                                            oe = new org.eclipse.core.internal.expressions.IterateExpression(
                                                    "or");
                                        } catch (CoreException e) {
                                            // ignore, since this is hardcoded
                                        }
                                        for (String str : imc.visibleOnActionSet) {
                                            org.eclipse.core.expressions.EqualsExpression ee = new org.eclipse.core.expressions.EqualsExpression(
                                                    str);
                                            oe.add(ee);
                                        }
                                        we.add(oe);
                                        exp = we;
                                    }

                                    for (IContributionItem item : items) {
                                        additions.addContributionItem(item,
                                                exp);
                                    }
                                } catch (VizException e) {
                                    statusHandler.handle(Priority.SIGNIFICANT,
                                            "Error setting up menus", e);
                                }
                            }
                        };
                        newFactories.add(viewMenuAddition);
                    }
                }
            } catch (JAXBException | IOException | LocalizationException
                    | ParseException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error parsing menu file: " + lf.getPath(), e);

            }
        }

        for (AbstractContributionFactory factory : factories) {
            menuService.removeContributionFactory(factory);

        }
        for (AbstractContributionFactory factory : newFactories) {
            menuService.addContributionFactory(factory);
        }
        factories = newFactories;
    }

    /**
     * This class works around bugs in eclipse that fail to update menu
     * visibility when a visibleWhen is used. Within eclipse it is properly
     * executing the visibleWhen expression, however the result does not always
     * affect the visibility of the IContributionItems so the expression does
     * not actually affect visibility. To workaround the problem this class
     * extends the WithExpression and applies the result of the Expression to
     * the visibility of the IContributionItems each time the expression is
     * evaluated.
     *
     * Internally the problem with eclipse is that a MMenuElement is created for
     * each IContributionItem and the Expression result is applied only to the
     * MMenuElement and not to the IContributionItem. The MenuManagerRenderer
     * has some code to listen for and copy the visibility from an MMenuElement
     * to a ContributionItem but it is not working right.
     *
     * This is primarily caused by
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=484143 because the
     * IContributionItems we provide are MenuManagers which is not a subclass of
     * ContributionItem. The code in MenuManagerRenderer is only checking for
     * ContributionItem so it does not work with MenuManager.
     *
     * The problem has become worse with changes made under
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=558279 because now the
     * MMenuElement can be removed from the model and is no longer sending out
     * notification when the visibility is changed. Without an event when
     * visibility changes it has become more difficult to respond to changes in
     * visibility. This class has been created to update the visibility of the
     * IContributionItem without relying on any notification from the
     * MMenuElement.
     *
     * This class should be removed once
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=484143 is resolved.
     *
     */
    private static class VisibleWithExpression
            extends org.eclipse.core.expressions.WithExpression {

        private final IContributionItem[] items;

        public VisibleWithExpression(String variable,
                IContributionItem[] items) {
            super(variable);
            this.items = items;
        }

        @Override
        public EvaluationResult evaluate(IEvaluationContext context)
                throws CoreException {
            EvaluationResult result = super.evaluate(context);
            for (IContributionItem item : items) {
                item.setVisible(result != EvaluationResult.FALSE);
            }
            return result;
        }
    }

    public static class MenuPathObserver implements ILocalizationPathObserver {
        @Override
        public void fileChanged(ILocalizationFile file) {
            VizApp.runAsync(() -> {
                synchronized (DiscoverMenuContributions.class) {
                    ran = false;
                    discoverContributions();
                }
            });
        }
    }
}
