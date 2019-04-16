/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.dao.model.jsontype;

import com.google.gson.JsonObject;

import java.io.Serializable;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.onap.clamp.clds.util.JsonUtils;

public class JsonTypeDescriptor extends AbstractTypeDescriptor<JsonObject> {

    /**
     * The serial version ID.
     */
    private static final long serialVersionUID = -3439698221196089003L;

    public static final JsonTypeDescriptor INSTANCE = new JsonTypeDescriptor();

    /**
     * Creates an instance of JsonTypeDescriptor.
     */
    public JsonTypeDescriptor() {
        super(JsonObject.class, new ImmutableMutabilityPlan<JsonObject>() {

            /**
             * The serial version ID.
             */
            private static final long serialVersionUID = 1169396106518110214L;

            @Override
            public Serializable disassemble(JsonObject value) {
                return JsonUtils.GSON_JPA_MODEL.toJson(value);
            }

            @Override
            public JsonObject assemble(Serializable cached) {
                return JsonUtils.GSON_JPA_MODEL.fromJson((String) cached, JsonObject.class);
            }

        });
    }

    @Override
    public String toString(JsonObject value) {
        return JsonUtils.GSON_JPA_MODEL.toJson(value);
    }

    @Override
    public JsonObject fromString(String string) {
        return JsonUtils.GSON_JPA_MODEL.fromJson(string, JsonObject.class);
    }

    @Override
    public <X> X unwrap(JsonObject value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }

        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }

        if (JsonObject.class.isAssignableFrom(type)) {
            return (X) JsonUtils.GSON_JPA_MODEL.toJson(toString(value));
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> JsonObject wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }

        if (String.class.isInstance(value)) {
            return JsonUtils.GSON_JPA_MODEL.fromJson((String) value, JsonObject.class);
        }

        throw unknownWrap(value.getClass());
    }

}
