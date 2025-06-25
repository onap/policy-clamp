/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import jakarta.ws.rs.core.Response;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;

public abstract class AbstractConverter {

    private static final Coder coder = new StandardCoder();

    protected  <T> String encode(final T object) {
        try {
            return object == null ? null : coder.encode(object);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

    protected <T> T decode(String message, Class<T> clazz) {
        try {
            return coder.decode(message, clazz);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Converts an Object to a typed Object.
     *
     * @param obj the Object
     * @param clazz the Class
     * @return the object converted
     */
    public static <T> T convertObject(Object obj, Class<T> clazz) {
        try {
            var json = coder.encode(obj);
            return coder.decode(json, clazz);
        } catch (CoderException e) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
