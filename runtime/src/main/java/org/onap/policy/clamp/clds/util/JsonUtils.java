/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018-2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.Instant;
import org.onap.policy.clamp.authorization.SecureServicePermission;
import org.onap.policy.clamp.authorization.SecureServicePermissionDeserializer;
import org.onap.policy.clamp.dao.model.gson.converter.InstantDeserializer;
import org.onap.policy.clamp.dao.model.gson.converter.InstantSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to access the GSON with restricted type access.
 */
public class JsonUtils {

    protected static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .registerTypeAdapter(Instant.class, new InstantDeserializer())
            .setPrettyPrinting()
            .registerTypeAdapter(SecureServicePermission.class, new SecureServicePermissionDeserializer()).create();

    public static final Gson GSON_JPA_MODEL = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .registerTypeAdapter(Instant.class, new InstantDeserializer()).setPrettyPrinting()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation().create();

    private JsonUtils() {
    }
}
