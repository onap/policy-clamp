/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2018, 2021 AT&T Intellectual Property. All rights
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
 */

package org.onap.policy.clamp.clds.config;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http.HttpComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.onap.policy.clamp.clds.util.ClampVersioning;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.util.PassDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CamelConfiguration extends RouteBuilder {

    private static final String HTTP = "http";
    private static final String HTTPS = "https";

    @Autowired
    CamelContext camelContext;

    @Value("${server.ssl.key-store:#{null}}")
    private String keyStore;

    @Value("${server.ssl.key-store-type:JKS}")
    private String keyStoreType;

    @Value("${server.ssl.key-store-password:#{null}}")
    private String keyStorePass;

    @Value("${server.ssl.trust-store:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trust-store-password:#{null}}")
    private String trustStorePass;

    @Value("${server.ssl.trust-store-type:JKS}")
    private String trustStoreType;

    @Value("${server.ssl.trust-store-algorithm:PKIX}")
    private String trustStoreAlgorithm;

    @Value("${clamp.config.httpclient.connectTimeout:-1}")
    private int connectTimeout;

    @Value("${clamp.config.httpclient.connectRequestTimeout:-1}")
    private int connectRequestTimeout;

    @Value("${clamp.config.httpclient.socketTimeout:-1}")
    private int socketTimeout;

    @Value("${clamp.config.keyFile:#{null}}")
    private String keyFile;

    private void configureDefaultSslProperties() {
        if (trustStore != null) {
            System.setProperty("javax.net.ssl.trustStore", Thread.currentThread().getContextClassLoader()
                    .getResource(trustStore.replaceFirst("classpath:", "")).getPath());
            System.setProperty("javax.net.ssl.trustStorePassword", Objects.requireNonNull(
                    PassDecoder.decode(trustStorePass, keyFile)));
            System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
            System.setProperty("ssl.TrustManagerFactory.algorithm", trustStoreAlgorithm);
        }
        if (keyStore != null) {
            System.setProperty("javax.net.ssl.keyStore", Thread.currentThread().getContextClassLoader()
                    .getResource(keyStore.replaceFirst("classpath:", "")).getPath());
            System.setProperty("javax.net.ssl.keyStorePassword", Objects.requireNonNull(
                    PassDecoder.decode(keyStorePass, keyFile)));
            System.setProperty("javax.net.ssl.keyStoreType", keyStoreType);
        }

    }

    private void configureCamelHttpComponent()
            throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException,
            IOException {
        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectRequestTimeout)
                .setSocketTimeout(socketTimeout).build();

        if (trustStore != null) {
            var truststore = KeyStore.getInstance(trustStoreType);
            truststore.load(
                    ResourceFileUtils.getResourceAsStream(trustStore),
                    Objects.requireNonNull(PassDecoder.decode(trustStorePass, keyFile)).toCharArray());
            var trustFactory = TrustManagerFactory.getInstance(trustStoreAlgorithm);
            trustFactory.init(truststore);
            var sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustFactory.getTrustManagers(), null);
            camelContext.getComponent(HTTPS, HttpComponent.class).setHttpClientConfigurer(builder -> {
                var factory = new SSLSocketFactory(sslcontext,
                        SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
                builder.setSSLSocketFactory(factory);
                builder.setConnectionManager(new BasicHttpClientConnectionManager(
                        RegistryBuilder.<ConnectionSocketFactory>create().register(HTTPS, factory).build()))
                        .setDefaultRequestConfig(requestConfig);
            });
        }
        camelContext.getComponent(HTTP, HttpComponent.class).setHttpClientConfigurer(builder -> {
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register(HTTP, PlainConnectionSocketFactory.getSocketFactory()).build();
            builder.setConnectionManager(new BasicHttpClientConnectionManager(registry))
                    .setDefaultRequestConfig(requestConfig);
        });
    }

    @Override
    public void configure()
            throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException {
        restConfiguration().component("servlet").bindingMode(RestBindingMode.json).jsonDataFormat("clamp-gson")
                .dataFormatProperty("prettyPrint", "true")// .enableCORS(true)
                // turn on swagger api-doc
                .apiContextPath("api-doc").apiVendorExtension(true).apiProperty("api.title", "Clamp Rest API")
                .apiProperty("api.version", ClampVersioning.getCldsVersionFromProps())
                .apiProperty("base.path", "/restservices/clds/");

        // Configure httpClient properties for Camel HTTP/HTTPS calls
        configureDefaultSslProperties();
        configureCamelHttpComponent();
    }
}
