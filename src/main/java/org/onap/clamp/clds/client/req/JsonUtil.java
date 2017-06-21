/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.client.req;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for formatting json
 */
public class JsonUtil {

    /**
     * Add list of values to json array node
     *
     * @param list
     * @param node
     */
    public static void addListToArrayNode(List<String> list, ArrayNode node) {
        for (String aList : list) {
            node.add(aList);
        }
    }

    /**
     * Add list of values to json array node
     *
     * @param json
     * @param name
     * @param list
     */
    public static void addArrayField(JsonNode json, String name, List<String> list) {
        if (list != null) {
            ArrayNode node = (ArrayNode) json.withArray(name);
            for (String aList : list) {
                node.add(aList);
            }
        }

    }

}
