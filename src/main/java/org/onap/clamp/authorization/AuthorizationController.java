/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.authorization;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.Date;

import javax.ws.rs.NotAuthorizedException;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.service.SecureServiceBase;
import org.onap.clamp.clds.service.SecureServicePermission;
import org.onap.clamp.clds.util.LoggingUtils;
import org.onap.clamp.util.PrincipalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Create CLDS Event.
 */
@Component
public class AuthorizationController {

    protected static final EELFLogger logger          = EELFManager.getInstance().getLogger(SecureServiceBase.class);
    protected static final EELFLogger auditLogger     = EELFManager.getInstance().getMetricsLogger();
    protected static final EELFLogger securityLogger  = EELFManager.getInstance().getSecurityLogger();

    // By default we'll set it to a default handler
    @Autowired
    private ClampProperties refProp;

    private static final String PERM_PREFIX = "security.permission.type.";
    private static final String PERM_INSTANCE = "security.permission.instance";

    /**
     * Insert authorize the api based on the permission
     *
     * @param camelExchange
     *        The Camel Exchange object containing the properties
     * @param typeVar
     *        The type of the permissions
     * @param instanceVar
     *        The instance of the permissions. e.g. dev
     * @param action
     *        The action of the permissions. e.g. read
     */
    public void authorize(Exchange camelExchange, String typeVar, String instanceVar, String action) {
        String type = refProp.getStringValue(PERM_PREFIX + typeVar);
        String instance = refProp.getStringValue(PERM_INSTANCE);

        if (null == type || type.isEmpty()) {
            //authorization is turned off, since the permission is not defined
            return;
        }
        if (null != instanceVar && !instanceVar.isEmpty()) {
            instance = instanceVar;
        }
        String principalName = PrincipalUtils.getPrincipalName();
        SecureServicePermission perm = SecureServicePermission.create(type, instance, action);
        Date startTime = new Date();
        LoggingUtils.setTargetContext("Clamp", "authorize");
        LoggingUtils.setTimeContext(startTime, new Date());
        securityLogger.debug("checking if {} has permission: {}", principalName, perm);

        if (!isUserPermitted(perm)){
            String msg = principalName + " does not have permission: " + perm;
            LoggingUtils.setErrorContext("100", "Authorization Error");
            securityLogger.warn(msg);
            throw new NotAuthorizedException(msg);
        }
    }

    public boolean isUserPermitted(SecureServicePermission inPermission) {

        String principalName = PrincipalUtils.getPrincipalName();
        // check if the user has the permission key or the permission key with a
        // combination of  all instance and/or all action.
        if (hasRole(inPermission.getKey()) || hasRole(inPermission.getKeyAllInstance())) {
            auditLogger.info("{} authorized because user has permission with * for instance: {}",
                    principalName, inPermission.getKey());
            return true;
            // the rest of these don't seem to be required - isUserInRole method
            // appears to take * as a wildcard
        } else if (hasRole(inPermission.getKeyAllInstanceAction())) {
            auditLogger.info("{} authorized because user has permission with * for instance and * for action: {}",
                    principalName, inPermission.getKey());
            return true;
        } else if (hasRole(inPermission.getKeyAllAction())) {
            auditLogger.info("{} authorized because user has permission with * for action: {}",
                    principalName, inPermission.getKey());
            return true;
        } else {
            return false;
        }
    }

    protected boolean hasRole(String role) {
        Authentication authentication = PrincipalUtils.getSecurityContext().getAuthentication();
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

}
