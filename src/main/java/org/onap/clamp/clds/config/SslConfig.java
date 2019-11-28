/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.clamp.util.PassDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@Configuration
@Profile("clamp-ssl-config")
public class SslConfig {
    @Autowired
    private Environment env;

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer(ServerProperties serverProperties,
            ResourceLoader resourceLoader) {
        return (tomcat) -> tomcat.setSslStoreProvider(new SslStoreProvider() {
            @Override
            public KeyStore getKeyStore() throws KeyStoreException, 
                    NoSuchAlgorithmException, CertificateException, IOException {
                KeyStore keystore = KeyStore.getInstance(env.getProperty("server.ssl.key-store-type"));
                String password = PassDecoder.decode(env.getProperty("server.ssl.key-store-password"), 
                    env.getProperty("clamp.config.keyFile"));
                String keyStore = env.getProperty("server.ssl.key-store");
                InputStream is = ResourceFileUtil.getResourceAsStream(keyStore.replaceAll("classpath:", ""));
                keystore.load(is, password.toCharArray());
                return keystore;
            }

            @Override
            public KeyStore getTrustStore() throws KeyStoreException, 
                    NoSuchAlgorithmException, CertificateException, IOException {
                    KeyStore truststore = KeyStore.getInstance("JKS");
                    String password = PassDecoder.decode(env.getProperty("server.ssl.trust-store-password"), 
                            env.getProperty("clamp.config.keyFile"));
                    truststore.load(
                        Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(env.getProperty("server.ssl.trust-store")
                                .replaceAll("classpath:", "")),
                            password.toCharArray());
                    return truststore;
            }
        });
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatSslCustomizer(ServerProperties serverProperties,
            ResourceLoader resourceLoader) {
        return (tomcat) -> tomcat.setSsl(new Ssl() {
            @Override
            public String getKeyPassword() {
                    String password = PassDecoder.decode(env.getProperty("server.ssl.key-password"), 
                            env.getProperty("clamp.config.keyFile"));
                    return password;
            }
        });
    }
}