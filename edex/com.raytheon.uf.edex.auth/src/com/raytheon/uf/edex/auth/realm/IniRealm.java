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
package com.raytheon.uf.edex.auth.realm;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;
import org.apache.shiro.authz.permission.PermissionResolver;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.util.PermissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.util.PermissionDescriptionBuilder;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.IRolesAndPermissionsStore;
import com.raytheon.uf.edex.auth.ShiroPermissionsManager;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * IniRealm which initializes from multiple ini files from localization tree
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 04, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class IniRealm extends org.apache.shiro.realm.text.IniRealm
        implements IRolesAndPermissionsStore {
    private static final String PERMISSIONS_SECTION_NAME = "permissions";

    private static final String USERS_INI_HEADER = ""
            + "# -----------------------------------------------------------------------------\n"
            + "# This file should not be manually edited.\n"
            + "# Please use the user administration GUI to modify user roles/permissions.\n"
            + "# -----------------------------------------------------------------------------\n"
            + "# Users and their (optional) assigned roles\n"
            + "# Users may only have roles, they may not have permissions.\n#\n"
            + "# username = password, role1, role2, ..., roleN\n"
            + "# -----------------------------------------------------------------------------";

    private static final String ROLES_INI_HEADER = ""
            + "# -----------------------------------------------------------------------------\n"
            + "# This file should not be manually edited.\n"
            + "# Please use the user administration GUI to modify user roles/permissions.\n"
            + "# -----------------------------------------------------------------------------\n"
            + "# Roles with assigned permissions\n"
            + "# roleName = perm1, perm2, ..., permN\n"
            + "# -----------------------------------------------------------------------------";

    private final Logger log = LoggerFactory.getLogger(IniRealm.class);

    private String defaultRole = "defaultUser";

    private String adminRole = "admin";

    private Set<String> protectedUsers = new HashSet<>();

    private Set<String> protectedRoles = new HashSet<>();

    /** permission-to-description */
    protected final Map<String, String> permissions;

    /** permissions lock */
    protected final ReadWriteLock PERMISSIONS_LOCK;

    /**
     * Constructor
     */
    public IniRealm() {
        super();

        this.setPermissionResolver(new PermissionResolver() {
            @Override
            public Permission resolvePermission(String permissionString) {
                /* allow case sensitive permissions */
                return new WildcardPermission(permissionString, true);
            }
        });

        this.permissions = new LinkedHashMap<>();
        PERMISSIONS_LOCK = new ReentrantReadWriteLock();

        setIni(getMergedIni());
        init();
        setCachingEnabled(true);
        setAuthenticationCachingEnabled(true);
    }

    /**
     * @param defaultRole
     *            the defaultRole to set
     */
    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    /**
     * @param adminRole
     *            the adminRole to set
     */
    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    private Ini readIni(ILocalizationFile lf)
            throws IOException, LocalizationException, ConfigurationException {
        try (InputStream in = lf.openInputStream()) {
            Ini ini = new Ini();

            ini.load(in);

            return ini;
        }
    }

    private void writeIni(String header, Ini ini, ILocalizationFile lf) {
        try (SaveableOutputStream stream = lf.openOutputStream();
                PrintWriter out = new PrintWriter(stream)) {
            out.println(header);

            for (Section section : ini.getSections()) {
                out.println(String.format("[%s]", section.getName()));
                for (Entry<String, String> entry : section.entrySet()) {
                    out.println(String.format("%s = %s", entry.getKey(),
                            entry.getValue()));
                }
                out.println();
            }
            out.close();
            stream.save();

        } catch (LocalizationException | IOException e) {
            log.error("Error writing ini file: " + lf, e);
        }
    }

    private class MergeValues implements BiFunction<String, String, String> {

        @Override
        public String apply(String s1, String s2) {
            Set<String> set = new HashSet<>(
                    Arrays.asList(s1.split("\\s*,\\s*")));

            set.addAll(Arrays.asList(s2.split("\\s*,\\s*")));

            return String.join(", ", set.toArray(new String[set.size()]));
        }

    }

    private Ini getMergedIni() {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext baseCtx = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);
        LocalizationContext configCtx = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        LocalizationContext[] contexts = new LocalizationContext[] { baseCtx,
                configCtx };

        ILocalizationFile[] iniFiles = pm.listFiles(contexts, "roles",
                new String[] { ".ini" }, false, true);

        Ini merged = new Ini();
        MergeValues mergeValues = new MergeValues();
        for (ILocalizationFile lf : iniFiles) {
            try {
                Ini ini = readIni(lf);
                for (Section section : ini.getSections()) {
                    String sectionName = section.getName();
                    for (Entry<String, String> entry : section.entrySet()) {
                        Section mergedSection = merged.getSection(sectionName);
                        if (mergedSection == null) {
                            merged.setSectionProperty(sectionName,
                                    entry.getKey(), entry.getValue());
                        } else {
                            mergedSection.merge(entry.getKey(),
                                    entry.getValue(), mergeValues);
                        }
                    }
                    if (LocalizationLevel.BASE
                            .equals(lf.getContext().getLocalizationLevel())) {
                        if (USERS_SECTION_NAME.equals(sectionName)) {
                            protectedUsers.addAll(section.keySet());

                        } else if (ROLES_SECTION_NAME.equals(sectionName)) {
                            protectedRoles.addAll(section.keySet());
                        }
                    }
                }
            } catch (Throwable e) {
                log.error("Error reading ini file: " + lf + ". File ignored.",
                        e);
            }
        }
        return merged;
    }

    @Override
    protected void onInit() {
        super.onInit();

        Ini ini = getIni();

        Ini.Section permissionsSection = ini
                .getSection(PERMISSIONS_SECTION_NAME);
        processPermissionDefinitions(permissionsSection);
    }

    private void processPermissionDefinitions(
            Map<String, String> permissionDefs) {
        if (permissionDefs != null) {
            for (Entry<String, String> entry : permissionDefs.entrySet()) {
                addPermission(entry.getKey(), entry.getValue());
            }

            try {
                ROLES_LOCK.readLock().lock();
                try {
                    PERMISSIONS_LOCK.writeLock().lock();
                    for (SimpleRole role : roles.values()) {
                        for (Permission permission : role.getPermissions()) {
                            String permString = permission.toString();
                            if (permString.startsWith("localization")
                                    && !permissions.containsKey(permString)) {
                                permissions.put(permString,
                                        PermissionDescriptionBuilder
                                                .buildDescription(permString));
                            }
                        }
                    }
                } finally {
                    PERMISSIONS_LOCK.writeLock().unlock();
                }
            } finally {
                ROLES_LOCK.readLock().unlock();
            }
        }

    }

    /**
     * Add a new permission
     *
     * @param permission
     * @param description
     */
    protected void addPermission(String permission, String description) {
        PERMISSIONS_LOCK.writeLock().lock();
        try {
            permissions.put(permission, description);
        } finally {
            PERMISSIONS_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void addAccount(String username, String password, String... roles) {
        Map<String, Set<String>> userChanges = new HashMap<>(1, 1.0f);
        userChanges.put(username, new HashSet<>(Arrays.asList(roles)));
        updateAccounts(password, userChanges);
    }

    private void updateAccounts(String password,
            Map<String, Set<String>> userChanges) {
        if (userChanges.isEmpty()) {
            return;
        }

        // get the site users.ini file
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext context = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile lf = pm.getLocalizationFile(context,
                LocalizationUtil.join("roles", "users.ini"));

        // if file exists load it, otherwise create new Ini
        Ini siteUsers;
        if (lf.exists()) {
            try {
                siteUsers = readIni(lf);
            } catch (Throwable e) {
                /*
                 * Most likely a corrupted users.ini file, this is bad!!!
                 *
                 * We won't overwrite the file so the site admin can attempt to
                 * repair it.
                 */
                log.error(
                        "Error reading: " + lf
                                + ". Unable to create new user roles and permissions account",
                        e);

                StringWriter stackTrace = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTrace));
                EDEXUtil.sendMessageAlertViz(Priority.ERROR, "AUTH", "EDEX",
                        "MISC",
                        "Error reading: " + lf
                                + ". Unable to create new user roles and permissions account",
                        stackTrace.toString(), null);
                return;
            }
        } else {
            siteUsers = new Ini();
        }

        for (Entry<String, Set<String>> entry : userChanges.entrySet()) {
            String username = entry.getKey();
            Set<String> roles = entry.getValue();

            if (roles == null || roles.isEmpty()) {
                /* remove account */
                this.users.remove(username);

                Section usersSection = siteUsers.getSection(USERS_SECTION_NAME);
                if (usersSection != null) {
                    usersSection.remove(username);
                }

            } else {
                /* add/update account */
                super.addAccount(username, password,
                        roles.toArray(new String[roles.size()]));

                // add the new user with specified password and roles
                siteUsers.setSectionProperty(USERS_SECTION_NAME, username,
                        String.join(", ", password, String.join(", ", roles)));
            }
        }

        // update the site level users.ini file
        writeIni(USERS_INI_HEADER, siteUsers, lf);
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(
            AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken up = (UsernamePasswordToken) token;

        /*
         * Since we are not really authenticating users yet, just create a new
         * user with the specified password and role if the user does not
         * already have an account
         */
        String username = up.getUsername();
        String password = new String(up.getPassword());
        if (!accountExists(username)) {
            addAccount(username, password, defaultRole);
        }
        return super.doGetAuthenticationInfo(token);
    }

    @Override
    public RolesAndPermissions getRolesAndPermissions() {

        RolesAndPermissions retVal = new RolesAndPermissions();
        retVal.setDefaultRole(defaultRole);
        retVal.setAdminRole(adminRole);
        retVal.setProtectedRoles(protectedRoles);
        retVal.setProtectedUsers(protectedUsers);

        PERMISSIONS_LOCK.readLock().lock();
        try {
            Map<String, String> permDefs = new HashMap<>(permissions.size(),
                    1.0f);
            for (Entry<String, String> entry : permissions.entrySet()) {
                permDefs.put(entry.getKey().replace('.', ':'),
                        entry.getValue());
            }
            retVal.setPermissions(permDefs);
        } finally {
            PERMISSIONS_LOCK.readLock().unlock();
        }

        ROLES_LOCK.readLock().lock();
        try {
            Map<String, Set<String>> roleDefs = new HashMap<>(roles.size(),
                    1.0f);
            for (Entry<String, SimpleRole> entry : roles.entrySet()) {
                Set<String> permissionStrings = new HashSet<>(
                        entry.getValue().getPermissions().size(), 1.0f);
                for (Permission permission : entry.getValue()
                        .getPermissions()) {
                    permissionStrings.add(permission.toString());
                }
                roleDefs.put(entry.getKey(), permissionStrings);
            }
            retVal.setRoles(roleDefs);
        } finally {
            ROLES_LOCK.readLock().unlock();
        }

        USERS_LOCK.readLock().lock();
        try {
            Map<String, Set<String>> userDefs = new HashMap<>(users.size(),
                    1.0f);
            for (Entry<String, SimpleAccount> entry : users.entrySet()) {
                userDefs.put(entry.getKey(),
                        new HashSet<>(entry.getValue().getRoles()));
            }
            retVal.setUsers(userDefs);
        } finally {
            USERS_LOCK.readLock().unlock();
        }

        return retVal;
    }

    private void updateRoles(Map<String, Set<String>> roleChanges) {
        if (roleChanges.isEmpty()) {
            return;
        }

        // get the site roles.ini file
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext context = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.CONFIGURED);
        ILocalizationFile lf = pm.getLocalizationFile(context,
                LocalizationUtil.join("roles", "roles.ini"));

        // if file exists load it, otherwise create new Ini
        Ini siteRoles;
        if (lf.exists()) {
            try {
                siteRoles = readIni(lf);
            } catch (Throwable e) {
                /*
                 * Most likely a corrupted users.ini file, this is bad!!!
                 *
                 * We won't overwrite the file so the site admin can attempt to
                 * repair it.
                 */
                log.error("Error reading: " + lf + ". Unable to update users",
                        e);
                return;
            }
        } else {
            siteRoles = new Ini();
            siteRoles.addSection(ROLES_SECTION_NAME);
        }

        for (Entry<String, Set<String>> entry : roleChanges.entrySet()) {
            String rolename = entry.getKey();
            Set<String> values = entry.getValue();

            if (values == null || values.isEmpty()) {
                /* remove role */
                this.roles.remove(rolename);

                Section rolesSection = siteRoles.getSection(ROLES_SECTION_NAME);
                if (rolesSection != null) {
                    rolesSection.remove(rolename);
                }

            } else {
                /* add/update role */
                List<String> permissions = new ArrayList<>(values.size());
                for (String permission : entry.getValue()) {
                    permissions.add(permission);

                    /* add new permissions as required */
                    PERMISSIONS_LOCK.writeLock().lock();
                    try {
                        if (!this.permissions.containsKey(permission)) {
                            this.permissions.put(permission,
                                    PermissionDescriptionBuilder
                                            .buildDescription(permission));
                        }
                    } finally {
                        PERMISSIONS_LOCK.writeLock().unlock();
                    }
                }

                /* Create role if it doesn't already exist */
                SimpleRole role = getRole(rolename);
                if (role == null) {
                    role = new SimpleRole(rolename);
                    add(role);
                }

                /* Set the role's permissions */
                role.setPermissions(PermissionUtils.resolvePermissions(
                        permissions, getPermissionResolver()));

                /* add the new role with specified permissions */
                siteRoles.setSectionProperty(ROLES_SECTION_NAME, rolename,
                        String.join(", ", permissions));
            }
        }

        // update the site level users.ini file
        writeIni(ROLES_INI_HEADER, siteRoles, lf);
    }

    @Override
    public RolesAndPermissions saveRolesAndPermissions(
            RolesAndPermissions rolesAndPermissions) {
        updateAccounts(ShiroPermissionsManager.DEFAULT_PASSWORD,
                rolesAndPermissions.getUserChanges());

        updateRoles(rolesAndPermissions.getRoleChanges());

        return getRolesAndPermissions();
    }

}
