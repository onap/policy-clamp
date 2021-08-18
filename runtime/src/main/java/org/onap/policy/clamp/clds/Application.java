/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018, 2021 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.clds;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.apache.camel.component.servlet.springboot.ServletMappingAutoConfiguration;
import org.apache.catalina.connector.Connector;
import org.onap.policy.clamp.clds.util.ClampVersioning;
import org.onap.policy.clamp.clds.util.ResourceFileUtils;
import org.onap.policy.clamp.util.PassDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ComponentScan(basePackages = {"org.onap.policy.clamp"})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class,
    ServletMappingAutoConfiguration.class})
@EnableJpaRepositories(basePackages = {"org.onap.policy.clamp"})
@EntityScan(basePackages = {"org.onap.policy.clamp"})
@EnableTransactionManagement
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
@EnableJpaAuditing
public class Application extends SpringBootServletInitializer {

    protected static final Logger appLogger = LoggerFactory.getLogger(Application.class);
    // This settings is an additional one to Spring config,
    // only if we want to have an additional port automatically redirected to
    // HTTPS
    @Value("${server.http-to-https-redirection.port:#{null}}")
    private String httpRedirectedPort;
    /**
     * This 8080 is the default port used by spring if this parameter is not
     * specified in application.properties.
     */
    @Value("${server.port:8080}")
    private String springServerPort;

    @Value("${server.ssl.key-store:#{null}}")
    private String keystoreFile;

    @Value("${server.ssl.key-store-password:#{null}}")
    private String keyStorePass;

    @Value("${server.ssl.key-store-type:JKS}")
    private String keyStoreType;


    @Value("${clamp.config.keyFile:classpath:/clds/aaf/org.onap.clamp.keyfile}")
    private String keyFile;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    /**
     * Main method that starts the Clamp backend.
     *
     * @param args app params
     */
    public static void main(String[] args) {
        // Start the Spring application
        SpringApplication.run(Application.class, args);
    }

    /**
     * This method is used to declare the camel servlet.
     *
     * @return A servlet bean
     * @throws IOException IO Exception
     */
    @Bean
    public ServletRegistrationBean<ClampServlet> camelServletRegistrationBean() throws IOException {
        appLogger.info("{} (v {} ), {}, {}", ResourceFileUtils.getResourceAsString("boot-message.txt"),
            ClampVersioning.getCldsVersionFromProps(), System.getProperty("line.separator"),
            getSslExpirationDate());
        var registration = new ServletRegistrationBean<ClampServlet>(new ClampServlet(), "/restservices/clds/*");
        registration.setName("CamelServlet");
        return registration;
    }

    /**
     * This method is used by Spring to create the servlet container factory.
     *
     * @return The TomcatEmbeddedServletContainerFactory just created
     */
    @Bean
    public ServletWebServerFactory getEmbeddedServletContainerFactory() {
        var tomcat = new TomcatServletWebServerFactory();
        if (httpRedirectedPort != null && keystoreFile != null) {
            // Automatically redirect to HTTPS
            tomcat = new TomcatEmbeddedServletContainerFactoryRedirection();
            var newConnector = createRedirectConnector(Integer.parseInt(springServerPort));
            if (newConnector != null) {
                tomcat.addAdditionalTomcatConnectors(newConnector);
            }
        }
        return tomcat;
    }

    private Connector createRedirectConnector(int redirectSecuredPort) {
        if (redirectSecuredPort <= 0) {
            appLogger.warn(
                    "HTTP port redirection to HTTPS is disabled because the HTTPS"
                    + " port is 0 (random port) or -1 (Connector disabled)");
            return null;
        }
        var connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setSecure(false);
        connector.setPort(Integer.parseInt(httpRedirectedPort));
        connector.setRedirectPort(redirectSecuredPort);
        return connector;
    }

    private String getSslExpirationDate() throws IOException {
        var result = new StringBuilder("   :: SSL Certificates ::     ");
        try {
            if (keystoreFile != null) {
                var keystore = KeyStore.getInstance(keyStoreType);
                keystore.load(ResourceFileUtils.getResourceAsStream(keystoreFile.replace("classpath:", "")),
                    PassDecoder.decode(keyStorePass, keyFile).toCharArray());

                Enumeration<String> aliases = keystore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    if ("X.509".equals(keystore.getCertificate(alias).getType())) {
                        result.append("* " + alias + " expires "
                            + ((X509Certificate) keystore.getCertificate(alias)).getNotAfter()
                            + System.getProperty("line.separator"));
                    }
                }
            } else {
                result.append("* NONE HAS been configured");
            }
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
            appLogger.warn("SSL certificate access error", e);

        }
        return result.toString();
    }
}
