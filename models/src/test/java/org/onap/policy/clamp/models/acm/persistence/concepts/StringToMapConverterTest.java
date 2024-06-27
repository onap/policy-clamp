/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.concepts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.models.base.PfModelRuntimeException;

class StringToMapConverterTest {

    @Test
    void testConvert() {
        var stringToMapConverter = new StringToMapConverter();
        Map<String, Object> map =  Map.of("key", List.of("value"));
        var dbData = stringToMapConverter.convertToDatabaseColumn(map);
        var result = stringToMapConverter.convertToEntityAttribute(dbData);
        assertEquals(map, result);
    }

    @Test
    void testNull() {
        var stringToMapConverter = new StringToMapConverter();
        var dbData = stringToMapConverter.convertToDatabaseColumn(null);
        assertThat(dbData).isNull();
        var map = stringToMapConverter.convertToEntityAttribute(null);
        assertThat(map).isNotNull();
        assertThatThrownBy(() -> stringToMapConverter.convertToEntityAttribute("1"))
                .isInstanceOf(PfModelRuntimeException.class);
    }
}
