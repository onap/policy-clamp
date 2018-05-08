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
package org.onap.clamp.clds.filter;

import javax.servlet.FilterConfig;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.beans.factory.annotation.Value;

import org.onap.aaf.cadi.filter.CadiFilter;
import org.onap.clamp.clds.config.AAFConfiguration;

public class ClampCadiFilter extends CadiFilter {
    private static final String CADI_TRUST_STORE = "cadi_truststore";
    private static final String CADI_TRUST_STORE_PW = "cadi_truststore_password";
    private static final String CADI_KEY_STORE = "cadi_keystore";
    private static final String CADI_KEY_STORE_PW = "cadi_keystore_password";
    private static final String ALIAS = "cadi_alias";

    @Value("${server.ssl.key-store:none}")
    private String              keyStore;
    
    @Value("${clamp.config.cadi.cadiKeystorePassword:none}")
    private String              keyStorePass;

    @Value("${server.ssl.trust:none}")
    private String              trustStore;
    
    @Value("${clamp.config.cadi.cadiTruststorePassword:none}")
    private String              trustStorePass;

    @Value("${server.ssl.key-alias:clamp@clamp.onap.org}")
    private String              alias;

    @Autowired
    private AAFConfiguration aafConfiguration;
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Properties props = aafConfiguration.getProperties();
        props.setProperty(CADI_KEY_STORE, trimFileName(keyStore));
        props.setProperty(CADI_TRUST_STORE, trimFileName(trustStore));
        props.setProperty(ALIAS, alias);
        props.setProperty(CADI_KEY_STORE_PW,  keyStorePass);
        props.setProperty(CADI_TRUST_STORE_PW, trustStorePass);

        super.init(filterConfig);
    }

    private String trimFileName (String fileName) {
        int index= fileName.indexOf("file:");
        if (index == -1) { 
            return fileName;
        } else {
            return fileName.substring(index+5);
        }
    }
}
