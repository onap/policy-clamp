/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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

package org.onap.clamp.authorization;

/**
 * Permission class that can be instantiated easily using constructor or factory
 * methods.
 */
public class SecureServicePermission {
    public static final String ALL = "*";

    private String             type;
    private String             instance;
    private String             action;

    /**
     * Factory method to create permission given type, instance, and action.
     *
     * @param type type of the permission
     * @param instance instance of the permission
     * @param action action of the permission
     * @return instance of SecureServicePermission with type, instance and action
     */
    public static SecureServicePermission create(String type, String instance, String action) {
        return new SecureServicePermission(type, instance, action);
    }

    /**
     * Factory method to create permission given type and instance. Default
     * action to ALL/*.
     *
     * @param type type of the permission
     * @param instance instance of the permission
     * @return instance of SecureServicePermission with type, instance and default action
     */
    public static SecureServicePermission create(String type, String instance) {
        return new SecureServicePermission(type, instance, ALL);
    }

    /**
     * Factory method to create permission given type. Default instance and
     * action to ALL/*.
     *
     * @param type type of the permission
     * @return instance of SecureServicePermission with type and default instance and action
     */
    public static SecureServicePermission create(String type) {
        return new SecureServicePermission(type, ALL, ALL);
    }

    /**
     * Instantiate permission given type, instance, and action.
     *
     * @param type type of the permission
     * @param instance instance of the permission
     * @param action action of the permission
     */
    public SecureServicePermission(String type, String instance, String action) {
        this.type = type;
        this.instance = instance;
        this.action = action;
    }

    /**
     * Instantiate permission given type from concatenated string
     *
     * @param concatenatedString
     *            the string type|instance|action, less than 3 params can be
     *            provided (e.g. "permission-type-cl", "permission-type-cl|dev",
     *            "permission-type-cl|dev|update" )
     */
    public SecureServicePermission(String concatenatedString) {
        String[] userInfo = concatenatedString.split("[|]");
        // We should have at least 1 string
        this.type = userInfo[0];
        this.instance = (userInfo.length > 1 ? userInfo[1] : ALL);
        this.action = (userInfo.length > 2 ? userInfo[2] : ALL);
    }

    /**
     * Override toString - return permission in key format.
     */
    @Override
    public String toString() {
        return getKey();
    }

    /**
     * Return Permission in Key format = type, instance, and action separate by
     * pipe character.
     *
     * @return permission in key format
     */
    public String getKey() {
        return type + "|" + instance + "|" + action;
    }

    /**
     * Return Permission in Key format = type, all instance, and action separate
     * by pipe character.
     *
     * @return permission in key format
     */
    public String getKeyAllInstance() {
        return type + "|" + ALL + "|" + action;
    }

    /**
     * Return Permission in Key format = type, all instance, and all action
     * separate by pipe character.
     *
     * @return permission in key format
     */
    public String getKeyAllInstanceAction() {
        return type + "|" + ALL + "|" + ALL;
    }

    /**
     * Return Permission in Key format = type, instance, and all action separate
     * by pipe character.
     *
     * @return permission in key format
     */
    public String getKeyAllAction() {
        return type + "|" + instance + "|" + ALL;
    }

    /**
     * Returns the permission type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of permission.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the instance of permission.
     *
     * @return the instance
     */
    public String getInstance() {
        return instance;
    }

    /**
     * Sets the instance of permission.
     *
     * @param instance the instance to set
     */
    public void setInstance(String instance) {
        this.instance = instance;
    }

    /**
     * Returns the action of permission.
     *
     * @return the action
     */
    public String getAction() {
        return action;
    }

    /**
     * Sets the action of permission.
     *
     * @param action the action to set
     */
    public void setAction(String action) {
        this.action = action;
    }

}
