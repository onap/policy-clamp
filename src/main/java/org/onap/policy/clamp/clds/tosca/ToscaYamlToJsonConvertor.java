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
 * ===================================================================
 *
 */

package org.onap.policy.clamp.clds.tosca;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONObject;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.tosca.Dictionary;
import org.onap.policy.clamp.tosca.DictionaryElement;
import org.onap.policy.clamp.tosca.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * Tosca Model Yaml parser and convertor to JSON Schema consumable for JSON
 * Editor.
 *
 */
@Component
@Deprecated
public class ToscaYamlToJsonConvertor {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private ClampProperties refProp;

    private int simpleTypeOrder = 1000;
    private int complexTypeOrder = 10000;
    private int complexSimpleTypeOrder = 1;

    private int incrementSimpleTypeOrder() {
        return simpleTypeOrder++;
    }

    private int incrementComplexTypeOrder() {
        return complexTypeOrder = complexTypeOrder + 10000;
    }

    private int incrementComplexSimpleTypeOrder() {
        complexSimpleTypeOrder++;
        return complexTypeOrder + complexSimpleTypeOrder;
    }

    /**
     * Parses Tosca YAML string and Converts to JsonObject.
     *
     * @param yamlString YAML string
     * @return JsonObject
     */
    public JsonObject validateAndConvertToJson(String yamlString) {

        Yaml yaml = new Yaml();
        LinkedHashMap<String, Object> loadedYaml = yaml.load(yamlString);
        if (loadedYaml == null) {
            return null;
        }

        JSONObject jsonObject = new JSONObject(loadedYaml);
        return new Gson().fromJson(jsonObject.toString(), JsonObject.class);
    }

