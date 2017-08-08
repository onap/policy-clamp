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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provide base ModelElement functionality.
 */
public abstract class ModelElement {
    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(ModelElement.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    public static final String      TYPE_POLICY = "policy";
    public static final String      TYPE_TCA    = "tca";

    private final String            type;
    private final ModelBpmn         modelBpmn;
    private final String            id;
    protected String                topicPublishes;
    protected final JsonNode        meNode;
    private boolean                 isFound;

    private final ModelProperties   modelProp;

    /**
     * Perform base parsing of properties for a ModelElement (such as,
     * Collector, StringMatch, Policy and Tca)
     *
     * @param type
     * @param modelProp
     * @param modelBpmn
     * @param modelJson
     */
    protected ModelElement(String type, ModelProperties modelProp, ModelBpmn modelBpmn, JsonNode modelJson) {
        this.type = type;
        this.modelProp = modelProp;
        this.modelBpmn = modelBpmn;
        this.id = modelBpmn.getId(type);
        this.meNode = modelJson.get(id);
        this.isFound = modelBpmn.getModelElementFound(type);
    }

    /**
     * topicSubscribes is the topicPublishes of the from Model Element
     *
     * @return the topicSubscribes
     */
    public String getTopicSubscribes() {
        // get fromId for this type
        String fromId = modelBpmn.getFromId(type);
        // find the type of the from model element
        String fromType = modelBpmn.getType(fromId);
        // get the model element for the type
        ModelElement me = modelProp.getModelElementByType(fromType);
        // get the topic publishes for the model element
        return me.topicPublishes;
    }

    /**
     * @return the topicPublishes
     */
    public String getTopicPublishes() {
        return topicPublishes;
    }

    /**
     * Return the value field of the json node element that has a name field
     * equals to the given name.
     *
     * @param nodeIn
     * @param name
     * @return
     */
    public static String getValueByName(JsonNode nodeIn, String name) {
        String value = null;
        if (nodeIn != null) {
            for (JsonNode node : nodeIn) {
                if (node.path("name").asText().equals(name)) {
                    JsonNode vnode = node.path("value");
                    if (vnode.isArray()) {
                        // if array, assume value is in first element
                        value = vnode.path(0).asText();
                    } else {
                        // otherwise, just return text
                        value = vnode.asText();
                    }
                }
            }
        }
        if (value == null || value.length() == 0) {
            logger.warn(name + "=" + value);
        } else {
            logger.debug(name + "=" + value);
        }
        return value;
    }

    /**
     * Return the int value field of the json node element that has a name field
     * equals to the given name.
     *
     * @param nodeIn
     * @param name
     * @return
     */
    public static Integer getIntValueByName(JsonNode nodeIn, String name) {
        String value = getValueByName(nodeIn, name);
        return Integer.valueOf(value);
    }

    /**
     * Return an array of values for the field of the json node element that has
     * a name field equals to the given name.
     *
     * @param nodeIn
     * @param name
     * @return
     */
    public static List<String> getValuesByName(JsonNode nodeIn, String name) {
        List<String> values = null;
        if (nodeIn != null) {
            Iterator<JsonNode> i = nodeIn.iterator();
            while (i.hasNext()) {
                JsonNode node = i.next();
                if (node.path("name").asText().equals(name)) {
                    values = getValuesList(node);
                }
            }
        }
        if (values == null || values.size() == 0) {
            logger.warn(name + "=" + values);
        } else {
            logger.debug(name + "=" + values);
        }
        return values;
    }

    /**
     * Return an array of String values.
     *
     * @param nodeIn
     * @return
     */
    public static List<String> getValuesList(JsonNode nodeIn) {
        ArrayList<String> al = new ArrayList<>();
        if (nodeIn != null) {
            Iterator<JsonNode> itr = nodeIn.path("value").elements();
            while (itr.hasNext()) {
                JsonNode node = itr.next();
                al.add(node.asText());
            }
        }
        return al;
    }

    /**
     * Return the value field of the json node element that has a name field
     * equals to the given name.
     *
     * @param name
     * @return
     */
    public String getValueByName(String name) {
        return getValueByName(meNode, name);
    }

    /**
     * Return the int value field of the json node element that has a name field
     * equals to the given name.
     *
     * @param name
     * @return
     */
    public Integer getIntValueByName(String name) {
        return getIntValueByName(meNode, name);
    }

    /**
     * Return an array of values for the field of the json node element that has
     * a name field equals to the given name.
     *
     * @param name
     * @return
     */
    public List<String> getValuesByName(String name) {
        return getValuesByName(meNode, name);
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the isFound
     */
    public boolean isFound() {
        return isFound;
    }
}
