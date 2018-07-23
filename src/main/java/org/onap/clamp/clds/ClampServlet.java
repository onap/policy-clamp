/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.service.SecureServicePermission;
import org.onap.clamp.clds.util.ClampTimer;


public class ClampServlet extends CamelHttpTransportServlet {

    protected static final EELFLogger logger          = EELFManager.getInstance().getLogger(ClampServlet.class);
    public static final String PERM_INSTANCE = "clamp.config.security.permission.instance";
    public static final String PERM_CL= "clamp.config.security.permission.type.cl";
    public static final String PERM_TEMPLACE = "clamp.config.security.permission.type.template";

    protected void doService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        List<SecureServicePermission> permissionList = new ArrayList<>();

        // Get Principal info and translate it into Spring Authentication
        // If authenticataion is null: a) the authentication info was set manually in the previous thread 
        //                             b) handled by Spring automatically
        // for the 2 cases above, no need for the translation, just skip the following step
        if (null == authentication) {
           logger.debug ("Populate Spring Authenticataion info manually.");
            ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext());
            // Start a timer to clear the authentication after 5 mins, so that the authentication will be reinitialized with AAF DB
            new ClampTimer(300);
            String cldsPersmissionTypeCl = applicationContext.getEnvironment().getProperty(PERM_INSTANCE);
            String cldsPermissionTypeTemplate = applicationContext.getEnvironment().getProperty(PERM_CL);
            String cldsPermissionInstance = applicationContext.getEnvironment().getProperty(PERM_TEMPLACE);

            // set the stragety to Mode_Global, so that all thread is able to see the authentication
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
            Principal p = request.getUserPrincipal(); 

            permissionList.add(SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "read"));
            permissionList.add(SecureServicePermission.create(cldsPersmissionTypeCl, cldsPermissionInstance, "update"));
            permissionList.add(SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance, "read"));
            permissionList.add(SecureServicePermission.create(cldsPermissionTypeTemplate, cldsPermissionInstance, "update"));

            List<GrantedAuthority> grantedAuths = new ArrayList<>();
            for (SecureServicePermission perm:permissionList) {
                String permString = perm.toString();
                if (request.isUserInRole(permString)) {
                    grantedAuths.add(new SimpleGrantedAuthority(permString));
                }
            }
            Authentication auth =  new UsernamePasswordAuthenticationToken(new User(p.getName(), "", grantedAuths), "", grantedAuths);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        super.doService(request, response);
    }
}