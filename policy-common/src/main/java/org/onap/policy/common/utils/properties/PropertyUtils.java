/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.properties;

import java.util.Properties;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Utilities for extracting property values and converting them to other types.
 */
@AllArgsConstructor
public class PropertyUtils {
    /**
     * Properties on which to work.
     */
    private Properties properties;

    /**
     * Prefix to prepend to property names.
     */
    private String prefix;

    /**
     * Function to invoke if a property value is invalid.
     */
    private TriConsumer<String, String, Exception> invalidHandler;

    /**
     * Gets a string property.
     *
     * @param propName name of the property whose value is to be retrieved
     * @param defaultValue value to use if the property value is empty or does not exist
     * @return the property's value
     */
    public String getString(String propName, String defaultValue) {
        String propValue = getProperty(propName);
        return (StringUtils.isBlank(propValue) ? defaultValue : propValue);
    }

    /**
     * Gets a boolean property.
     *
     * @param propName name of the property whose value is to be retrieved
     * @param defaultValue value to use if the property value is empty or does not exist
     * @return the property's value
     */
    public boolean getBoolean(String propName, boolean defaultValue) {
        String propValue = getProperty(propName);

        if (!StringUtils.isBlank(propValue)) {
            return Boolean.parseBoolean(propValue);
        }

        return defaultValue;
    }

    /**
     * Gets an integer property.
     *
     * @param propName name of the property whose value is to be retrieved
     * @param defaultValue value to use if the property value is empty or does not exist
     * @return the property's value
     */
    public int getInteger(String propName, int defaultValue) {
        String propValue = getProperty(propName);

        if (!StringUtils.isBlank(propValue)) {
            try {
                return Integer.parseInt(propValue);

            } catch (NumberFormatException nfe) {
                invalidHandler.accept(getFullName(propName), propValue, nfe);
            }
        }

        return defaultValue;
    }


    /**
     * Gets a property's value.
     *
     * @param propName name of the property whose value is to be retrieved
     * @return the property's value, or {@code null} if it does not exist
     */
    private String getProperty(String propName) {
        return properties.getProperty(getFullName(propName));
    }

    /**
     * Gets the full property name, with the prefix prepended.
     *
     * @param propName property name, without the prefix
     * @return the full property name
     */
    private String getFullName(String propName) {
        return prefix + propName;
    }

    @FunctionalInterface
    public static interface TriConsumer<A, B, C> {
        public void accept(A propName, B propValue, C exception);
    }
}
