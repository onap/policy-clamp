/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.http4.HttpClientConfigurer;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.onap.clamp.clds.util.ClampVersioning;
import org.onap.clamp.clds.util.ResourceFileUtils;
import org.onap.clamp.util.PassDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CamelConfiguration extends RouteBuilder {

    @Autowired
    CamelContext camelContext;

    @Autowired
    private Environment env;

    private void configureDefaultSslProperties() throws IOException {
        if (env.getProperty("server.ssl.trust-store") != null) {
            URL storeResource = Thread.currentThread().getContextClassLoader()
                .getResource(env.getProperty("server.ssl.trust-store").replaceFirst("classpath:", ""));
            System.setProperty("javax.net.ssl.trustStore", storeResource.getPath());
            String keyFile = env.getProperty("clamp.config.keyFile");
            String trustStorePass = PassDecoder.decode(env.getProperty("server.ssl.trust-store-password"),
                keyFile);
            System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
            System.setProperty("javax.net.ssl.trustStoreType", "jks");
            System.setProperty("ssl.TrustManagerFactory.algorithm", "PKIX");
            storeResource = Thread.currentThread().getContextClassLoader()
                .getResource(env.getProperty("server.ssl.key-store").replaceFirst("classpath:", ""));
            System.setProperty("javax.net.ssl.keyStore", storeResource.getPath());

            String keyStorePass = PassDecoder.decode(env.getProperty("server.ssl.key-store-password"),
                keyFile);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePass);
            System.setProperty("javax.net.ssl.keyStoreType", env.getProperty("server.ssl.key-store-type"));
        }
    }

    private void registerTrustStore()
        throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException, CertificateException, IOException {
        if (env.getProperty("server.ssl.trust-store") != null) {
            KeyStore truststore = KeyStore.getInstance("JKS");
            String keyFile = env.getProperty("clamp.config.keyFile");
            String password = PassDecoder.decode(env.getProperty("server.ssl.trust-store-password"), keyFile);
            truststore.load(
                    ResourceFileUtils.getResourceAsStream(env.getProperty("server.ssl.trust-store")),
                    password.toCharArray());

            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance("PKIX");
            trustFactory.init(truststore);
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, trustFactory.getTrustManagers(), null);
            SSLSocketFactory factory = new SSLSocketFactory(sslcontext, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();
            final Scheme scheme = new Scheme("https4", 443, factory);
            registry.register(scheme);
            ConnectionSocketFactory plainsf = PlainConnectionSocketFactory.getSocketFactory();
            HttpComponent http4 = camelContext.getComponent("https4", HttpComponent.class);
            http4.setHttpClientConfigurer(new HttpClientConfigurer() {

                @Override
                public void configureHttpClient(HttpClientBuilder builder) {
                    builder.setSSLSocketFactory(factory);
                    Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", factory).register("http", plainsf).build();
                    builder.setConnectionManager(new BasicHttpClientConnectionManager(registry));
                }
            });
        }
    }

    @Override
    public void configure()
        throws KeyManagementException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        restConfiguration().component("servlet").bindingMode(RestBindingMode.json).jsonDataFormat("clamp-gson")
            .dataFormatProperty("prettyPrint", "true")// .enableCORS(true)
            // turn on swagger api-doc
            .apiContextPath("api-doc").apiVendorExtension(true).apiProperty("api.title", "Clamp Rest API")
            .apiProperty("api.version", ClampVersioning.getCldsVersionFromProps())
            .apiProperty("base.path", "/restservices/clds/");

        // camelContext.setTracing(true);

        configureDefaultSslProperties();
        registerTrustStore();
    }
}
