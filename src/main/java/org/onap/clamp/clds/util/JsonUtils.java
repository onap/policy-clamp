/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018-2019 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.util;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;

import org.onap.clamp.authorization.SecureServicePermission;
import org.onap.clamp.authorization.SecureServicePermissionDeserializer;
import org.onap.clamp.dao.model.gson.converter.InstantDeserializer;
import org.onap.clamp.dao.model.gson.converter.InstantSerializer;

/**
 * This class is used to access the GSON with restricted type access.
 */
public class JsonUtils {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(JsonUtils.class);

    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(SecureServicePermission.class, new SecureServicePermissionDeserializer()).create();

    public static final Gson GSON_JPA_MODEL = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantSerializer())
            .registerTypeAdapter(Instant.class, new InstantDeserializer()).setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation().create();

    private JsonUtils() {
    }
}
