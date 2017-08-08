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

import java.security.Principal;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.onap.clamp.clds.util.LoggingUtils;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Base/abstract Service class. Implements shared security methods.
 */
public abstract class SecureServiceBase {
    protected static final EELFLogger       logger          = EELFManager.getInstance().getLogger(SecureServiceBase.class);
    protected static final EELFLogger auditLogger     = EELFManager.getInstance().getAuditLogger();

    // By default we'll set it to a default handler
    private static UserNameHandler  userNameHandler = new DefaultUserNameHandler();

    @Context
    private SecurityContext         securityContext;

    /**
     * Get the userId from AAF/CSP.
     *
     * @return
     */
    public String getUserId() {
        return getUserName();
    }

    /**
     * Get the Full name.
     *
     * @return
     */
    public String getUserName() {
        String name = userNameHandler.retrieveUserName(securityContext);
        logger.debug("userName={}", name);
        return name;
    }

    /**
     * Get the principal name.
     *
     * @return
     */
    public String getPrincipalName() {
        Principal principal = securityContext.getUserPrincipal();
        String name = "Not found";
        if (principal != null) {
            name = principal.getName();
        }
        logger.debug("userPrincipal.getName()={}", name);
        return name;
    }

    /**
     * Check if user is authorized for the given the permission. Allow matches
     * if user has a permission with an "*" in permission instance or permission
     * action even if the permission to check has a specific value in those
     * fields. For example: if the user has this permission: app-perm-type|*|*
     * it will be authorized if the inPermission to check is:
     * app-perm-type|dev|read
     *
     * @param inPermission
     * @return
     * @throws NotAuthorizedException
     */
    public boolean isAuthorized(SecureServicePermission inPermission) throws NotAuthorizedException {
        boolean authorized = false;
        logger.debug("checking if {} has permission: {}", getPrincipalName(), inPermission);
        // check if the user has the permission key or the permission key with a
        // combination of all instance and/or all action.
        if (securityContext.isUserInRole(inPermission.getKey())) {
            logger.info("{} authorized for permission: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
            // the rest of these don't seem to be required - isUserInRole method
            // appears to take * as a wildcard
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstance())) {
            logger.info("{} authorized because user has permission with * for instance: {}", getPrincipalName(),
                    inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstanceAction())) {
            logger.info("{} authorized because user has permission with * for instance and * for action: {}",
                    getPrincipalName(), inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllAction())) {
            logger.info("{} authorized because user has permission with * for action: {}", getPrincipalName(),
                    inPermission.getKey());
            authorized = true;
        } else {
            String msg = getPrincipalName() + " does not have permission: " + inPermission;
            LoggingUtils.setErrorContext("100", "Authorization Error");
            logger.warn(msg);
            throw new NotAuthorizedException(msg);
        }
        return authorized;
    }

    /**
     * Check if user is authorized for the given aaf permission. Allow matches
     * if user has a permission with an "*" in permission instance or permission
     * action even if the permission to check has a specific value in those
     * fields. For example: if the user has this permission: app-perm-type|*|*
     * it will be authorized if the inPermission to check is:
     * app-perm-type|dev|read
     *
     * @param aafPermission
     * @return
     * @throws NotAuthorizedException
     */
    public boolean isAuthorizedNoException(SecureServicePermission inPermission) throws NotAuthorizedException {
        boolean authorized = false;
        logger.debug("checking if {} has permission: {}", getPrincipalName(), inPermission);
        // check if the user has the permission key or the permission key with a
        // combination of all instance and/or all action.
        if (securityContext.isUserInRole(inPermission.getKey())) {
            logger.info("{} authorized for permission: {}", getPrincipalName(), inPermission.getKey());
            authorized = true;
            // the rest of these don't seem to be required - isUserInRole method
            // appears to take * as a wildcard
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstance())) {
            logger.info("{} authorized because user has permission with * for instance: {}", getPrincipalName(),
                    inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllInstanceAction())) {
            logger.info("{} authorized because user has permission with * for instance and * for action: {}",
                    getPrincipalName(), inPermission.getKey());
            authorized = true;
        } else if (securityContext.isUserInRole(inPermission.getKeyAllAction())) {
            logger.info("{} authorized because user has permission with * for action: {}", getPrincipalName(),
                    inPermission.getKey());
            authorized = true;
        } else {
            String msg = getPrincipalName() + " does not have permission: " + inPermission;
            LoggingUtils.setErrorContext("100", "Authorization Error");
            logger.warn(msg);
        }
        return authorized;
    }

    public static final void setUserNameHandler(UserNameHandler handler) {
        if (handler != null) {
            userNameHandler = handler;
        }
    }
}
