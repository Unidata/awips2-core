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
package com.raytheon.uf.common.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Data structure containing the defined users, roles, and permissions
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 25, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */
@DynamicSerialize
public class RolesAndPermissions {

    /**
     * Container for role description and associated permissions
     */
    @DynamicSerialize
    public static class Role {
        @DynamicSerializeElement
        private String description;

        @DynamicSerializeElement
        private Set<String> permissions;

        /**
         * Default constructor for dynamic serialization
         */
        public Role() {
        }

        /**
         * Constructor
         *
         * @param description
         * @param permissions
         */
        public Role(String description, Set<String> permissions) {
            this.description = description;
            this.permissions = permissions;
        }

        /**
         * Copy constructor
         *
         * @param role
         */
        public Role(Role role) {
            this(role.description, new HashSet<>(role.permissions));
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         *
         * @param description
         *            the description to set
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return the permissions
         */
        public Set<String> getPermissions() {
            return permissions;
        }

        /**
         * @param permissions
         *            the permissions to set
         */
        public void setPermissions(Set<String> permissions) {
            this.permissions = permissions;
        }
    }

    @DynamicSerializeElement
    private String defaultRole = "defaultUser";

    @DynamicSerializeElement
    private String adminRole = "admin";

    @DynamicSerializeElement
    private Set<String> protectedRoles = Collections.emptySet();

    @DynamicSerializeElement
    private Set<String> protectedUsers = Collections.emptySet();

    @DynamicSerializeElement
    private Map<String, String> permissions = Collections.emptyMap();

    @DynamicSerializeElement
    private Map<String, Role> roles = Collections.emptyMap();

    @DynamicSerializeElement
    private Map<String, Set<String>> users = Collections.emptyMap();

    @DynamicSerializeElement
    private Map<String, Set<String>> userChanges = Collections.emptyMap();

    @DynamicSerializeElement
    private Map<String, Role> roleChanges = Collections.emptyMap();

    /**
     * @return the defaultRole
     */
    public String getDefaultRole() {
        return defaultRole;
    }

    /**
     * @param defaultRole
     *            the defaultRole to set
     */
    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    /**
     * @return the adminRole
     */
    public String getAdminRole() {
        return adminRole;
    }

    /**
     * @param adminRole
     *            the adminRole to set
     */
    public void setAdminRole(String adminRole) {
        this.adminRole = adminRole;
    }

    /**
     * @return the protectedUsers
     */
    public Set<String> getProtectedUsers() {
        return protectedUsers;
    }

    /**
     * @param protectedUsers
     *            the protectedUsers to set
     */
    public void setProtectedUsers(Set<String> protectedUsers) {
        this.protectedUsers = protectedUsers;
    }

    /**
     * @return the protectedRoles
     */
    public Set<String> getProtectedRoles() {
        return protectedRoles;
    }

    /**
     * @param protectedRoles
     *            the protectedRoles to set
     */
    public void setProtectedRoles(Set<String> protectedRoles) {
        this.protectedRoles = protectedRoles;
    }

    /**
     * Returns a map where the keys are all defined permissions and the values
     * are the descriptions of those permissions
     *
     * @return the permissions
     */
    public Map<String, String> getPermissions() {
        return permissions;
    }

    /**
     * Sets the permissions. The map should contain keys for all defined
     * permissions with values containing descriptions of those permissions
     *
     * @param permissions
     *            the permissions to set
     */
    public void setPermissions(Map<String, String> permissions) {
        this.permissions = permissions;
    }

    /**
     * Returns a map where the keys are the names of the defined roles and the
     * values are the roles.
     *
     * @return the roles
     */
    public Map<String, Role> getRoles() {
        return roles;
    }

    /**
     * Sets the roles. The map should contain keys for all defined roles with
     * values containing the role.
     *
     * @param roles
     *            the roles to set
     */
    public void setRoles(Map<String, Role> roles) {
        this.roles = roles;
    }

    /**
     * Returns a map where the keys are all defined users and the values contain
     * a list of all roles assigned to the user.
     *
     * @return the users
     */
    public Map<String, Set<String>> getUsers() {
        return users;
    }

    /**
     * Sets the users. The map should contain keys for all defined users with
     * values containing a list of all roles assigned to the user.
     *
     * @param users
     *            the users to set
     */
    public void setUsers(Map<String, Set<String>> users) {
        this.users = users;
    }

    /**
     * @return the userChanges
     */
    public Map<String, Set<String>> getUserChanges() {
        return userChanges;
    }

    /**
     * @param userChanges
     *            the userChanges to set
     */
    public void setUserChanges(Map<String, Set<String>> userChanges) {
        this.userChanges = userChanges;
    }

    /**
     * @return the roleChanges
     */
    public Map<String, Role> getRoleChanges() {
        return roleChanges;
    }

    /**
     * @param roleChanges
     *            the roleChanges to set
     */
    public void setRoleChanges(Map<String, Role> roleChanges) {
        this.roleChanges = roleChanges;
    }

    /**
     * Update a user's assigned roles
     *
     * @param username
     * @param roles
     *            if null user will be deleted
     */
    public void updateUser(String username, Set<String> roles) {
        if (roles == null) {
            getUsers().remove(username);
        } else {
            getUsers().put(username, roles);
        }
        userChanges.put(username, roles);
    }

    /**
     * Update a role's assigned permissions
     *
     * @param name
     * @param role
     *            if null role will be deleted
     */
    public void updateRole(String name, Role role) {
        if (role == null) {
            getRoles().remove(name);
        } else {
            getRoles().put(name, role);
        }
        roleChanges.put(name, role);
    }
}