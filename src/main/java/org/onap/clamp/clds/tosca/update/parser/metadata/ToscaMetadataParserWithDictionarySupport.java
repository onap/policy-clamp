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

package org.onap.clamp.clds.tosca.update.parser.metadata;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.onap.clamp.clds.tosca.JsonEditorSchemaConstants;
import org.onap.clamp.clds.tosca.ToscaSchemaConstants;
import org.onap.clamp.clds.tosca.update.elements.ToscaElementProperty;
import org.onap.clamp.clds.tosca.update.execution.ToscaMetadataExecutor;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.tosca.DictionaryElement;
import org.onap.clamp.tosca.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ToscaMetadataParserWithDictionarySupport implements ToscaMetadataParser {

    @Autowired
    private ToscaMetadataExecutor toscaMetadataExecutor;

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * This method is used to start the processing of the metadata field.
     *
     * @param toscaElementProperty The property metadata as Json Object
     * @return The jsonObject structure that must be added to the json schema
     */
    public JsonObject processAllMetadataElement(ToscaElementProperty toscaElementProperty, Service serviceModel) {
        if (dictionaryService != null) {
            return parseMetadataPossibleValues(toscaElementProperty.getItems(), dictionaryService, serviceModel,
                    toscaMetadataExecutor);
        }
        else {
            return null;
        }
    }

    private static JsonObject parseMetadataPossibleValues(LinkedHashMap<String, Object> childNodeMap,
                                                          DictionaryService dictionaryService, Service serviceModel,
                                                          ToscaMetadataExecutor toscaMetadataExecutor) {
        JsonObject childObject = new JsonObject();
        if (childNodeMap.containsKey(ToscaSchemaConstants.METADATA)
                && childNodeMap.get(ToscaSchemaConstants.METADATA) != null) {
            ((LinkedHashMap<String, Object>) childNodeMap.get(ToscaSchemaConstants.METADATA)).forEach((key,
                                                                                                       value) -> {
                if (key.equalsIgnoreCase(ToscaSchemaConstants.METADATA_CLAMP_POSSIBLE_VALUES)) {
                    if (((String) value).contains(ToscaSchemaConstants.DICTIONARY)) {
                        processDictionaryElements((String) value, childObject, dictionaryService);
                    }
                    if (((String) value).contains("ClampExecution:")) {
                        executeClampProcess(((String) value).replaceAll("ClampExecution:", ""), childObject,
                                serviceModel, toscaMetadataExecutor);
                    }
                }
            });
        }
        return childObject;
    }

    private static void executeClampProcess(String processInfo, JsonObject childObject, Service serviceModel,
                                            ToscaMetadataExecutor toscaMetadataExecutor) {
        toscaMetadataExecutor.executeTheProcess(processInfo, childObject, serviceModel);
    }

    private static void processDictionaryElements(String dictionaryReference, JsonObject childObject,
                                                  DictionaryService dictionaryService) {
        String[] dictionaryKeyArray =
                dictionaryReference.substring(dictionaryReference.indexOf(ToscaSchemaConstants.DICTIONARY) + 11,
                        dictionaryReference.length()).split("#");
        if (dictionaryKeyArray.length > 1) {
            // We support only one # as of now.
            List<DictionaryElement> dictionaryElements = null;
            if (dictionaryKeyArray.length == 2) {
                dictionaryElements = new ArrayList<>(dictionaryService.getDictionary(dictionaryKeyArray[0])
                        .getDictionaryElements());
                JsonArray subDictionaryNames = new JsonArray();
                new ArrayList<DictionaryElement>(dictionaryService.getDictionary(dictionaryKeyArray[1])
                        .getDictionaryElements()).forEach(elem -> subDictionaryNames.add(elem.getShortName()));

                JsonArray jsonArray = new JsonArray();

                Optional.of(dictionaryElements).get().stream().forEach(c -> {
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty(JsonEditorSchemaConstants.TYPE, getJsonType(c.getType()));
                    if (c.getType() != null
                            && c.getType().equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)) {
                        jsonObject.addProperty(JsonEditorSchemaConstants.MIN_LENGTH, 1);

                    }
                    jsonObject.addProperty(JsonEditorSchemaConstants.ID, c.getName());
                    jsonObject.addProperty(JsonEditorSchemaConstants.LABEL, c.getShortName());
                    jsonObject.add(JsonEditorSchemaConstants.OPERATORS, subDictionaryNames);
                    jsonArray.add(jsonObject);
                });

                JsonObject filterObject = new JsonObject();
                filterObject.add(JsonEditorSchemaConstants.FILTERS, jsonArray);

                childObject.addProperty(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_QBLDR);
                // TO invoke validation on such parameters
                childObject.addProperty(JsonEditorSchemaConstants.MIN_LENGTH, 1);
                childObject.add(JsonEditorSchemaConstants.QSSCHEMA, filterObject);

            }
        }
        else {
            List<DictionaryElement> dictionaryElements =
                    new ArrayList<>(dictionaryService.getDictionary(dictionaryKeyArray[0]).getDictionaryElements());
            JsonArray dictionaryNames = new JsonArray();
            JsonArray dictionaryFullNames = new JsonArray();
            dictionaryElements.stream().forEach(c -> {
                // Json type will be translated before Policy creation
                if (c.getType() != null && !c.getType().equalsIgnoreCase("json")) {
                    dictionaryFullNames.add(c.getName());
                }
                dictionaryNames.add(c.getShortName());
            });

            if (dictionaryFullNames.size() > 0) {
                childObject.add(JsonEditorSchemaConstants.ENUM, dictionaryFullNames);
                // Add Enum titles for generated translated values during JSON instance
                // generation
                JsonObject enumTitles = new JsonObject();
                enumTitles.add(JsonEditorSchemaConstants.ENUM_TITLES, dictionaryNames);
                childObject.add(JsonEditorSchemaConstants.OPTIONS, enumTitles);
            }
            else {
                childObject.add(JsonEditorSchemaConstants.ENUM, dictionaryNames);
            }
        }
    }

    private static String getJsonType(String toscaType) {
        String jsonType = null;
        if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_INTEGER)) {
            jsonType = JsonEditorSchemaConstants.TYPE_INTEGER;
        }
        else if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_LIST)) {
            jsonType = JsonEditorSchemaConstants.TYPE_ARRAY;
        }
        else {
            jsonType = JsonEditorSchemaConstants.TYPE_STRING;
        }
        return jsonType;
    }

}
