/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 * 
 */

package org.onap.clamp.clds.service;

import java.util.Arrays;

/**
 * The class represents the CldsUser that can be extracted from cldsusers.json.
 */
public class CldsUser {

    private String user;
    private String password;
    private SecureServicePermission[] permissions;

    /**
     * Returns the user.
     * 
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * Sets the user.
     * 
     * @param user
     *            the user to set
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Returns the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     * 
     * @param password
     *            the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the permissions.
     * 
     * @return the permissions
     */
    public SecureServicePermission[] getPermissions() {
        return Arrays.copyOf(permissions, permissions.length);
    }

    public String[] getPermissionsString() {
        return Arrays.stream(getPermissions()).map(SecureServicePermission::getKey).toArray(String[]::new);
    }

    /**
     * Sets the permissions.
     *
     * @param permissionsArray
     *            the permissions to set
     */
    public void setPermissions(SecureServicePermission[] permissionsArray) {
        this.permissions = Arrays.copyOf(permissionsArray, permissionsArray.length);
    }
}
