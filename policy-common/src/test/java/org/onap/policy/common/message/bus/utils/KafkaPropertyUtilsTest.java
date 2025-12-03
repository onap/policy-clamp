/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.message.bus.utils;

import static org.onap.policy.common.message.bus.properties.MessageBusProperties.PROPERTY_ADDITIONAL_PROPS_SUFFIX;

import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.utils.properties.PropertyUtils;

class KafkaPropertyUtilsTest {

    @Test
    void test() {
        var properties = new Properties();
        properties.setProperty("mytopic" + PROPERTY_ADDITIONAL_PROPS_SUFFIX, "{444-");
        PropertyUtils props = new PropertyUtils(properties, "mytopic", null);

        var build = KafkaPropertyUtils.makeBuilder(props, "mytopic", "servers").build();
        Assertions.assertTrue(build.getAdditionalProps().isEmpty());

        properties.setProperty("mytopic" + PROPERTY_ADDITIONAL_PROPS_SUFFIX,
            "{\"security.protocol\": \"SASL_PLAINTEXT\"}");
        build = KafkaPropertyUtils.makeBuilder(props, "mytopic", "servers").build();
        Assertions.assertTrue(build.getAdditionalProps().containsKey("security.protocol"));

        properties.setProperty("mytopic" + PROPERTY_ADDITIONAL_PROPS_SUFFIX,
            "{\"security.protocol\": false }");
        build = KafkaPropertyUtils.makeBuilder(props, "mytopic", "servers").build();
        Assertions.assertTrue(build.getAdditionalProps().isEmpty());

        properties.setProperty("mytopic" + PROPERTY_ADDITIONAL_PROPS_SUFFIX, "");
        build = KafkaPropertyUtils.makeBuilder(props, "mytopic", "servers").build();
        Assertions.assertTrue(build.getAdditionalProps().isEmpty());
    }
  
}