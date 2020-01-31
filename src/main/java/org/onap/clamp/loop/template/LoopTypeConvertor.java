/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.loop.template;

import java.util.stream.Stream;
import javax.persistence.AttributeConverter;

/**
 * Attribute Converter to allow using LoopType Enum values in DB and Java classes.
 *
 */
public class LoopTypeConvertor implements AttributeConverter<LoopType, String> {

    @Override
    public String convertToDatabaseColumn(LoopType loopType) {
        if (loopType == null) {
            return null;
        }
        return loopType.getValue();
    }

    @Override
    public LoopType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return Stream.of(LoopType.values()).filter(c -> c.getValue().equals(value)).findFirst()
            .orElseThrow(IllegalArgumentException::new);
    }
}
