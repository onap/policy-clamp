/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.utils.coder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MapperFactory {

    /**
     *  Create new Object Mapper for json.
     *
     * @return a new Mapper
     */
    public static ObjectMapper createJsonMapper() {
        var mapper = new ObjectMapper();
        configure(mapper);
        return mapper;
    }

    /**
     *  Create new Yaml Mapper.
     *
     * @return a new Yaml Mapper
     */
    public static YAMLMapper createYamlMapper() {
        var mapper = new YAMLMapper();
        configure(mapper);
        return mapper;
    }

    private static void configure(ObjectMapper mapper) {
        // Configure to handle empty beans (like test classes with no getters/setters)
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // Configure to ignore unknown properties (similar to Gson behavior)
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Configure to handle null values more gracefully
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        // Configure to handle circular references - disable self-reference detection entirely
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false);
        // Don't write self references as null, just ignore them
        mapper.configure(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL, false);
        // Ignore null fields during serialization
        mapper.setDefaultPropertyInclusion(
                JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        // Register modules for Java 8 time support (JSR310)
        mapper.findAndRegisterModules();
    }
}
