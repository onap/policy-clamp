/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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

package org.onap.policy.clamp.authorization;

import java.util.Date;
import org.apache.camel.Exchange;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.clds.exception.NotAuthorizedException;
import org.onap.policy.clamp.clds.model.ClampInformation;
import org.onap.policy.clamp.clds.util.LoggingUtils;
import org.onap.policy.common.utils.logging.LoggerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Verify user has right permissions.
 */
@Component
public class AuthorizationController {

    protected static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);

    // By default we'll set it to a default handler
    @Autowired
    private ClampProperties refProp;

    public static final String PERM_PREFIX = "security.permission.type.";
    private static final String PERM_INSTANCE = "security.permission.instance";

    private static String retrieveUserName(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getAuthentication() == null) {
            return null;
        }
        if ((securityContext.getAuthentication().getPrincipal()) instanceof String) {
            // anonymous case
            return ((String) securityContext.getAuthentication().getPrincipal());
        } else {
            return ((UserDetails) securityContext.getAuthentication().getPrincipal()).getUsername();
        }
    }

    /**
     * Get the principal name.
     *
     * @return The principal name
     */
    public static String getPrincipalName(SecurityContext securityContext) {
        String principal = AuthorizationController.retrieveUserName(securityContext);
        return principal != null ? principal : "Not found";
    }

    /**
     * Insert authorize the api based on the permission.
     *
     * @param camelExchange The Camel Exchange object containing the properties
     * @param typeVar       The type of the permissions
     * @param instanceVar   The instance of the permissions. e.g. dev
     * @param action        The action of the permissions. e.g. read
     */
    public void authorize(Exchange camelExchange, String typeVar, String instanceVar,
                          String action) {
        var type = refProp.getStringValue(PERM_PREFIX + typeVar);
        var instance = refProp.getStringValue(PERM_INSTANCE);

        if (null == type || type.isEmpty()) {
            // authorization is turned off, since the permission is not defined
            return;
        }
        if (null != instanceVar && !instanceVar.isEmpty()) {
            instance = instanceVar;
        }
        String principalName = AuthorizationController.getPrincipalName(SecurityContextHolder.getContext());
        var perm = SecureServicePermission.create(type, instance, action);
        var startTime = new Date();
        LoggingUtils.setTargetContext("Clamp", "authorize");
        LoggingUtils.setTimeContext(startTime, new Date());
        logger.debug(LoggerUtils.SECURITY_LOG_MARKER, "checking if {} has permission: {}", principalName, perm);

        if (!isUserPermitted(perm)) {
            String msg = principalName + " does not have permission: " + perm;
            LoggingUtils.setErrorContext("100", "Authorization Error");
            logger.warn(LoggerUtils.SECURITY_LOG_MARKER, msg);

            // FIXME: Temporary Fix disabled since second request is coming in as anonymous user
            //throw new NotAuthorizedException(msg);
        }
    }

    /**
     * Insert authorize the api based on the permission.
     *
     * @param inPermission Security permission in input
     * @return True if user is permitted
     */
    public boolean isUserPermitted(SecureServicePermission inPermission) {

        String principalName = AuthorizationController.getPrincipalName(SecurityContextHolder.getContext());
        // check if the user has the permission key or the permission key with a
        // combination of all instance and/or all action.
        if (hasRole(inPermission.getKey()) || hasRole(inPermission.getKeyAllInstance())) {
            logger.info(LoggerUtils.AUDIT_LOG_MARKER,
                    "{} authorized because user has permission with * for instance: {}",
                    principalName, inPermission.getKey().replace("|", ":"));
            return true;
            // the rest of these don't seem to be required - isUserInRole method
            // appears to take * as a wildcard
        } else if (hasRole(inPermission.getKeyAllInstanceAction())) {
            logger.info(LoggerUtils.AUDIT_LOG_MARKER,
                    "{} authorized because user has permission with * for instance and * for action: {}",
                    principalName, inPermission.getKey().replace("|", ":"));
            return true;
        } else if (hasRole(inPermission.getKeyAllAction())) {
            logger.info(LoggerUtils.AUDIT_LOG_MARKER,
                    "{} authorized because user has permission with * for action: {}",
                    principalName, inPermission.getKey().replace("|", ":"));
            return true;
        } else {
            return false;
        }
    }

    protected boolean hasRole(String role) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (role.equals(auth.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets clds info. CLDS IFO service will return 3 things 1. User Name 2. CLDS
     * code version that is currently installed from pom.xml file 3. User
     * permissions
     *
     * @return the clds info
     */
    public ClampInformation getClampInformation() {
        var clampInfo = new ClampInformation();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return new ClampInformation();
        }
        clampInfo.setUserName(AuthorizationController.getPrincipalName(SecurityContextHolder.getContext()));
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            clampInfo.getAllPermissions().add(auth.getAuthority());
        }
        return clampInfo;
    }
}
