/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.config;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.io.IOException;
import org.onap.policy.clamp.authorization.CldsUser;
import org.onap.policy.clamp.clds.exception.CldsConfigException;
import org.onap.policy.clamp.clds.exception.CldsUsersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This class is used to enable the HTTP authentication to login. It requires a
 * specific JSON file containing the user definition
 * (classpath:clds/clds-users.json).
 */
@Configuration
@EnableWebSecurity
@Profile("clamp-default-user")
public class DefaultUserConfiguration extends WebSecurityConfigurerAdapter {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(DefaultUserConfiguration.class);

    private static final String SETUP_WEB_USERS_EXCEPTION_MSG = "Exception occurred during the "
            + " setup of the Web users in memory";
    @Autowired
    private ClampProperties refProp;
    @Value("${clamp.config.security.permission.type.cl:permission-type-cl}")
    private String cldsPersmissionTypeCl;
    @Value("${CLDS_PERMISSION_INSTANCE:dev}")
    private String cldsPermissionInstance;
    @Value("${clamp.config.security.encoder:bcrypt}")
    private String cldsEncoderMethod;
    @Value("${clamp.config.security.encoder.bcrypt.strength:10}")
    private Integer cldsBcryptEncoderStrength;

    /**
     * This method configures on which URL the authorization will be enabled.
     */
    @Override
    protected void configure(HttpSecurity http) {
        try {
            // Do no remove the csrf as recommended by Sonar otherwise Put/post will not work
            // Moreover this default user class is only used by dev, on prod we use AAF and this code will be disabled
            http.csrf().disable().httpBasic().and().authorizeRequests().antMatchers("/restservices/clds/v1/user/**")
                    .authenticated().anyRequest().permitAll().and().sessionManagement()
                    .maximumSessions(1);

        } catch (Exception e) {
            logger.error(SETUP_WEB_USERS_EXCEPTION_MSG, e);
            throw new CldsUsersException(SETUP_WEB_USERS_EXCEPTION_MSG, e);
        }
    }

    /**
     * This method is called by the framework and is used to load all the users
     * defined in cldsUsersFile variable (this file path can be configured in the
     * application.properties).
     *
     * @param auth authentication manager builder
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        // configure algorithm used for password hashing
        final PasswordEncoder passwordEncoder = getPasswordEncoder();

        try {
            CldsUser[] usersList = loadUsers();
            // no users defined
            if (null == usersList) {
                logger.warn("No users defined. Users should be defined under clds-users.json");
                return;
            }
            for (CldsUser user : usersList) {
                auth.inMemoryAuthentication().withUser(user.getUser()).password(user.getPassword())
                        .authorities(user.getPermissionsString()).and().passwordEncoder(passwordEncoder);
            }
        } catch (Exception e) {
            logger.error(SETUP_WEB_USERS_EXCEPTION_MSG, e);
            throw new CldsUsersException(SETUP_WEB_USERS_EXCEPTION_MSG, e);
        }
    }

    /**
     * This method loads physically the JSON file and convert it to an Array of
     * CldsUser.
     *
     * @return The array of CldsUser
     * @throws IOException In case of the file is not found
     */
    private CldsUser[] loadUsers() throws IOException {
        logger.info("Load from clds-users.properties");
        return CldsUserJsonDecoder.decodeJson(refProp.getFileContent("files.cldsUsers"));
    }

    /**
     * This methods returns the chosen encoder for password hashing.
     */
    private PasswordEncoder getPasswordEncoder() {
        if ("bcrypt".equals(cldsEncoderMethod)) {
            return new BCryptPasswordEncoder(cldsBcryptEncoderStrength);
        } else {
            throw new CldsConfigException(
                    "Invalid clamp.config.security.encoder value. 'bcrypt' is the only option at this time.");
        }
    }
}