/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.clds.tosca.update.parser;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.onap.policy.clamp.clds.tosca.update.elements.ToscaElement;
import org.onap.policy.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.yaml.snakeyaml.Yaml;

public class ToscaElementParser {
    private static final String DERIVED_FROM = "derived_from";
    private static final String DESCRIPTION = "description";
    private static final String PROPERTIES = "properties";
    private static final String TYPE_VERSION = "type_version";

    /**
     * Constructor.
     */
    private ToscaElementParser() {
    }

    private static LinkedHashMap<String, Object> searchAllDataTypesAndPolicyTypes(String toscaYaml) {
        @SuppressWarnings("unchecked")
        LinkedHashMap<String, LinkedHashMap<String, Object>> file =
                (LinkedHashMap<String, LinkedHashMap<String, Object>>) new Yaml().load(toscaYaml);
        LinkedHashMap<String, Object> allDataTypesFound = file.get("data_types");
        LinkedHashMap<String, Object> allPolicyTypesFound = file.get("policy_types");
        LinkedHashMap<String, Object> allItemsFound;
        // Put the policies and datatypes in the same collection
        allItemsFound = (allDataTypesFound == null) ? (new LinkedHashMap<>()) : allDataTypesFound;
        allItemsFound.putAll(allPolicyTypesFound == null ? new LinkedHashMap<>() : allPolicyTypesFound);
        return allItemsFound;
    }

    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, Object> searchAllNativeToscaDataTypes(String toscaNativeYaml) {
        return ((LinkedHashMap<String, LinkedHashMap<String, Object>>) new Yaml().load(toscaNativeYaml))
                .get("data_types");
    }

    /**
     * Yaml Parse gets raw policies and datatypes, in different sections : necessary to extract
     * all entities and put them at the same level.
     *
     * @param toscaYaml the tosca model content
     * @param nativeToscaYaml the tosca native datatype content
     * @return a map of Tosca Element containing all tosca elements found (policy types and datatypes)
     */
    public static LinkedHashMap<String, ToscaElement> searchAllToscaElements(String toscaYaml, String nativeToscaYaml) {
        LinkedHashMap<String, Object> allItemsFound = searchAllDataTypesAndPolicyTypes(toscaYaml);
        allItemsFound.putAll(searchAllNativeToscaDataTypes(nativeToscaYaml));
        return parseAllItemsFound(allItemsFound);
    }

    /**
     * With all the component, get as Map, Components and Components properties are created.
     *
     * @param allMaps maps
     */
    @SuppressWarnings("unchecked")
    private static LinkedHashMap<String, ToscaElement> parseAllItemsFound(LinkedHashMap<String, Object> allMaps) {
        LinkedHashMap<String, ToscaElement> allItemsFound = new LinkedHashMap<>();
        // Component creations, from the file maps
        for (Entry<String, Object> itemToParse : allMaps.entrySet()) {
            LinkedHashMap<String, Object> componentBody = (LinkedHashMap<String, Object>) itemToParse.getValue();
            var toscaElement = new ToscaElement(itemToParse.getKey(), (String) componentBody.get(DERIVED_FROM),
                    (String) componentBody.get(DESCRIPTION));
            // If policy, version and type_version :
            if (componentBody.get(TYPE_VERSION) != null) {
                toscaElement.setVersion((String) componentBody.get(TYPE_VERSION));
                toscaElement.setTypeVersion((String) componentBody.get(TYPE_VERSION));
            }
            // Properties creation, from the map
            if (componentBody.get(PROPERTIES) != null) {
                LinkedHashMap<String, Object> foundProperties =
                        (LinkedHashMap<String, Object>) componentBody.get(PROPERTIES);
                for (Entry<String, Object> itemToProperty : foundProperties.entrySet()) {
                    var toscaElementProperty = new ToscaElementProperty(itemToProperty.getKey(),
                            (LinkedHashMap<String, Object>) itemToProperty.getValue());
                    toscaElement.addProperties(toscaElementProperty);
                }
            }
            allItemsFound.put(toscaElement.getName(), toscaElement);
        }
        return allItemsFound;
    }
}
