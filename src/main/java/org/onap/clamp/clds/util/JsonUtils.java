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
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.onap.clamp.clds.model.properties.AbstractModelElement;
import org.onap.clamp.clds.service.SecureServicePermission;
import org.onap.clamp.clds.service.SecureServicePermissionDeserializer;
import org.onap.clamp.dao.model.gson.converter.InstantDeserializer;
import org.onap.clamp.dao.model.gson.converter.InstantSerializer;

/**
 * This class is used to access the GSON with restricted type access.
 */
public class JsonUtils {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(AbstractModelElement.class);
    private static final String LOG_ELEMENT_NOT_FOUND = "Value '{}' for key 'name' not found in JSON";
    private static final String LOG_ELEMENT_NOT_FOUND_IN_JSON = "Value '{}' for key 'name' not found in JSON {}";

    public static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(SecureServicePermission.class, new SecureServicePermissionDeserializer()).create();

    public static final Gson GSON_JPA_MODEL = new GsonBuilder()
        .registerTypeAdapter(Instant.class, new InstantSerializer())
        .registerTypeAdapter(Instant.class, new InstantDeserializer()).setPrettyPrinting()
        .excludeFieldsWithoutExposeAnnotation().create();

    private JsonUtils() {
    }

    /**
     * typeAdapter Return the value field of the json node element that has a name
     * field equals to the given name.
     */
    public static String getStringValueByName(JsonElement jsonElement, String name) {
        String value = extractJsonValueFromElement(jsonElement, name).map(JsonUtils::extractStringValueFromElement)
            .orElse(null);
        if (value == null) {
            if (logger.isDebugEnabled()) {
                logger.debug(LOG_ELEMENT_NOT_FOUND_IN_JSON, name, jsonElement.toString());
            } else {
                logger.warn(LOG_ELEMENT_NOT_FOUND, name);
            }
        }
        return value;
    }

    /**
     * Return an array of values for the field of the json node element that has a
     * name field equals to the given name.
     */
    public static List<String> getStringValuesByName(JsonElement jsonElement, String name) {
        List<String> values = extractJsonValueFromElement(jsonElement, name)
            .map(JsonUtils::extractStringValuesFromElement).orElse(new ArrayList<>());
        if (values.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(LOG_ELEMENT_NOT_FOUND_IN_JSON, name, jsonElement.toString());
            } else {
                logger.warn(LOG_ELEMENT_NOT_FOUND, name);
            }
        }
        return values;
    }

    /**
     * Return the int value field of the json node element that has a name field
     * equals to the given name.
     */
    public static Integer getIntValueByName(JsonElement element, String name) {
        String value = getStringValueByName(element, name);
        return Integer.valueOf(value);
    }

    /**
     * Return the Json value field of the json node element that has a name field
     * equals to the given name.
     */
    public static JsonObject getJsonObjectByName(JsonElement jsonElement, String name) {
        JsonObject jsonObject = extractJsonValueFromElement(jsonElement, name).map(JsonElement::getAsJsonObject)
            .orElse(null);
        if (jsonObject == null) {
            logger.warn(LOG_ELEMENT_NOT_FOUND, name);
        } else {
            logger.debug(LOG_ELEMENT_NOT_FOUND_IN_JSON, name, jsonElement.toString());
        }
        return jsonObject;
    }

    private static Optional<JsonElement> extractJsonValueFromElement(JsonElement jsonElement, String name) {
        if (jsonElement != null) {
            if (jsonElement.isJsonArray()) {
                return extractValueJsonFromArray(jsonElement, name);
            } else if (hasMatchingParameterName(name, jsonElement)) {
                return Optional.of(jsonElement);
            }
        }
        return Optional.empty();
    }

    private static Optional<JsonElement> extractValueJsonFromArray(JsonElement jsonElement, String name) {
        for (JsonElement element : jsonElement.getAsJsonArray()) {
            if (hasMatchingParameterName(name, element)) {
                return Optional.of(element.getAsJsonObject().get("value"));
            }
        }
        return Optional.empty();
    }

    private static boolean hasMatchingParameterName(String name, JsonElement element) {
        return element.isJsonObject() && element.getAsJsonObject().has("name")
            && name.equals(element.getAsJsonObject().get("name").getAsString());
    }

    private static String extractStringValueFromElement(JsonElement element) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray().get(0).getAsString();
        } else if (element.isJsonPrimitive()) {
            return element.getAsJsonPrimitive().getAsString();
        } else {
            return GSON.toJson(element);
        }
    }

    private static List<String> extractStringValuesFromElement(JsonElement element) {
        if (element.isJsonArray()) {
            return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(element.getAsJsonArray().iterator(), Spliterator.ORDERED),
                    false)
                .filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isString).map(JsonPrimitive::getAsString).collect(Collectors.toList());
        } else {
            String value = extractStringValueFromElement(element);
            return Lists.newArrayList(value);
        }

    }
}
