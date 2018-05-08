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

import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SSLConfiguration {
    private static final String TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PW = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";

    @Value("${server.ssl.trust:/opt/app/osaaf/client/local/truststoreONAP.p12}")
    private String sslTruststoreFile;
    @Value("${server.ssl.trust-password:changeit}")
    private String sslTruststorePw;
    @Value("${server.ssl.trust-type:PKCS12}")
    private String sslTruststoreType;

    @PostConstruct
    private void configureSSL() {
        if (!sslTruststoreFile.equals("none")) {
            System.setProperty(TRUST_STORE, sslTruststoreFile);
        }
        if (!sslTruststoreType.equals("none")) {
            System.setProperty(TRUST_STORE_TYPE, sslTruststoreType);
        }
        if (!sslTruststorePw.equals("none")) {
            System.setProperty(TRUST_STORE_PW, sslTruststorePw);
        }
    }
}
