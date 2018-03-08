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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.service;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
@ApplicationPath("/restservices/clds/v1")
public class JaxrsApplication extends Application {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(JaxrsApplication.class);

    private Function<BeanDefinition, Optional<Class<?>>> beanDefinitionToClass = b -> {
        try {
            return Optional.of(Class.forName(b.getBeanClassName()));
        } catch (ClassNotFoundException e) {
            logger.error("Could not get class annotated with @Path for swagger documentation generation", e);
            return Optional.empty();
        }
    };

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        resources.add(io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class);
        resources.addAll(scan());
        return resources;
    }

    private List<Class<?>> scan() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(javax.ws.rs.Path.class));
        return scanner.findCandidateComponents("org.onap.clamp.clds").stream()
                .map(beanDefinitionToClass)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

}