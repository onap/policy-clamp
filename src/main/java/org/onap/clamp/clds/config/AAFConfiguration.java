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

import java.util.Properties;

import javax.servlet.Filter;

import org.onap.clamp.clds.filter.ClampCadiFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Configuration
@Profile("clamp-aaf-authentication")
@ConfigurationProperties(prefix = "clamp.config.cadi")
public class AAFConfiguration {
    private static final String CADI_KEY_FILE = "cadi_keyfile";
    private static final String CADI_LOG_LEVEL = "cadi_loglevel";
    private static final String LATITUDE = "cadi_latitude";
    private static final String LONGITUDE = "cadi_longitude";
    private static final String LOCATE_URL = "aaf_locate_url";
    private static final String OAUTH_TOKEN_URL = "aaf_oauth2_token_url";
    private static final String OAUTH_INTROSPECT_URL = "aaf_oauth2_introspect_url";
    private static final String AAF_ENV = "aaf_env";
    private static final String AAF_URL = "aaf_url";
    private static final String X509_ISSUERS = "cadi_x509_issuers";
	
    private String              keyFile;
    private String              cadiLoglevel;
    private String              cadiLatitude;
    private String              cadiLongitude;
    private String              aafLocateUrl;
    private String              oauthTokenUrl;
    private String              oauthIntrospectUrl;
    private String              aafEnv;
    private String              aafUrl;
    private String              cadiX509Issuers;

    /**
     * Method to return clamp cadi filter.
     * 
     * @return Filter
     */
    @Bean(name = "cadiFilter")
    public Filter cadiFilter() {
        return new ClampCadiFilter();
    }

    /**
     * Method to register cadi filter.
     * 
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean cadiFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(cadiFilter());
        registration.addUrlPatterns("/restservices/*");
        //registration.addUrlPatterns("*");
        registration.setName("cadiFilter");
        registration.setOrder(0);
        return registration;
    }

	public String getKeyFile() {
		return keyFile;
	}

	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	public String getCadiLoglevel() {
		return cadiLoglevel;
	}

	public void setCadiLoglevel(String cadiLoglevel) {
		this.cadiLoglevel = cadiLoglevel;
	}

	public String getCadiLatitude() {
		return cadiLatitude;
	}

	public void setCadiLatitude(String cadiLatitude) {
		this.cadiLatitude = cadiLatitude;
	}

	public String getCadiLongitude() {
		return cadiLongitude;
	}

	public void setCadiLongitude(String cadiLongitude) {
		this.cadiLongitude = cadiLongitude;
	}

	public String getAafLocateUrl() {
		return aafLocateUrl;
	}

	public void setAafLocateUrl(String aafLocateUrl) {
		this.aafLocateUrl = aafLocateUrl;
	}

	public String getOauthTokenUrl() {
		return oauthTokenUrl;
	}

	public void setOauthTokenUrl(String oauthTokenUrl) {
		this.oauthTokenUrl = oauthTokenUrl;
	}

	public String getOauthIntrospectUrl() {
		return oauthIntrospectUrl;
	}

	public void setOauthIntrospectUrl(String oauthIntrospectUrl) {
		this.oauthIntrospectUrl = oauthIntrospectUrl;
	}

	public String getAafEnv() {
		return aafEnv;
	}

	public void setAafEnv(String aafEnv) {
		this.aafEnv = aafEnv;
	}

	public String getAafUrl() {
		return aafUrl;
	}

	public void setAafUrl(String aafUrl) {
		this.aafUrl = aafUrl;
	}

	public String getCadiX509Issuers() {
		return cadiX509Issuers;
	}

	public void setCadiX509Issuers(String cadiX509Issuers) {
		this.cadiX509Issuers = cadiX509Issuers;
	}

	public Properties getProperties() {
        Properties prop = System.getProperties();
        //prop.put("cadi_prop_files", "");
        prop.put(CADI_KEY_FILE, keyFile);
        prop.put(CADI_LOG_LEVEL, cadiLoglevel);
        prop.put(LATITUDE, cadiLatitude);
        prop.put(LONGITUDE, cadiLongitude);
        prop.put(LOCATE_URL, aafLocateUrl);
        if (oauthTokenUrl != null) {
            prop.put(OAUTH_TOKEN_URL, oauthTokenUrl);
        }
        if (oauthIntrospectUrl != null) {
            prop.put(OAUTH_INTROSPECT_URL, oauthIntrospectUrl);
        }
        prop.put(AAF_ENV, aafEnv);
        prop.put(AAF_URL, aafUrl);
        prop.put(X509_ISSUERS, cadiX509Issuers);
        return prop;
    }
}