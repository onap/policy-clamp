/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.onap.clamp.authorization.SecureServicePermission;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ClampServlet extends CamelHttpTransportServlet {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = -4198841134910211542L;

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(ClampServlet.class);
    private static final String PERM_INSTANCE = "clamp.config.security.permission.instance";
    private static final String PERM_CL = "clamp.config.security.permission.type.cl";
    private static final String PERM_TEMPLATE = "clamp.config.security.permission.type.template";
    private static final String PERM_VF = "clamp.config.security.permission.type.filter.vf";
    private static final String PERM_MANAGE = "clamp.config.security.permission.type.cl.manage";
    private static final String PERM_TOSCA = "clamp.config.security.permission.type.tosca";
    private static final String AUTHENTICATION_CLASS = "clamp.config.security.authentication.class";
    private static final String READ = "read";
    private static final String UPDATE = "update";

    private static List<SecureServicePermission> permissionList;

    private synchronized Class loadDynamicAuthenticationClass() {
        try {
            String authenticationObject = WebApplicationContextUtils.getWebApplicationContext(getServletContext())
                    .getEnvironment().getProperty(AUTHENTICATION_CLASS);
            return Class.forName(authenticationObject);
        } catch (ClassNotFoundException e) {
            logger.error(
                    "Exception caught when attempting to create associated class of config:" + AUTHENTICATION_CLASS, e);
            return Object.class;
        }
    }

    private synchronized List<SecureServicePermission> getPermissionList() {
        if (permissionList == null) {
            permissionList = new ArrayList<>();
            ApplicationContext applicationContext = WebApplicationContextUtils
                    .getWebApplicationContext(getServletContext());
            String cldsPermissionInstance = applicationContext.getEnvironment().getProperty(PERM_INSTANCE);
            permissionList.add(SecureServicePermission.create(applicationContext.getEnvironment().getProperty(PERM_CL),
                    cldsPermissionInstance, READ));
            permissionList.add(SecureServicePermission.create(applicationContext.getEnvironment().getProperty(PERM_CL),
                    cldsPermissionInstance, UPDATE));
            permissionList.add(SecureServicePermission.create(
                    applicationContext.getEnvironment().getProperty(PERM_TEMPLATE), cldsPermissionInstance, READ));
            permissionList.add(SecureServicePermission.create(
                    applicationContext.getEnvironment().getProperty(PERM_TEMPLATE), cldsPermissionInstance, UPDATE));
            permissionList.add(SecureServicePermission.create(applicationContext.getEnvironment().getProperty(PERM_VF),
                    cldsPermissionInstance, "*"));
            permissionList.add(SecureServicePermission
                    .create(applicationContext.getEnvironment().getProperty(PERM_MANAGE), cldsPermissionInstance, "*"));
            permissionList.add(SecureServicePermission
                    .create(applicationContext.getEnvironment().getProperty(PERM_TOSCA), cldsPermissionInstance, READ));
            permissionList.add(SecureServicePermission
                    .create(applicationContext.getEnvironment().getProperty(PERM_TOSCA),
                            cldsPermissionInstance, UPDATE));
        }
        return permissionList;
    }

    /**
     * When AAF is enabled, request object will contain a cadi Wrapper, so queries
     * to isUserInRole will invoke a http call to AAF server.
     */
    @Override
    protected void doService(HttpServletRequest request, HttpServletResponse response) {
        Principal principal = request.getUserPrincipal();
        if (loadDynamicAuthenticationClass().isInstance(principal)) {
            // When AAF is enabled, there is a need to provision the permissions to Spring
            // system
            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            for (SecureServicePermission perm : getPermissionList()) {
                String permString = perm.toString();
                if (request.isUserInRole(permString)) {
                    grantedAuths.add(new SimpleGrantedAuthority(permString));
                }
            }
            Authentication auth = new UsernamePasswordAuthenticationToken(new User(principal.getName(), "",
                    grantedAuths), "", grantedAuths);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        try {
            super.doService(request, response);
        } catch (ServletException | IOException ioe) {
            logger.error("Exception caught when executing doService in servlet", ioe);
            try {
                response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value());
            } catch (IOException e) {
                logger.error("Exception caught when executing HTTP sendError in servlet", e);
            }
        }
    }
}
