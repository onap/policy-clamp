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

package org.onap.clamp.clds.tosca.update;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.yaml.snakeyaml.Yaml;

public class ToscaItemsParser {
    private LinkedHashMap<String, ToscaElement> allItemsFound;

    /**
     * Constructor.
     *
     * @param toscaYaml               The tosca to parse
     * @param toscaNativeDataTypeYaml THe name of the policy type to search
     */
    public ToscaItemsParser(String toscaYaml, String toscaNativeDataTypeYaml) {
        this.allItemsFound = searchAllToscaElements(toscaYaml, toscaNativeDataTypeYaml);
    }

    public LinkedHashMap<String, ToscaElement> getAllItemsFound() {
        return allItemsFound;
    }

    private static LinkedHashMap<String, Object> searchAllDataTypesAndPolicyTypes(String toscaYaml) {
        LinkedHashMap<String, LinkedHashMap<String, Object>> file =
                (LinkedHashMap<String, LinkedHashMap<String, Object>>) new Yaml().load(toscaYaml);
        // Get DataTypes
        LinkedHashMap<String, Object> allItemsFound = file.get("data_types");
        allItemsFound = (allItemsFound == null) ? (new LinkedHashMap<>()) : allItemsFound;
        // Put the policies and datatypes in the same collection
        allItemsFound.putAll(file.get("policy_types"));
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
     * @return a map
     */
    private static LinkedHashMap<String, ToscaElement> searchAllToscaElements(String toscaYaml,
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
                    Property property = new Property(itemToProperty.getKey(),
                            (LinkedHashMap<String, Object>) itemToProperty.getValue());
                    toscaElement.addProperties(property);
                }
            }
            allItemsFound.put(toscaElement.getName(), toscaElement);
        }
        return allItemsFound;
    }
}
