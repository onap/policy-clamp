/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 Huawei Technologies Co., Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 * ================================================================================
 *
 */

package org.onap.clamp.clds.client;

import static java.lang.Boolean.parseBoolean;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Date;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.ExchangeBuilder;
import org.onap.clamp.clds.exception.cds.CdsParametersException;
import org.onap.clamp.clds.model.cds.CdsBpWorkFlowListResponse;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.clds.util.LoggingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class implements the communication with CDS for the service inventory.
 */
@Component
public class CdsServices {

    @Autowired
    CamelContext camelContext;

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CdsServices.class);

    private static final String TYPE = "type";
    private static final String PROPERTIES = "properties";
    private static final String LIST = "list";

    /**
     * Constructor.
     */
    @Autowired
    public CdsServices() {
    }


    /**
     * Query CDS to get blueprint's workflow list.
     *
     * @param blueprintName    CDS blueprint name
     * @param blueprintVersion CDS blueprint version
     * @return CdsBpWorkFlowListResponse CDS blueprint's workflow list
     */
    public CdsBpWorkFlowListResponse getBlueprintWorkflowList(String blueprintName, String blueprintVersion) {
        LoggingUtils.setTargetContext("CDS", "getBlueprintWorkflowList");

        Exchange myCamelExchange = ExchangeBuilder.anExchange(camelContext)
                .withProperty("blueprintName", blueprintName).withProperty("blueprintVersion", blueprintVersion)
                .build();

        Exchange exchangeResponse = camelContext.createProducerTemplate()
                .send("direct:get-blueprint-workflow-list", myCamelExchange);

        if (Integer.valueOf(200).equals(exchangeResponse.getIn().getHeader("CamelHttpResponseCode"))) {
            String cdsResponse = (String) exchangeResponse.getIn().getBody();
            logger.info("getBlueprintWorkflowList, response from CDS:" + cdsResponse);
            LoggingUtils.setResponseContext("0", "Get Blueprint workflow list", this.getClass().getName());
            Date startTime = new Date();
            LoggingUtils.setTimeContext(startTime, new Date());
            return JsonUtils.GSON_JPA_MODEL.fromJson(cdsResponse, CdsBpWorkFlowListResponse.class);
        } else {
            logger.error("CDS getBlueprintWorkflowList FAILED");
            return null;
        }

    }

    /**
     * Query CDS to get input properties of workflow.
     *
     * @param blueprintName    CDS blueprint name
     * @param blueprintVersion CDS blueprint name
     * @param workflow         CDS blueprint's workflow
     * @return input properties in json format
     */
    public JsonObject getWorkflowInputProperties(String blueprintName, String blueprintVersion,
                                                 String workflow) {
        LoggingUtils.setTargetContext("CDS", "getWorkflowInputProperties");

        Exchange myCamelExchange = ExchangeBuilder.anExchange(camelContext)
                .withBody(getCdsPayloadForWorkFlow(blueprintName, blueprintVersion, workflow))
                .build();

        Exchange exchangeResponse = camelContext.createProducerTemplate()
                .send("direct:get-blueprint-workflow-input-properties", myCamelExchange);

        if (Integer.valueOf(200).equals(exchangeResponse.getIn().getHeader("CamelHttpResponseCode"))) {
            String cdsResponse = (String) exchangeResponse.getIn().getBody();
            logger.info("getWorkflowInputProperties, response from CDS:" + cdsResponse);
            LoggingUtils.setResponseContext("0", "Get Blueprint workflow input properties", this.getClass().getName());
            Date startTime = new Date();
            LoggingUtils.setTimeContext(startTime, new Date());
            return parseCdsResponse(cdsResponse);
        } else {
            logger.error("CDS getWorkflowInputProperties FAILED");
            return null;
        }
    }

    protected JsonObject parseCdsResponse(String response) {
        JsonObject root = JsonParser.parseString(response).getAsJsonObject();
        JsonObject inputs = root.getAsJsonObject("workFlowData").getAsJsonObject("inputs");
        JsonObject dataTypes = root.getAsJsonObject("dataTypes");

        JsonObject workFlowProperties = new JsonObject();
        workFlowProperties.add("inputs", getInputProperties(inputs, dataTypes, new JsonObject()));
        return workFlowProperties;
    }

    private JsonObject getInputProperties(JsonObject inputs, JsonObject dataTypes,
                                          JsonObject inputObject) {
        if (inputs == null) {
            return inputObject;
        }

        for (Map.Entry<String, JsonElement> entry : inputs.entrySet()) {
            String key = entry.getKey();
            JsonObject inputProperty = inputs.getAsJsonObject(key);
            String type = inputProperty.get(TYPE).getAsString();
            if (isComplexType(type, dataTypes)) {
                inputObject.add(key, handleComplexType(type, dataTypes));
            } else if (LIST.equalsIgnoreCase(type)) {
                handleListType(key, inputProperty, dataTypes, inputObject);
            } else if (isInputParam(inputProperty)) {
                inputObject.add(key, entry.getValue());
            }
        }
        return inputObject;
    }

    private void handleListType(String propertyName,
                                      JsonObject inputProperty,
                                      JsonObject dataTypes,
                                      JsonObject inputObject) {
        if (inputProperty.get("entry_schema") == null) {
            throw new CdsParametersException("Entry schema is null for " + propertyName);
        }

        String type = inputProperty.get("entry_schema").getAsJsonObject().get(
                TYPE).getAsString();
        if (dataTypes.get(type) != null) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty(TYPE, LIST);
            jsonObject.add(PROPERTIES, getPropertiesObject(type, dataTypes));
            inputObject.add(propertyName, jsonObject);
        } else if (isInputParam(inputProperty)) {
            inputObject.add(propertyName, inputProperty);
        }
    }

    private JsonObject handleComplexType(String key, JsonObject dataTypes) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(TYPE, "object");
        jsonObject.add(PROPERTIES, getPropertiesObject(key, dataTypes));
        return jsonObject;
    }

    private JsonObject getPropertiesObject(String key, JsonObject dataTypes) {
        JsonObject properties = dataTypes.get(key).getAsJsonObject().get(PROPERTIES).getAsJsonObject();
        JsonObject object = new JsonObject();
        getInputProperties(properties, dataTypes, object);
        return object;
    }

    private boolean isComplexType(String type, JsonObject dataTypes) {
        if (dataTypes == null) {
            return false;
        }
        return dataTypes.get(type) != null;
    }

    private boolean isInputParam(JsonObject inputProperty) {
        JsonElement inputParam = inputProperty.get("input-param");
        if (inputParam == null) {
            return false;
        }
        return parseBoolean(inputParam.getAsString());
    }

    /**
     * Creates payload to query CDS to get workflow input properties.
     *
     * @param blueprintName CDS blueprint name
     * @param version       CDS blueprint version
     * @param workflow      CDS blueprint workflow
     * @return returns payload in json format
     */
    public String getCdsPayloadForWorkFlow(String blueprintName, String version, String workflow) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("blueprintName", blueprintName);
        jsonObject.addProperty("version", version);
        jsonObject.addProperty("returnContent", "json");
        jsonObject.addProperty("workflowName", workflow);
        jsonObject.addProperty("specType", "TOSCA");
        return jsonObject.toString();
    }
}
