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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;

import javax.servlet.FilterConfig;
import javax.servlet.ServletException;

import org.onap.aaf.cadi.config.Config;
import org.onap.aaf.cadi.filter.CadiFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

public class ClampCadiFilter extends CadiFilter {
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(ClampCadiFilter.class);

    @Autowired
    private ApplicationContext appContext;

    @Value("${server.ssl.key-store:#{null}}")
    private String keyStore;

    @Value("${clamp.config.cadi.cadiKeystorePassword:#{null}}")
    private String keyStorePass;

    @Value("${server.ssl.trust-store:#{null}}")
    private String trustStore;

    @Value("${clamp.config.cadi.cadiTruststorePassword:#{null}}")
    private String trustStorePass;

    @Value("${server.ssl.key-alias:clamp@clamp.onap.org}")
    private String alias;

    @Value("${clamp.config.cadi.keyFile:#{null}}")
    private String keyFile;

    @Value("${clamp.config.cadi.cadiLoglevel:#{null}}")
    private String cadiLoglevel;

    @Value("${clamp.config.cadi.cadiLatitude:#{null}}")
    private String cadiLatitude;

    @Value("${clamp.config.cadi.cadiLongitude:#{null}}")
    private String cadiLongitude;

    @Value("${clamp.config.cadi.aafLocateUrl:#{null}}")
    private String aafLocateUrl;

    @Value("${clamp.config.cadi.oauthTokenUrl:#{null}}")
    private String oauthTokenUrl;

    @Value("${clamp.config.cadi.oauthIntrospectUrl:#{null}}")
    private String oauthIntrospectUrl;

    @Value("${clamp.config.cadi.aafEnv:#{null}}")
    private String aafEnv;

    @Value("${clamp.config.cadi.aafUrl:#{null}}")
    private String aafUrl;

    @Value("${clamp.config.cadi.cadiX509Issuers:#{null}}")
    private String cadiX509Issuers;

    private void checkIfNullProperty(String key, String value) {
        /* When value is null, so not defined in application.properties
           set nothing in System properties */
        if (value != null) {
            /* Ensure that any properties already defined in System.prop by JVM params
                won't be overwritten by Spring application.properties values */
            System.setProperty(key, System.getProperty(key, value));
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // set some properties in System so that Cadi filter will find its config
        // The JVM values set will always overwrite the Spring ones.
        checkIfNullProperty(Config.CADI_KEYFILE, convertSpringToPath(keyFile));
        checkIfNullProperty(Config.CADI_LOGLEVEL, cadiLoglevel);
        checkIfNullProperty(Config.CADI_LATITUDE, cadiLatitude);
        checkIfNullProperty(Config.CADI_LONGITUDE, cadiLongitude);

        checkIfNullProperty(Config.AAF_LOCATE_URL, aafLocateUrl);
        checkIfNullProperty(Config.AAF_OAUTH2_TOKEN_URL, oauthTokenUrl);
        checkIfNullProperty(Config.AAF_OAUTH2_INTROSPECT_URL, oauthIntrospectUrl);

        checkIfNullProperty(Config.AAF_ENV, aafEnv);
        checkIfNullProperty(Config.AAF_URL, aafUrl);
        checkIfNullProperty(Config.CADI_X509_ISSUERS, cadiX509Issuers);
        checkIfNullProperty(Config.CADI_KEYSTORE, convertSpringToPath(keyStore));
        checkIfNullProperty(Config.CADI_TRUSTSTORE, convertSpringToPath(trustStore));
        checkIfNullProperty(Config.CADI_ALIAS, alias);
        checkIfNullProperty(Config.CADI_KEYSTORE_PASSWORD, keyStorePass);
        checkIfNullProperty(Config.CADI_TRUSTSTORE_PASSWORD, trustStorePass);

        super.init(filterConfig);
    }

    private String convertSpringToPath(String fileName) {
        try (InputStream ioFile = appContext.getResource(fileName).getInputStream()) {
            if (!fileName.contains("file:")) {
                File targetFile = new File(appContext.getResource(fileName).getFilename());
                java.nio.file.Files.copy(ioFile, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return targetFile.getPath();
            } else {
                return appContext.getResource(fileName).getFile().getPath();
            }
        } catch (IOException e) {
            logger.error("Unable to open and copy the file: " + fileName, e);
            return null;
        }

    }
}
