/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;

@Converter(autoApply = true)
public class StringToDocMessage extends AbstractConverter implements AttributeConverter<DocMessage, String> {

    @Override
    public String convertToDatabaseColumn(DocMessage docMessage) {
        return encode(docMessage);
    }

    @Override
    public DocMessage convertToEntityAttribute(String message) {
        if (message == null) {
            return new DocMessage();
        }
        return decode(message, DocMessage.class);
    }
}
