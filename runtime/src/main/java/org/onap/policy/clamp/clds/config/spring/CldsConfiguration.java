/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.config.spring;

import org.onap.policy.clamp.clds.config.ClampProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("clamp-default")
public class CldsConfiguration {

    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private ClampProperties refProp;


    /**
     * This loads the file system.properties.
     *
     * @return The PropertiesFactoryBean
     */
    @Bean(name = "mapper")
    public PropertiesFactoryBean mapper() {
        var bean = new PropertiesFactoryBean();
        bean.setLocation(appContext.getResource(refProp.getStringValue("files.systemProperties")));
        return bean;
    }
}
