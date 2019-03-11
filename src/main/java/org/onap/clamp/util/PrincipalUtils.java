/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.util;

import java.util.Date;

import org.onap.clamp.clds.service.DefaultUserNameHandler;
import org.onap.clamp.clds.service.UserNameHandler;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class PrincipalUtils {
    private static UserNameHandler userNameHandler = new DefaultUserNameHandler();
    private static SecurityContext securityContext = SecurityContextHolder.getContext();

    /**
     * Get the Full name.
     *
     * @return
     */
    public static String getUserName() {
        String name = userNameHandler.retrieveUserName(securityContext);
        Date startTime = new Date();
        LoggingUtils.setTargetContext("CLDS", "getUserName");
        LoggingUtils.setTimeContext(startTime, new Date());
        return name;
    }

    /**
     * Get the userId from AAF/CSP.
     *
     * @return
     */
    public static String getUserId() {
        return getUserName();
    }

    /**
     * Get the principal name.
     *
     * @return
     */
    public static String getPrincipalName() {
        String principal = ((UserDetails)securityContext.getAuthentication().getPrincipal()).getUsername();
        String name = "Not found";
        if (principal != null) {
            name = principal;
        }
        return name;
    }
    public static void setSecurityContext(SecurityContext securityContext) {
        PrincipalUtils.securityContext = securityContext;
    }

    public static SecurityContext getSecurityContext() {
        return securityContext;
    }
}
