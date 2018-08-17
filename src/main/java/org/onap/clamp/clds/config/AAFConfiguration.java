/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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
package org.onap.clamp.clds.config;

import javax.servlet.Filter;

import org.onap.clamp.clds.filter.ClampCadiFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Configuration
@Profile("clamp-aaf-authentication")
public class AAFConfiguration {

    /**
     * Method to return clamp cadi filter.
     *
     * @return Filter
     */
    @Bean(name = "cadiFilter")
    public Filter cadiFilter() {
        return new ClampCadiFilter();
    }

    /**
     * Method to register cadi filter.
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean cadiFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(cadiFilter());
        registration.addUrlPatterns("/restservices/*");
        //registration.addUrlPatterns("*");
        registration.setName("cadiFilter");
        registration.setOrder(0);
        return registration;
    }
}