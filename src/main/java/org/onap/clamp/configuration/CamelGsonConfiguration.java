/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.clamp.configuration;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.annotations.Expose;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.camel.component.gson.GsonDataFormat;
import org.apache.camel.spi.DataFormatCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CamelGsonConfiguration {

    @Bean
    public List<DataFormatCustomizer<GsonDataFormat>> provideGsonCustomizers() {
        DataFormatCustomizer<GsonDataFormat> dataFormatCustomizer = dataformat ->
            dataformat.setExclusionStrategies(
                Collections.singletonList(new ExcludeFieldsWithoutExposedAnnotation())
            );
        return Collections.singletonList(dataFormatCustomizer);
    }

    private static class ExcludeFieldsWithoutExposedAnnotation implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return Optional.ofNullable(f.getAnnotation(Expose.class))
                .map(expose -> !expose.serialize())
                .orElse(true);
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
