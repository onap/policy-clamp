/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2022-2023 Nordix Foundation.
 * ======================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================LICENSE_END===================================
 */

package org.onap.policy.clamp.acm.participant.a1pms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configure how access to this module's REST end points is secured.
 */
@Configuration
public class SecurityConfig {
    /**
     * Return the configuration of how access to this module's REST end points is secured.
     *
     * @param http the HTTP security settings
     * @return the HTTP security settings
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .httpBasic()
            .and()
            .authorizeHttpRequests().anyRequest().authenticated()
            .and()
            .csrf().disable();
        return http.build();
    }
}
