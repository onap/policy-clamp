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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */
package org.onap.clamp.clds.config;

import javax.servlet.Filter;

import org.onap.clamp.clds.filter.ClampDefaultUserFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("clamp-default-user")
public class DefaultUserConfiguration {

    /**
     * Method to return clamp default user filter.
     * 
     * @return Filter
     */
    @Bean(name = "defaultUserFilter")
    public Filter defaultUserFilter() {
        return new ClampDefaultUserFilter();
    }

    /**
     * Method to register defaultUserFilter.
     * 
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean defaultUserFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(defaultUserFilter());
        registration.addUrlPatterns("/restservices/*");
        registration.setName("defaultUserFilter");
        registration.setOrder(0);
        return registration;
    }

}