    /**
     * return the values by looking up the key in the Toscsa JSON object.
     *
     * @param obj Tosca Json Object
     * @param key the parameter key to look up
     * @return the value for the provided key
     */
    public String getValueFromMetadata(JsonObject obj, String key) {
        JsonElement jsonElement = obj.get(ToscaSchemaConstants.NODE_TYPES);
        if (jsonElement.isJsonObject()) {
            Iterator<Entry<String, JsonElement>> itr =
                jsonElement.getAsJsonObject().entrySet().iterator();
            while (itr.hasNext()) {
                Entry<String, JsonElement> entry = itr.next();
                if (entry.getValue() != null && entry.getValue().isJsonObject()
                    && entry.getValue().getAsJsonObject().has(ToscaSchemaConstants.METADATA)) {
                    JsonObject metadatas = entry.getValue().getAsJsonObject()
                        .get(ToscaSchemaConstants.METADATA).getAsJsonObject();
                    if (metadatas.has(key)) {
                        return metadatas.get(key).getAsString();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Parses Tosca YAML string.
     *
     * @param yamlString YAML string
     * @param modelTypeToUse The model type that must be used to obtain the Json
     *        Schema
     * @return JSON string
     */
    public String parseToscaYaml(String yamlString, String modelTypeToUse) {

        Yaml yaml = new Yaml();
        LinkedHashMap<String, Object> loadedYaml = yaml.load(yamlString);
        if (loadedYaml == null) {
            return "";
        }
        LinkedHashMap<String, Object> nodeTypes = new LinkedHashMap<>();
        LinkedHashMap<String, Object> dataNodes = new LinkedHashMap<>();
        JSONObject jsonParentObject = new JSONObject();
        JSONObject jsonTempObject = new JSONObject();
        parseNodeAndDataType(loadedYaml, nodeTypes, dataNodes);
        populateJsonEditorObject(loadedYaml, nodeTypes, dataNodes, jsonParentObject, jsonTempObject,
            modelTypeToUse);

        String headerTemplate = getValueFromMetadata(validateAndConvertToJson(yamlString),
            ToscaSchemaConstants.METADATA_HEADER_TEMPLATE);
        if (headerTemplate != null) {
            jsonParentObject.put(JsonEditorSchemaConstants.HEADER_TEMPLATE,
                JsonEditorSchemaConstants.HEADER_TEMPLATE_VALUE);
        }
        if (jsonTempObject.length() > 0) {
            jsonParentObject = jsonTempObject;
        }
        JSONObject jsonEditorObject = new JSONObject();
        jsonEditorObject.put(JsonEditorSchemaConstants.SCHEMA, jsonParentObject);
        return jsonEditorObject.toString();
    }

    // Parse node_type and data_type
    @SuppressWarnings("unchecked")
    private void parseNodeAndDataType(LinkedHashMap<String, Object> map,
        LinkedHashMap<String, Object> nodeTypes, LinkedHashMap<String, Object> dataNodes) {
        map.entrySet().stream().forEach(n -> {
            if (n.getKey().contains(ToscaSchemaConstants.NODE_TYPES)
                && n.getValue() instanceof Map) {
                parseNodeAndDataType((LinkedHashMap<String, Object>) n.getValue(), nodeTypes,
                    dataNodes);
            } else if (n.getKey().contains(ToscaSchemaConstants.DATA_TYPES)
                && n.getValue() instanceof Map) {
                parseNodeAndDataType((LinkedHashMap<String, Object>) n.getValue(), nodeTypes,
                    dataNodes);
            } else if (n.getKey().contains(ToscaSchemaConstants.POLICY_NODE)) {
                nodeTypes.put(n.getKey(), n.getValue());
            } else if (n.getKey().contains(ToscaSchemaConstants.POLICY_DATA)) {
                dataNodes.put(n.getKey(), n.getValue());
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void populateJsonEditorObject(LinkedHashMap<String, Object> map,
        LinkedHashMap<String, Object> nodeTypes, LinkedHashMap<String, Object> dataNodes,
        JSONObject jsonParentObject, JSONObject jsonTempObject, String modelTypeToUse) {

        Map<String, JSONObject> jsonEntrySchema = new HashMap<>();
        jsonParentObject.put(JsonEditorSchemaConstants.TYPE, JsonEditorSchemaConstants.TYPE_OBJECT);
        if (nodeTypes.get(modelTypeToUse) instanceof Map) {
            ((LinkedHashMap<String, Object>) nodeTypes.get(modelTypeToUse)).entrySet()
                .forEach(ntElement -> {
                    if (ntElement.getKey().equalsIgnoreCase(ToscaSchemaConstants.PROPERTIES)) {
                        JSONArray rootNodeArray = new JSONArray();
                        if (ntElement.getValue() instanceof Map) {
                            ((LinkedHashMap<String, Object>) ntElement.getValue()).entrySet()
                                .forEach((ntPropertiesElement) -> {
                                    boolean isListNode = false;
                                    parseDescription(
                                        (LinkedHashMap<String, Object>) ntPropertiesElement
                                            .getValue(),
                                        jsonParentObject);
                                    LinkedHashMap<String, Object> parentPropertiesMap =
                                        (LinkedHashMap<String, Object>) ntPropertiesElement
                                            .getValue();
                                    if (parentPropertiesMap.containsKey(ToscaSchemaConstants.TYPE)
                                        && ((String) parentPropertiesMap
                                            .get(ToscaSchemaConstants.TYPE))
                                                .contains(ToscaSchemaConstants.TYPE_MAP)
                                        && parentPropertiesMap
                                            .containsKey(ToscaSchemaConstants.ENTRY_SCHEMA)) {
                                        parentPropertiesMap =
                                            (LinkedHashMap<String, Object>) parentPropertiesMap
                                                .get(ToscaSchemaConstants.ENTRY_SCHEMA);
                                        isListNode = true;
                                    }
                                    if (parentPropertiesMap.containsKey(ToscaSchemaConstants.TYPE)
                                        && ((String) parentPropertiesMap
                                            .get(ToscaSchemaConstants.TYPE))
                                                .contains(ToscaSchemaConstants.POLICY_DATA)) {
                                        ((LinkedHashMap<String, Object>) dataNodes.get(
                                            parentPropertiesMap.get(ToscaSchemaConstants.TYPE)))
                                                .entrySet().stream().forEach(pmap -> {
                                                    if (pmap.getKey().equalsIgnoreCase(
                                                        ToscaSchemaConstants.PROPERTIES)) {
                                                        parseToscaProperties(
                                                            ToscaSchemaConstants.POLICY_NODE,
                                                            (LinkedHashMap<String, Object>) pmap
                                                                .getValue(),
                                                            jsonParentObject, rootNodeArray,
                                                            jsonEntrySchema, dataNodes,
                                                            incrementSimpleTypeOrder());
                                                    }
                                                });
                                    }
                                    if (isListNode) {
                                        jsonTempObject.put(JsonEditorSchemaConstants.TYPE,
                                            JsonEditorSchemaConstants.TYPE_ARRAY);
                                        parseDescription(
                                            (LinkedHashMap<String, Object>) ntPropertiesElement
                                                .getValue(),
                                            jsonTempObject);
                                        jsonTempObject.put(JsonEditorSchemaConstants.ITEMS,
                                            jsonParentObject);
                                        jsonTempObject.put(JsonEditorSchemaConstants.FORMAT,
                                            JsonEditorSchemaConstants.CUSTOM_KEY_FORMAT_TABS);
                                        jsonTempObject.put(JsonEditorSchemaConstants.UNIQUE_ITEMS,
                                            JsonEditorSchemaConstants.TRUE);
                                    }
                                });
                        }
                    }
                });
        }
    }

    @SuppressWarnings("unchecked")
    private void parseToscaProperties(String parentKey, LinkedHashMap<String, Object> propertiesMap,
        JSONObject jsonDataNode, JSONArray array, Map<String, JSONObject> jsonEntrySchema,
        LinkedHashMap<String, Object> dataNodes, final int order) {
        JSONObject jsonPropertyNode = new JSONObject();
        propertiesMap.entrySet().stream().forEach(p -> {
            // Populate JSON Array for "required" key

            if (p.getValue() instanceof Map) {
                LinkedHashMap<String, Object> nodeMap =
                    (LinkedHashMap<String, Object>) p.getValue();
                if (nodeMap.containsKey(ToscaSchemaConstants.REQUIRED)
                    && ((boolean) nodeMap.get(ToscaSchemaConstants.REQUIRED))) {
                    array.put(p.getKey());
                }
                // if(nodeMap.containsKey(ToscaSchemaConstants.CONSTRAINTS))
                parseToscaChildNodeMap(p.getKey(), nodeMap, jsonPropertyNode, jsonEntrySchema,
                    dataNodes, array, incrementSimpleTypeOrder());
            }
        });
        jsonDataNode.put(JsonEditorSchemaConstants.REQUIRED, array);
        jsonDataNode.put(JsonEditorSchemaConstants.PROPERTIES, jsonPropertyNode);
    }

    @SuppressWarnings("unchecked")
    private void parseToscaPropertiesForType(String parentKey,
        LinkedHashMap<String, Object> propertiesMap, JSONObject jsonDataNode, JSONArray array,
        Map<String, JSONObject> jsonEntrySchema, LinkedHashMap<String, Object> dataNodes,
        boolean isType, int order) {
        JSONObject jsonPropertyNode = new JSONObject();

        propertiesMap.entrySet().stream().forEach(p -> {
            // array.put(p.getKey());
            boolean overWriteArray = false;
            if (p.getValue() instanceof Map) {
                LinkedHashMap<String, Object> nodeMap =
                    (LinkedHashMap<String, Object>) p.getValue();
                if (!(parentKey.contains(ToscaSchemaConstants.ENTRY_SCHEMA)
                    || parentKey.contains(ToscaSchemaConstants.POLICY_NODE))
                    && nodeMap.containsKey(ToscaSchemaConstants.TYPE)
                    && (((String) nodeMap.get(ToscaSchemaConstants.TYPE))
                        .contains(ToscaSchemaConstants.POLICY_DATA))) {
                    overWriteArray = true;
                }
                if (nodeMap.containsKey(ToscaSchemaConstants.REQUIRED)
                    && ((boolean) nodeMap.get(ToscaSchemaConstants.REQUIRED))) {
                    array.put(p.getKey());
                }
                parseToscaChildNodeMap(p.getKey(), nodeMap, jsonPropertyNode, jsonEntrySchema,
                    dataNodes, array, order);
            }
        });
        jsonDataNode.put(JsonEditorSchemaConstants.REQUIRED, array);
        jsonDataNode.put(JsonEditorSchemaConstants.PROPERTIES, jsonPropertyNode);
    }

    private void parseToscaChildNodeMap(String childObjectKey,
        LinkedHashMap<String, Object> childNodeMap, JSONObject jsonPropertyNode,
        Map<String, JSONObject> jsonEntrySchema, LinkedHashMap<String, Object> dataNodes,
        JSONArray array, int order) {
        JSONObject childObject = new JSONObject();
        // JSONArray childArray = new JSONArray();
        parseDescription(childNodeMap, childObject);
        parseTypes(childObjectKey, childNodeMap, childObject, jsonEntrySchema, dataNodes, array,
            order);
        parseConstraints(childNodeMap, childObject);
        parseMetadataPossibleValues(childNodeMap, childObject);
        parseEntrySchema(childNodeMap, childObject, jsonPropertyNode, jsonEntrySchema, dataNodes);

        jsonPropertyNode.put(childObjectKey, childObject);
        order++;

    }

    private void parseEntrySchema(LinkedHashMap<String, Object> childNodeMap,
        JSONObject childObject, JSONObject jsonPropertyNode,
        Map<String, JSONObject> jsonEntrySchema, LinkedHashMap<String, Object> dataNodes) {
        if (childNodeMap.get(ToscaSchemaConstants.ENTRY_SCHEMA) != null) {
            if (childNodeMap.get(ToscaSchemaConstants.ENTRY_SCHEMA) instanceof Map) {
                LinkedHashMap<String, Object> entrySchemaMap =
                    (LinkedHashMap<String, Object>) childNodeMap
                        .get(ToscaSchemaConstants.ENTRY_SCHEMA);
                entrySchemaMap.entrySet().stream().forEach(entry -> {
                    if (entry.getKey().equalsIgnoreCase(ToscaSchemaConstants.TYPE)
                        && entry.getValue() != null) {
                        String entrySchemaType = (String) entry.getValue();
                        if (entrySchemaType.contains(ToscaSchemaConstants.POLICY_DATA)) {
                            JSONArray array = new JSONArray();
                            if (jsonEntrySchema.get(entrySchemaType) != null) {
                                // Already traversed
                                JSONObject entrySchemaObject = jsonEntrySchema.get(entrySchemaType);
                                attachEntrySchemaJsonObject(childObject, entrySchemaObject,
                                    JsonEditorSchemaConstants.TYPE_OBJECT);
                            } else if (dataNodes.containsKey(entrySchemaType)) {

                                JSONObject entrySchemaObject = new JSONObject();
                                // Need to traverse
                                ((LinkedHashMap<String, Object>) dataNodes.get(entrySchemaType))
                                    .entrySet().stream().forEach(pmap -> {
                                        if (pmap.getKey()
                                            .equalsIgnoreCase(ToscaSchemaConstants.PROPERTIES)) {
                                            parseToscaProperties(ToscaSchemaConstants.ENTRY_SCHEMA,
                                                (LinkedHashMap<String, Object>) pmap.getValue(),
                                                entrySchemaObject, array, jsonEntrySchema,
                                                dataNodes, incrementComplexTypeOrder());
                                            jsonEntrySchema.put(entrySchemaType, entrySchemaObject);
                                            dataNodes.remove(entrySchemaType);
                                            attachEntrySchemaJsonObject(childObject,
                                                entrySchemaObject,
                                                JsonEditorSchemaConstants.TYPE_OBJECT);
                                        }

                                    });
                            }
                        } else if (entrySchemaType
                            .equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)
                            || entrySchemaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_INTEGER)
                            || entrySchemaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_FLOAT)) {
                            JSONObject entrySchemaObject = new JSONObject();
                            parseConstraints(entrySchemaMap, entrySchemaObject);
                            parseMetadataPossibleValues(entrySchemaMap, entrySchemaObject);
                            String jsontype = JsonEditorSchemaConstants.TYPE_STRING;
                            if (entrySchemaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_INTEGER)
                                || entrySchemaType
                                    .equalsIgnoreCase(ToscaSchemaConstants.TYPE_FLOAT)) {
                                jsontype = JsonEditorSchemaConstants.TYPE_INTEGER;
                            }
                            if (childNodeMap.get(ToscaSchemaConstants.TYPE) != null) {
                                // Only known value of type is String for now
                                if (childNodeMap.get(ToscaSchemaConstants.TYPE) instanceof String) {
                                    String typeValue =
                                        (String) childNodeMap.get(ToscaSchemaConstants.TYPE);
                                    if (typeValue
                                        .equalsIgnoreCase(ToscaSchemaConstants.TYPE_LIST)) {
                                        // Custom key for JSON Editor and UI rendering
                                        childObject.put(JsonEditorSchemaConstants.CUSTOM_KEY_FORMAT,
                                            JsonEditorSchemaConstants.FORMAT_SELECT);
                                        // childObject.put(JsonEditorSchemaConstants.UNIQUE_ITEMS,
                                        // JsonEditorSchemaConstants.TRUE);
                                    }
                                }
                            }
                            attachEntrySchemaJsonObject(childObject, entrySchemaObject, jsontype);
                        }
                    }
                });
            }
        }
    }

    private void attachEntrySchemaJsonObject(JSONObject childObject, JSONObject entrySchemaObject,
        String dataType) {

        entrySchemaObject.put(JsonEditorSchemaConstants.TYPE, dataType);
        childObject.put(JsonEditorSchemaConstants.ITEMS, entrySchemaObject);
    }

    @SuppressWarnings("unchecked")
    private void attachTypeJsonObject(JSONObject childObject, JSONObject typeObject) {
        Iterator<String> keys = typeObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            childObject.put(key, typeObject.get(key));
        }
    }

    /*
     * private String parseKey(String toscaKey, String lookupString) { return
     * toscaKey.substring(toscaKey.indexOf(lookupString) + lookupString.length(),
     * toscaKey.length()); }
     */

    private void parseDescription(LinkedHashMap<String, Object> childNodeMap,
        JSONObject childObject) {
        if (childNodeMap.get(ToscaSchemaConstants.DESCRIPTION) != null) {
            childObject.put(JsonEditorSchemaConstants.TITLE,
                childNodeMap.get(ToscaSchemaConstants.DESCRIPTION));
        }
    }

    private void parseTypes(String childObjectKey, LinkedHashMap<String, Object> childNodeMap,
        JSONObject childObject, Map<String, JSONObject> jsonEntrySchema,
        LinkedHashMap<String, Object> dataNodes, JSONArray array, int order) {
        if (childNodeMap.get(ToscaSchemaConstants.TYPE) != null) {
            // Only known value of type is String for now
            if (childNodeMap.get(ToscaSchemaConstants.TYPE) instanceof String) {
                childObject.put(JsonEditorSchemaConstants.PROPERTY_ORDER, order);
                String typeValue = (String) childNodeMap.get(ToscaSchemaConstants.TYPE);
                if (typeValue.equalsIgnoreCase(ToscaSchemaConstants.TYPE_INTEGER)) {
                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_INTEGER);

                } else if (typeValue.equalsIgnoreCase(ToscaSchemaConstants.TYPE_FLOAT)) {
                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_INTEGER);
                } else if (typeValue.equalsIgnoreCase(ToscaSchemaConstants.TYPE_LIST)) {
                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_ARRAY);
                    // Custom key for JSON Editor and UI rendering
                    childObject.put(JsonEditorSchemaConstants.CUSTOM_KEY_FORMAT,
                        JsonEditorSchemaConstants.CUSTOM_KEY_FORMAT_TABS_TOP);
                    childObject.put(JsonEditorSchemaConstants.UNIQUE_ITEMS,
                        JsonEditorSchemaConstants.TRUE);
                } else if (typeValue.equalsIgnoreCase(ToscaSchemaConstants.TYPE_MAP)) {
                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_OBJECT);
                } else if (typeValue.contains(ToscaSchemaConstants.POLICY_DATA)) {
                    JSONArray childArray = new JSONArray();

                    if (jsonEntrySchema.get(typeValue) != null) {
                        // Already traversed
                        JSONObject entrySchemaObject = jsonEntrySchema.get(typeValue);
                        attachTypeJsonObject(childObject, entrySchemaObject);
                    } else if (dataNodes.containsKey(typeValue)) {
                        JSONObject entrySchemaObject = new JSONObject();
                        // Need to traverse
                        JSONArray jsonArray = new JSONArray();
                        ((LinkedHashMap<String, Object>) dataNodes.get(typeValue)).entrySet()
                            .stream().forEach(pmap -> {
                                if (pmap.getKey()
                                    .equalsIgnoreCase(ToscaSchemaConstants.PROPERTIES)) {
                                    parseToscaPropertiesForType(childObjectKey,
                                        (LinkedHashMap<String, Object>) pmap.getValue(),
                                        entrySchemaObject, childArray, jsonEntrySchema, dataNodes,
                                        true, incrementComplexSimpleTypeOrder());
                                    jsonEntrySchema.put(typeValue, entrySchemaObject);
                                    dataNodes.remove(typeValue);
                                    attachTypeJsonObject(childObject, entrySchemaObject);
                                }
                            });
                    }
                } else {
                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_STRING);
                }
            }
            if (childNodeMap.get(ToscaSchemaConstants.DEFAULT) != null) {
                childObject.put(JsonEditorSchemaConstants.DEFAULT,
                    childNodeMap.get(ToscaSchemaConstants.DEFAULT));
            }
        }
    }

    private void parseConstraints(LinkedHashMap<String, Object> childNodeMap,
        JSONObject childObject) {
        if (childNodeMap.containsKey(ToscaSchemaConstants.CONSTRAINTS)
            && childNodeMap.get(ToscaSchemaConstants.CONSTRAINTS) != null) {
            List<LinkedHashMap<String, Object>> constraintsList =
                (List<LinkedHashMap<String, Object>>) childNodeMap
                    .get(ToscaSchemaConstants.CONSTRAINTS);
            constraintsList.stream().forEach(c -> {
                if (c instanceof Map) {
                    c.entrySet().stream().forEach(constraint -> {
                        if (constraint.getKey().equalsIgnoreCase(ToscaSchemaConstants.MIN_LENGTH)
                            || constraint.getKey()
                                .equalsIgnoreCase(ToscaSchemaConstants.GREATER_OR_EQUAL)) {
                            // For String min_lenghth is minimum length whereas for number, it will
                            // be
                            // minimum or greater than to the defined value
                            if (childNodeMap.containsKey(ToscaSchemaConstants.TYPE)
                                && (childNodeMap.get(ToscaSchemaConstants.TYPE) instanceof String)
                                && ((String) childNodeMap.get(ToscaSchemaConstants.TYPE))
                                    .equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)) {
                                childObject.put(JsonEditorSchemaConstants.MIN_LENGTH,
                                    constraint.getValue());
                            } else {
                                childObject.put(JsonEditorSchemaConstants.MINIMUM,
                                    constraint.getValue());
                            }
                        } else if (constraint.getKey()
                            .equalsIgnoreCase(ToscaSchemaConstants.MAX_LENGTH)
                            || constraint.getKey()
                                .equalsIgnoreCase(ToscaSchemaConstants.LESS_OR_EQUAL)) {
                            // For String max_lenghth is maximum length whereas for number, it will
                            // be
                            // maximum or less than the defined value
                            if (childNodeMap.containsKey(ToscaSchemaConstants.TYPE)
                                && (childNodeMap.get(ToscaSchemaConstants.TYPE) instanceof String)
                                && ((String) childNodeMap.get(ToscaSchemaConstants.TYPE))
                                    .equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)) {
                                childObject.put(JsonEditorSchemaConstants.MAX_LENGTH,
                                    constraint.getValue());
                            } else {
                                childObject.put(JsonEditorSchemaConstants.MAXIMUM,
                                    constraint.getValue());
                            }
                        } else if (constraint.getKey()
                            .equalsIgnoreCase(ToscaSchemaConstants.LESS_THAN)) {
                            childObject.put(JsonEditorSchemaConstants.EXCLUSIVE_MAXIMUM,
                                constraint.getValue());
                        } else if (constraint.getKey()
                            .equalsIgnoreCase(ToscaSchemaConstants.GREATER_THAN)) {
                            childObject.put(JsonEditorSchemaConstants.EXCLUSIVE_MINIMUM,
                                constraint.getValue());
                        } else if (constraint.getKey()
                            .equalsIgnoreCase(ToscaSchemaConstants.IN_RANGE)) {
                            if (constraint.getValue() instanceof ArrayList<?>) {
                                if (childNodeMap.containsKey(ToscaSchemaConstants.TYPE)
                                    && (childNodeMap
                                        .get(ToscaSchemaConstants.TYPE) instanceof String)
                                    && ((String) childNodeMap.get(ToscaSchemaConstants.TYPE))
                                        .equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)) {
                                    childObject.put(JsonEditorSchemaConstants.MIN_LENGTH,
                                        ((ArrayList) constraint.getValue()).get(0));
                                    childObject.put(JsonEditorSchemaConstants.MAX_LENGTH,
                                        ((ArrayList) constraint.getValue()).get(1));
                                } else {
                                    childObject.put(JsonEditorSchemaConstants.MINIMUM,
                                        ((ArrayList) constraint.getValue()).get(0));
                                    childObject.put(JsonEditorSchemaConstants.MAXIMUM,
                                        ((ArrayList) constraint.getValue()).get(1));
                                }

                            }
                        } else if (constraint.getKey()
                            .equalsIgnoreCase(ToscaSchemaConstants.VALID_VALUES)) {
                            JSONArray validValuesArray = new JSONArray();

                            if (constraint.getValue() instanceof ArrayList<?>) {
                                boolean processDictionary =
                                    ((ArrayList<?>) constraint.getValue()).stream().anyMatch(
                                        value -> (value instanceof String && ((String) value)
                                            .contains(ToscaSchemaConstants.DICTIONARY)));
                                if (!processDictionary) {
                                    ((ArrayList<?>) constraint.getValue()).stream()
                                        .forEach(value -> {
                                            validValuesArray.put(value);
                                        });
                                    childObject.put(JsonEditorSchemaConstants.ENUM,
                                        validValuesArray);
                                } else {
                                    ((ArrayList<?>) constraint.getValue()).stream()
                                        .forEach(value -> {
                                            if ((value instanceof String && ((String) value)
                                                .contains(ToscaSchemaConstants.DICTIONARY))) {
                                                processDictionaryElements(childObject,
                                                    (String) value);
                                            }

                                        });

                                }
                            }

                        }
                    });
                }
            });
        }
    }

    private void parseMetadataPossibleValues(LinkedHashMap<String, Object> childNodeMap,
        JSONObject childObject) {
        if (childNodeMap.containsKey(ToscaSchemaConstants.METADATA)
            && childNodeMap.get(ToscaSchemaConstants.METADATA) != null) {
            LinkedHashMap<String, Object> metadataMap =
                (LinkedHashMap<String, Object>) childNodeMap.get(ToscaSchemaConstants.METADATA);
            if (metadataMap instanceof Map) {
                metadataMap.entrySet().stream().forEach(constraint -> {
                    if (constraint.getKey()
                        .equalsIgnoreCase(ToscaSchemaConstants.METADATA_CLAMP_POSSIBLE_VALUES)) {
                        JSONArray validValuesArray = new JSONArray();

                        if (constraint.getValue() instanceof ArrayList<?>) {
                            boolean processDictionary = ((ArrayList<?>) constraint.getValue())
                                .stream().anyMatch(value -> (value instanceof String
                                    && ((String) value).contains(ToscaSchemaConstants.DICTIONARY)));
                            if (processDictionary) {
                                ((ArrayList<?>) constraint.getValue()).stream().forEach(value -> {
                                    if ((value instanceof String && ((String) value)
                                        .contains(ToscaSchemaConstants.DICTIONARY))) {
                                        processDictionaryElements(childObject, (String) value);
                                    }

                                });

                            }
                        }

                    }
                });
            }
        }
    }

    private void processDictionaryElements(JSONObject childObject, String dictionaryReference) {
        if (dictionaryReference.contains("#")) {
            String[] dictionaryKeyArray = dictionaryReference
                .substring(dictionaryReference.indexOf(ToscaSchemaConstants.DICTIONARY) + 11,
                    dictionaryReference.length())
                .split("#");
            // We support only one # as of now.
            List<DictionaryElement> cldsDictionaryElements = null;
            List<DictionaryElement> subDictionaryElements = null;
            if (dictionaryKeyArray != null && dictionaryKeyArray.length == 2) {
                cldsDictionaryElements = dictionaryService.getDictionary(dictionaryKeyArray[0])
                    .getDictionaryElements().stream().collect(Collectors.toList());
                subDictionaryElements = dictionaryService.getDictionary(dictionaryKeyArray[1])
                    .getDictionaryElements().stream().collect(Collectors.toList());

                if (cldsDictionaryElements != null) {
                    List<String> subCldsDictionaryNames = subDictionaryElements.stream()
                        .map(DictionaryElement::getShortName).collect(Collectors.toList());
                    JSONArray jsonArray = new JSONArray();

                    Optional.ofNullable(cldsDictionaryElements).get().stream().forEach(c -> {
                        JSONObject jsonObject = new JSONObject();
                        if (c.getSubDictionary() != null) {
                            Dictionary subDictionary =
                                dictionaryService.getDictionary(c.getSubDictionary());
                            if (subDictionary != null
                                && !subDictionary.getDictionaryElements().isEmpty()) {

                                jsonObject.put(JsonEditorSchemaConstants.CUSTOM_KEY_FORMAT_INPUT,
                                    JsonEditorSchemaConstants.FORMAT_SELECT);

                                List<String> shortNames = new ArrayList<>();
                                subDictionary.getDictionaryElements().stream().forEach(c1 -> {
                                    shortNames.add(c1.getShortName());
                                });
                                jsonObject.put(JsonEditorSchemaConstants.VALUES, shortNames);
                            }
                        }
                        jsonObject.put(JsonEditorSchemaConstants.TYPE, getJsonType(c.getType()));

                        if (c.getType() != null
                            && (c.getType().equalsIgnoreCase(ToscaSchemaConstants.TYPE_STRING)
                                || c.getType().equalsIgnoreCase(ToscaSchemaConstants.TYPE_DATE_TIME)
                                || c.getType().equalsIgnoreCase(ToscaSchemaConstants.TYPE_MAP))) {
                            jsonObject.put(JsonEditorSchemaConstants.MIN_LENGTH, 1);

                            if (c.getType().equalsIgnoreCase(ToscaSchemaConstants.TYPE_DATE_TIME)) {
                                jsonObject.put(JsonEditorSchemaConstants.PLUGIN,
                                    JsonEditorSchemaConstants.DATE_TIME_PICKER);
                                jsonObject.put(JsonEditorSchemaConstants.INPUT_EVENT,
                                    JsonEditorSchemaConstants.DP_CHANGE);
                                JSONObject formatJsonObject = new JSONObject();
                                formatJsonObject.put(JsonEditorSchemaConstants.FORMAT,
                                    JsonEditorSchemaConstants.DATE_TIME_FORMAT);
                                jsonObject.put(JsonEditorSchemaConstants.VALIDATION,
                                    formatJsonObject);
                            }
                        }

                        jsonObject.put(JsonEditorSchemaConstants.ID, c.getName());
                        jsonObject.put(JsonEditorSchemaConstants.LABEL, c.getShortName());
                        jsonObject.put(JsonEditorSchemaConstants.OPERATORS, subCldsDictionaryNames);
                        jsonArray.put(jsonObject);
                    });;
                    JSONObject filterObject = new JSONObject();
                    filterObject.put(JsonEditorSchemaConstants.FILTERS, jsonArray);

                    childObject.put(JsonEditorSchemaConstants.TYPE,
                        JsonEditorSchemaConstants.TYPE_QBLDR);
                    // TO invoke validation on such parameters
                    childObject.put(JsonEditorSchemaConstants.MIN_LENGTH, 1);
                    childObject.put(JsonEditorSchemaConstants.QSSCHEMA, filterObject);

                }
            }
        } else {
            String dictionaryKey = dictionaryReference.substring(
                dictionaryReference.indexOf(ToscaSchemaConstants.DICTIONARY) + 11,
                dictionaryReference.length());

            if (dictionaryKey != null) {
                if (dictionaryKey.contains(ToscaSchemaConstants.TYPE_USER_DEFINED)) {
                    childObject.put(JsonEditorSchemaConstants.ENUM, new ArrayList<>());
                    // Add Enum titles for generated translated values during
                    // JSON instance generation
                    JSONObject enumTitles = new JSONObject();
                    enumTitles.put(JsonEditorSchemaConstants.ENUM_TITLES, new ArrayList<>());
                    childObject.put(JsonEditorSchemaConstants.OPTIONS, enumTitles);
                } else {
                    List<DictionaryElement> cldsDictionaryElements =
                        dictionaryService.getDictionary(dictionaryKey).getDictionaryElements()
                            .stream().collect(Collectors.toList());
                    if (cldsDictionaryElements != null) {
                        List<String> cldsDictionaryNames = new ArrayList<>();
                        List<String> cldsDictionaryFullNames = new ArrayList<>();
                        cldsDictionaryElements.stream().forEach(c -> {
                            // Json type will be translated before Policy creation
                            if (c.getType() != null && !c.getType().equalsIgnoreCase("json")) {
                                cldsDictionaryFullNames.add(c.getName());
                            }
                            cldsDictionaryNames.add(c.getShortName());
                        });

                        if (!cldsDictionaryFullNames.isEmpty()) {
                            childObject.put(JsonEditorSchemaConstants.ENUM,
                                cldsDictionaryFullNames);
                            // Add Enum titles for generated translated values during JSON instance
                            // generation
                            JSONObject enumTitles = new JSONObject();
                            enumTitles.put(JsonEditorSchemaConstants.ENUM_TITLES,
                                cldsDictionaryNames);
                            childObject.put(JsonEditorSchemaConstants.OPTIONS, enumTitles);
                        } else {
                            childObject.put(JsonEditorSchemaConstants.ENUM, cldsDictionaryNames);
                        }

                    }
                }
            }
        }
    }

    private String getJsonType(String toscaType) {
        String jsonType = null;
        if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_INTEGER)
            || toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_NUMBER)) {
            jsonType = JsonEditorSchemaConstants.TYPE_INTEGER;
        } else if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_DATE_TIME)) {
            jsonType = JsonEditorSchemaConstants.TYPE_DATE_TIME;
        } else if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_LIST)) {
            jsonType = JsonEditorSchemaConstants.TYPE_ARRAY;
        } else if (toscaType.equalsIgnoreCase(ToscaSchemaConstants.TYPE_MAP)) {
            jsonType = JsonEditorSchemaConstants.TYPE_MAP;
        } else {
            jsonType = JsonEditorSchemaConstants.TYPE_STRING;
        }
        return jsonType;
    }

}
