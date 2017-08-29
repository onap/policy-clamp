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

package org.onap.clamp.clds.config;

import org.onap.clamp.clds.service.CldsUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * This class is used to enable the HTTP authentication to login. It requires a
 * specific JSON file containing the user definition
 * (classpath:etc/config/clds/clds-users.json).
 *
 */
@Configuration
@EnableWebSecurity
@Profile("clamp-spring-authentication")
public class CldsSecurityConfigUsers extends WebSecurityConfigurerAdapter {

    protected static final EELFLogger logger        = EELFManager.getInstance()
            .getLogger(CldsSecurityConfigUsers.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    @Autowired
    private ApplicationContext        appContext;

    @Value("${org.onap.clamp.config.files.cldsUsers:'classpath:etc/config/clds/clds-users.json'}")
    private String                    cldsUsersFile;

    @Value("${CLDS_PERMISSION_TYPE_CL:permission-type-cl}")
    private String                    cldsPersmissionTypeCl;

    @Value("${CLDS_PERMISSION_INSTANCE:dev}")
    private String                    cldsPermissionInstance;

    /**
     * This method configures on which URL the authorization will be enabled.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable().httpBasic().and().authorizeRequests().antMatchers("/restservices/clds/v1/user/**")
                .authenticated().anyRequest().permitAll().and().logout();
    }

    /**
     * This method is called by the framework and is used to load all the users
     * defined in cldsUsersFile variable (this file path can be configured in
     * the application.properties).
     * 
     * @param auth
     * @throws Exception
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        CldsUser[] usersList = loadUsers();

        // no users defined
        if (null == usersList) {
            logger.warn("No users defined. Users should be defined under " + cldsUsersFile);
            return;
        }

        for (CldsUser user : usersList) {
            auth.inMemoryAuthentication().withUser(user.getUser()).password(user.getPassword())
                    .roles(user.getPermissionsString());
        }
    }

    /**
     * This method loads physically the JSON file and convert it to an Array of
     * CldsUser.
     * 
     * @return The array of CldsUser
     * @throws Exception
     */
    private CldsUser[] loadUsers() throws Exception {
        logger.info("Load from clds-users.properties");
        return CldsUserJsonDecoder.decodeJson(appContext.getResource(cldsUsersFile).getInputStream());
    }
}
