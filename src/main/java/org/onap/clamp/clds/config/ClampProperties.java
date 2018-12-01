/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Splitter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.onap.clamp.clds.util.JacksonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Holds Clamp properties and add some functionalities.
 */
@Component
public class ClampProperties {

    @Autowired
    private ApplicationContext appContext;
    @Autowired
    private Environment env;
    public static final String CONFIG_PREFIX = "clamp.config.";

    /**
     * get property value.
     *
     * @param key
     *        The first key
     * @return The string with the value
     */
    public String getStringValue(String key) {
        return env.getProperty(CONFIG_PREFIX + key);
    }

    /**
     * get property value for a combo key (key1 + "." + key2). If not found just use
     * key1.
     *
     * @param key1
     *        The first key
     * @param key2
     *        The second key after a dot
     * @return The string with the value
     */
    public String getStringValue(String key1, String key2) {
        String value = getStringValue(key1 + "." + key2);
        if (value == null || value.length() == 0) {
            value = getStringValue(key1);
        }
        return value;
    }

    /**
     * Return json as objects that can be updated. The value obtained from the
     * clds-reference file will be used as a filename.
     *
     * @param key
     *        The key that will be used to access the clds-reference file
     * @return A jsonNode
     * @throws IOException
     *         In case of issues with the JSON parser
     */
    public JsonNode getJsonTemplate(String key) throws IOException {
        String fileReference = getStringValue(key);
        return (fileReference != null)
            ? JacksonUtils.getObjectMapperInstance().readValue(getFileContentFromPath(fileReference), JsonNode.class)
            : null;
    }

    /**
     * Return json as objects that can be updated. First try with combo key (key1 +
     * "." + key2), otherwise default to just key1. The value obtained from the
     * clds-reference file will be used as a filename.
     *
     * @param key1
     *        The first key
     * @param key2
     *        The second key after a dot
     * @return A JsonNode
     * @throws IOException
     *         In case of issues with the JSON parser
     */
    public JsonNode getJsonTemplate(String key1, String key2) throws IOException {
        String fileReference = getStringValue(key1, key2);
        return (fileReference != null)
            ? JacksonUtils.getObjectMapperInstance().readValue(getFileContentFromPath(fileReference), JsonNode.class)
            : null;
    }

    /**
     * Return the file content. The value obtained from the clds-reference file will
     * be used as a filename.
     *
     * @param key
     *        The key that will be used to access the clds-reference file
     * @return File content in String
     * @throws IOException
     *         In case of issues with the JSON parser
     */
    public String getFileContent(String key) throws IOException {
        String fileReference = getStringValue(key);
        return (fileReference != null) ? getFileContentFromPath(fileReference) : null;
    }

    /**
     * Return the file content. First try with combo key (key1 + "." + key2),
     * otherwise default to just key1. The value obtained from the clds-reference
     * file will be used as a filename.
     *
     * @param key1
     *        The first key
     * @param key2
     *        The second key after a dot
     * @return File content in String
     * @throws IOException
     *         In case of issues with the JSON parser
     */
    public String getFileContent(String key1, String key2) throws IOException {
        String fileReference = getStringValue(key1, key2);
        return (fileReference != null) ? getFileContentFromPath(fileReference) : null;
    }

    private String getFileContentFromPath(String filepath) throws IOException {
        URL url = appContext.getResource(filepath).getURL();
        return IOUtils.toString(url, StandardCharsets.UTF_8);
    }

    /**
     * 
     * 
     * @param key
     *        property key
     * @param separator
     *        property value separator
     * @return List of Strings split with a separator
     */
    public List<String> getStringList(String key, String separator) {
        return Splitter.on(separator).trimResults().omitEmptyStrings()
            .splitToList(env.getProperty(CONFIG_PREFIX + key));
    }
}
