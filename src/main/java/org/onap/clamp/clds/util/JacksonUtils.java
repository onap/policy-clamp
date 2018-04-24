/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to access the jackson with restricted type access.
 */
public class JacksonUtils {

    private static ObjectMapper objectMapper;

    private JacksonUtils() {
    }

    /**
     * Call this method to retrieve a secure ObjectMapper.
     * 
     * @return an ObjectMapper instance (same for clamp)
     */
    public static synchronized ObjectMapper getObjectMapperInstance() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            // This is to disable the security hole that could be opened for
            // json deserialization, if needed do this
            // objectMapper.enableDefaultTyping(DefaultTyping.NON_FINAL);
            objectMapper.disableDefaultTyping();
        }
        return objectMapper;
    }
}
