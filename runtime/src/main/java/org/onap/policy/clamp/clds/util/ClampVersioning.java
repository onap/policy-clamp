/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018, 2021 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.policy.clamp.clds.util;

import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class give a way to know the Clamp version easily, the version in that
 * file is set by maven at build time.
 *
 */
public class ClampVersioning {
    private static final String RESOURCE_NAME = "clds-version.properties";
    private static final String CLDS_VERSION_PROPERTY = "clds.version";
    private static final Logger LOGGER = LoggerFactory.getLogger(ClampVersioning.class);

    private ClampVersioning() {
    }

    /**
     * Returns Clds version from properties.
     *
     * @return Clds version from properties
     */
    public static String getCldsVersionFromProps() {
        var cldsVersion = "";
        var props = new Properties();
        try (InputStream resourceStream = ResourceFileUtils.getResourceAsStream(RESOURCE_NAME)) {
            props.load(resourceStream);
            cldsVersion = props.getProperty(CLDS_VERSION_PROPERTY);
        } catch (Exception ex) {
            LOGGER.error("Exception caught during the " + CLDS_VERSION_PROPERTY + " property reading", ex);
        }
        return cldsVersion;
    }
}
