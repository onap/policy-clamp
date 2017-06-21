/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.config;

import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.camunda.bpm.spring.boot.starter.CamundaBpmAutoConfiguration;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureAfter(CamundaBpmAutoConfiguration.class)
public class CamundaAuthFilterInitializer implements ServletContextInitializer {

	private static final EnumSet<DispatcherType> DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

	private static final String AJSC_CADI_PROPS_FILE = "cadi.properties";

	private ServletContext servletContext;

	@Value("${com.att.ajsc.camunda.contextPath:/camunda}")
	private String CAMUNDA_SUFFIX;

	private static final Logger log = Logger.getLogger(CamundaAuthFilterInitializer.class.getName());

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		this.servletContext = servletContext;

		registerFilter("Authentication Filter", AuthenticationFilter.class, CAMUNDA_SUFFIX + "/*");
	}

	private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass,
			final String... urlPatterns) {
		return registerFilter(filterName, filterClass, null, urlPatterns);
	}

	private FilterRegistration registerFilter(final String filterName, final Class<? extends Filter> filterClass,
			final Map<String, String> initParameters, final String... urlPatterns) {
		FilterRegistration filterRegistration = servletContext.getFilterRegistration(filterName);

		if (filterRegistration == null) {
			filterRegistration = servletContext.addFilter(filterName, filterClass);
			filterRegistration.addMappingForUrlPatterns(DISPATCHER_TYPES, true, urlPatterns);

			if (initParameters != null) {
				filterRegistration.setInitParameters(initParameters);
			}
		}

		return filterRegistration;
	}
}
