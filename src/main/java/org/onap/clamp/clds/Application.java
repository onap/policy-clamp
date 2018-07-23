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

package org.onap.clamp.clds;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;
import org.apache.catalina.connector.Connector;
import org.onap.clamp.clds.model.properties.Holmes;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
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
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@ComponentScan(basePackages = {
    "org.onap.clamp.clds"
})
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class,
    SecurityAutoConfiguration.class,UserDetailsServiceAutoConfiguration .class
})
@EnableConfigurationProperties
@EnableAsync
@EnableScheduling
public class Application extends SpringBootServletInitializer {

    protected static final EELFLogger EELF_LOGGER = EELFManager.getInstance().getLogger(Application.class);
    // This settings is an additional one to Spring config,
    // only if we want to have an additional port automatically redirected to
    // HTTPS
    @Value("${server.http-to-https-redirection.port:none}")
    private String httpRedirectedPort;
    /**
     * This 8080 is the default port used by spring if this parameter is not
     * specified in application.properties.
     */
    @Value("${server.port:8080}")
    private String springServerPort;
    @Value("${server.ssl.key-store:none}")
    private String sslKeystoreFile;

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Application.class);
    }

    public static void main(String[] args) {
        // This is to initialize some Onap Clamp components
        initializeComponents();
        // Start the Spring application
        SpringApplication.run(Application.class, args);
    }

    private static void initializeComponents() {
        ModelProperties.registerModelElement(Holmes.class, Holmes.getType());
    }

    /**
     * This method is used to declare the camel servlet.
     *
     * @return A servlet bean
     */
    @Bean
    public ServletRegistrationBean camelServletRegistrationBean() {
        ServletRegistrationBean registration = new ServletRegistrationBean(new ClampServlet(),
            "/restservices/clds/v1/*");
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
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        if (!"none".equals(httpRedirectedPort) && !"none".equals(sslKeystoreFile)) {
            // Automatically redirect to HTTPS
            tomcat = new TomcatEmbeddedServletContainerFactoryRedirection();
            Connector newConnector = createRedirectConnector(Integer.parseInt(springServerPort));
            if (newConnector != null) {
                tomcat.addAdditionalTomcatConnectors(newConnector);
            }
        }
        return tomcat;
    }

    private Connector createRedirectConnector(int redirectSecuredPort) {
        if (redirectSecuredPort <= 0) {
            EELF_LOGGER.warn(
                "HTTP port redirection to HTTPS is disabled because the HTTPS port is 0 (random port) or -1 (Connector disabled)");
            return null;
        }
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setSecure(false);
        connector.setPort(Integer.parseInt(httpRedirectedPort));
        connector.setRedirectPort(redirectSecuredPort);
        return connector;
    }
}
