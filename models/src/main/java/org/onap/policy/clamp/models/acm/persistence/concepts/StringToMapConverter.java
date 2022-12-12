/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

import java.util.HashMap;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.ws.rs.core.Response;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;

@Converter(autoApply = true)
public class StringToMapConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final Coder coder = new StandardCoder();

    @Override
    public String convertToDatabaseColumn(Map<String, Object> map) {
        try {
            return map == null ? null : coder.encode(map);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return new HashMap<>();
        }
        try {
            return coder.decode(dbData, Map.class);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
