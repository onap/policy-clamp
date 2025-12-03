/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider used to serialize and deserialize policy objects via gson using jackson
 * default behaviors and annotations.
 */
public class JacksonHandler extends GsonMessageBodyHandler {

    public static final Logger logger = LoggerFactory.getLogger(JacksonHandler.class);

    /**
     * Constructs the object.
     */
    public JacksonHandler() {
        this(configBuilder(new GsonBuilder()).create());
    }

    /**
     * Constructs the object.
     *
     * @param gson the Gson object to be used to serialize and de-serialize
     */
    public JacksonHandler(Gson gson) {
        super(gson);
        logger.info("Using GSON with Jackson behaviors for REST calls");
    }

    /**
     * Configures a builder with the adapters normally used by this handler (e.g.,
     * adapters for GsonJsonXxx annotations).
     *
     * @param builder builder to be configured
     * @return the configured builder
     */
    public static GsonBuilder configBuilder(GsonBuilder builder) {
        return builder.registerTypeAdapterFactory(new JacksonFieldAdapterFactory())
                        .registerTypeAdapterFactory(new JacksonMethodAdapterFactory())
                        .registerTypeAdapterFactory(new MapDoubleAdapterFactory())
                        .setExclusionStrategies(new JacksonExclusionStrategy())
                        .disableHtmlEscaping();
    }
}
