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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@EnableWebSecurity
public class CldsSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final Logger logger = Logger.getLogger(CldsSecurityConfig.class.getName());

    @Autowired
    private ApplicationContext appContext;

    @Value("${org.onap.clamp.config.files.cldsUsers:'classpath:etc/config/clds/clds-users.properties'}")
    private String cldsUsers;

    private final static String ROLEPREFIX = "null|null|";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .anyRequest().authenticated()
                .and()
            .formLogin()
                .loginPage("/login.html")
                .permitAll()
                .and()
            .logout()
                .permitAll();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        List<String> userList = loadUsers();

        // no users defined
        if (null == userList || userList.isEmpty()) {
            logger.log(Level.SEVERE, "No users defined. Users should be defined under clds/clds-users.properties.");
            return;
        }

        for (String user : userList) {
            String[] userInfo = user.split("[|]");
            if (userInfo.length != 3) {
                logger.log(Level.SEVERE, "Defined User(" + user + ") is not in good format.  User format should be:<username>|<password>|<role>. Role should be eiother 'read' or 'all'.");
                continue;
            }

            auth
                .inMemoryAuthentication()
                .withUser(userInfo[0]).password(userInfo[1]).roles(ROLEPREFIX + ("all".equalsIgnoreCase(userInfo[2]) ? "*" : userInfo[2]));

        }
    }

    private boolean validUser(String[] userInfo) {
        return ((userInfo != null) && (userInfo.length == 3) && (("all".equals(userInfo[2])) || ("read".equals(userInfo[2]))));
    }

    private List<String> loadUsers() throws Exception {
        logger.info("Load from clds-users.properties");

        Resource resource = appContext.getResource(cldsUsers);
        BufferedReader input = new BufferedReader(new InputStreamReader(resource.getInputStream()));

        List<String> userList = new LinkedList<>();

        String line;
        while ((line = input.readLine()) != null) {
            if (!line.contains("#")) {
                userList.add(line);
            }
            logger.info("line read:" + line);
        }
        return userList;
    }
}