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

package org.onap.clamp.clds.model.prop;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * Parse Model BPMN properties.
 * <p>
 * Example json: {"collector":[{"id":"Collector_11r50j1", "from":"StartEvent_1"}],"stringMatch":[{"id":"StringMatch_0h6cbdv"}],"policy":[{"id":"Policy_0oxeocn", "from":"StringMatch_0h6cbdv"}]}
 */
public class ModelBpmn {
    private static final Logger logger = Logger.getLogger(ModelBpmn.class.getName());

    // for each type, an array of entries
    private final Map<String, List<ModelBpmnEntry>> entriesByType = new HashMap<>();

    // for each id, an array of entries
    private final Map<String, List<ModelBpmnEntry>> entriesById   = new HashMap<>();

    // List of all elementIds
    private List<String>                            bpmnElementIds;

    /**
     * Create ModelBpmn and populate maps from json
     *
     * @param modelBpmnPropText
     * @return
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public static ModelBpmn create(String modelBpmnPropText) throws IOException {
        ModelBpmn modelBpmn = new ModelBpmn();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.readValue(modelBpmnPropText, ObjectNode.class);
        // iterate over each entry like: "collector":[{"id":"Collector_11r50j1","from":"StartEvent_1"}]
        Iterator<Entry<String, JsonNode>> entryItr = root.fields();
        List<String> bpmnElementIdList = new ArrayList<>();
        while (entryItr.hasNext()) {
            // process the entry
            Entry<String, JsonNode> entry = entryItr.next();
            String type = entry.getKey();
            ArrayNode arrayNode = (ArrayNode) entry.getValue();
            // process each id/from object, like: {"id":"Collector_11r50j1","from":"StartEvent_1"}
            for (JsonNode anArrayNode : arrayNode) {
                ObjectNode node = (ObjectNode) anArrayNode;
                String id = node.get("id").asText();
                String fromId = node.get("from").asText();
                ModelBpmnEntry modelBpmnEntry = new ModelBpmnEntry(type, id, fromId);
                modelBpmn.addEntry(modelBpmnEntry);
                bpmnElementIdList.add(id);
            }
            modelBpmn.setBpmnElementIds(bpmnElementIdList);
        }
        return modelBpmn;
    }

    /**
     * Add entry to both maps.
     *
     * @param entry
     */
    private void addEntry(ModelBpmnEntry entry) {
        addEntry(entriesByType, entry, entry.getType());
        addEntry(entriesById, entry, entry.getId());
    }

    /**
     * Add an entry to provided map with provided key.
     *
     * @param map
     * @param entry
     * @param key
     */
    private static void addEntry(Map<String, List<ModelBpmnEntry>> map, ModelBpmnEntry entry, String key) {
        List<ModelBpmnEntry> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        list.add(entry);
    }

    /**
     * 
     * 
     * @param type
     * @return true if the element is found or false otherwise
     */
    public boolean getModelElementFound(String type) {
        return entriesByType.get(type) != null;
    }

    /**
     * @return the id field given the ModelElement type
     */
    public String getId(String type) {
        return entriesByType.get(type).get(0).getId();
    }

    /**
     * @return the fromId field given the ModelElement type
     */
    public String getFromId(String type) {
        return entriesByType.get(type).get(0).getFromId();
    }

    /**
     * @return the ModelElement type given the ModelElement id
     */
    public String getType(String id) {
        return entriesById.get(id).get(0).getType();
    }

    /**
     * @return list of elementIds from bpmn
     */
    public List<String> getBpmnElementIds() {
        return bpmnElementIds;
    }

    public void setBpmnElementIds(List<String> bpmnElementIds) {
        this.bpmnElementIds = bpmnElementIds;
    }
}
