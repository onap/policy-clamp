/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
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
import jakarta.ws.rs.core.Response;
import org.onap.policy.clamp.models.acm.document.concepts.DocMessage;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;

@Converter(autoApply = true)
public class StringToDocMessage implements AttributeConverter<DocMessage, String> {

    private static final Coder coder = new StandardCoder();

    @Override
    public String convertToDatabaseColumn(DocMessage docMessage) {
        try {
            return docMessage == null ? null : coder.encode(docMessage);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public DocMessage convertToEntityAttribute(String message) {
        if (message == null) {
            return new DocMessage();
        }
        try {
            return coder.decode(message, DocMessage.class);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
