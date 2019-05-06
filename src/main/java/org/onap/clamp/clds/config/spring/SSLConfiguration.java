/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * ===================================================================
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

package org.onap.clamp.clds.config.spring;

import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SSLConfiguration {
    @Autowired
    private Environment env;

    @PostConstruct
    private void configureSSL() {
        if (env.getProperty("server.ssl.trust-store") != null) {
            URL storeResource = SSLConfiguration.class
                .getResource(env.getProperty("server.ssl.trust-store").replaceAll("classpath:", ""));
            System.setProperty("javax.net.ssl.trustStore", storeResource.getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", env.getProperty("server.ssl.trust-store-password"));
            System.setProperty("javax.net.ssl.trustStoreType", env.getProperty("server.ssl.key-store-type"));

            storeResource = SSLConfiguration.class
                .getResource(env.getProperty("server.ssl.key-store").replaceAll("classpath:", ""));
            System.setProperty("javax.net.ssl.keyStore", storeResource.getPath());
            System.setProperty("javax.net.ssl.keyStorePassword", env.getProperty("server.ssl.key-store-password"));
            System.setProperty("javax.net.ssl.keyStoreType", env.getProperty("server.ssl.key-store-type"));
        }
    }
}