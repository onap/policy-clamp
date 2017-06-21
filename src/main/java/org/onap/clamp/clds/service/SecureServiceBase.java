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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * Base/abstract Service class.
 * Implements shared security methods.
 */
public abstract class SecureServiceBase {
    private static final Logger logger = LoggerFactory.getLogger(SecureServiceBase.class);

    @Context
    private SecurityContext securityContext;

    /**
     * Get the userid
     *
     * @return
     */
    public String getUserid() {
        return getPrincipalName();
    }

    /**
     * Get the principal name.
     *
     * @return
     */
    public String getPrincipalName() {
        Principal p = securityContext.getUserPrincipal();
        String name = "Not found";
        if (p != null) {
            name = p.getName();
        }
        logger.debug("userPrincipal.getName()={}", name);
        return name;
    }

    /**
     * Check if user is authorized for the given the permission.
     * Allow matches if user has a permission with an "*" in permission instance
     * or permission action even if the permission to check has a specific value
     * in those fields.  For example:
     * if the user has this permission: app-perm-type|*|*
     * it will be authorized if the inPermission to check is: app-perm-type|dev|read
     *
     * @param inPermission
     * @return
     * @throws NotAuthorizedException
     */
    public boolean isAuthorized(SecureServicePermission inPermission) throws NotAuthorizedException {
        boolean authorized = false;
        logger.debug("checking if {} has permission: {}", getPrincipalName(), inPermission);
     // check if the user has the permission key or the permission key with a combination of all instance and/or all action.
        if (securityContext.isUserInRole(inPermission.getKey())) {
            logger.info("{} authorized for permission: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
            // the rest of these don't seem to be required - isUserInRole method appears to take * as a wildcard
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstance())) {
            logger.info("{} authorized because user has permission with * for instance: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstanceAction())) {
            logger.info("{} authorized because user has permission with * for instance and * for action: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllAction())) {
            logger.info("{} authorized because user has permission with * for action: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
        } else {
            String msg = getPrincipalName() + " does not have permission: " + inPermission;
            logger.warn(msg);
            throw new NotAuthorizedException(msg);
        }
        return authorized;
    }

}
