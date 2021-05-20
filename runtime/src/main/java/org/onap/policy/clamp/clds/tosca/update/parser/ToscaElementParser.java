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
    /**
     * Constructor.
     */
    private ToscaElementParser() {
    }

    private static LinkedHashMap<String, Object> searchAllDataTypesAndPolicyTypes(String toscaYaml) {
        LinkedHashMap<String, LinkedHashMap<String, Object>> file =
                (LinkedHashMap<String, LinkedHashMap<String, Object>>) new Yaml().load(toscaYaml);
        LinkedHashMap<String, Object> allDataTypesFound = file.get("data_types");
        LinkedHashMap<String, Object> allPolicyTypesFound = file.get("policy_types");
        LinkedHashMap<String, Object> allItemsFound = new LinkedHashMap<>();
        // Put the policies and datatypes in the same collection
        allItemsFound = (allDataTypesFound == null) ? (new LinkedHashMap<>()) : allDataTypesFound;
        allItemsFound.putAll(allPolicyTypesFound == null ? new LinkedHashMap<>() : allPolicyTypesFound);
        return allItemsFound;
    }

    private static LinkedHashMap<String, Object> searchAllNativeToscaDataTypes(String toscaNativeYaml) {
        return ((LinkedHashMap<String, LinkedHashMap<String, Object>>) new Yaml().load(toscaNativeYaml))
                .get("data_types");
    }

    /**
     * Yaml Parse gets raw policies and datatypes, in different sections : necessary to extract
     * all entities and put them at the same level.
     *
     * @param toscaYaml       the tosca model content
     * @param nativeToscaYaml the tosca native datatype content
     * @return a map of Tosca Element containing all tosca elements found (policy types and datatypes)
     */
    public static LinkedHashMap<String, ToscaElement> searchAllToscaElements(String toscaYaml,
                                                                             String nativeToscaYaml) {
        LinkedHashMap<String, Object> allItemsFound = searchAllDataTypesAndPolicyTypes(toscaYaml);
        allItemsFound.putAll(searchAllNativeToscaDataTypes(nativeToscaYaml));
        return parseAllItemsFound(allItemsFound);
    }

    /**
     * With all the component, get as Map, Components and Components properties are created.
     *
     * @param allMaps maps
     */
    private static LinkedHashMap<String, ToscaElement> parseAllItemsFound(LinkedHashMap<String, Object> allMaps) {
        LinkedHashMap<String, ToscaElement> allItemsFound = new LinkedHashMap<String, ToscaElement>();
        //Component creations, from the file maps
        for (Entry<String, Object> itemToParse : allMaps.entrySet()) {
            LinkedHashMap<String, Object> componentBody = (LinkedHashMap<String, Object>) itemToParse.getValue();
            ToscaElement toscaElement =
                    new ToscaElement(itemToParse.getKey(), (String) componentBody.get("derived_from"),
                            (String) componentBody.get("description"));
            //If policy, version and type_version :
            if (componentBody.get("type_version") != null) {
                toscaElement.setVersion((String) componentBody.get("type_version"));
                toscaElement.setTypeVersion((String) componentBody.get("type_version"));
            }
            //Properties creation, from the map
            if (componentBody.get("properties") != null) {
                LinkedHashMap<String, Object> properties =
                        (LinkedHashMap<String, Object>) componentBody.get("properties");
                for (Entry<String, Object> itemToProperty : properties.entrySet()) {
                    ToscaElementProperty toscaElementProperty = new ToscaElementProperty(itemToProperty.getKey(),
                            (LinkedHashMap<String, Object>) itemToProperty.getValue());
                    toscaElement.addProperties(toscaElementProperty);
                }
            }
            allItemsFound.put(toscaElement.getName(), toscaElement);
        }
        return allItemsFound;
    }
}
