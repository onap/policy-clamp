/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.client.req.tca;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Map;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.exception.TcaRequestFormatterException;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.Tca;
import org.onap.clamp.clds.model.properties.TcaItem;
import org.onap.clamp.clds.model.properties.TcaThreshold;
import org.yaml.snakeyaml.Yaml;

/**
 * Construct the requests for TCA policy and SDC.
 */
public class TcaRequestFormatter {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(TcaRequestFormatter.class);
    protected static final EELFLogger metricsLogger = EELFManager.getInstance().getMetricsLogger();

    /**
     * Hide the default constructor.
     */
    private TcaRequestFormatter() {
    }

    /**
     * Format Tca Policy JSON request.
     *
     * @param refProp
     *            The refProp generally created by Spring, it's an access on the
     *            clds-references.properties file
     * @param modelProperties
     *            The Model Prop created from BPMN JSON and BPMN properties JSON
     * @return The Json string containing that should be sent to policy
     */
    public static String createPolicyJson(ClampProperties refProp, ModelProperties modelProperties) {
        try {
            String service = modelProperties.getGlobal().getService();
            Tca tca = modelProperties.getType(Tca.class);
            modelProperties.setCurrentModelElementId(tca.getId());
            ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("tca.policy.template", service);
            String policyName = modelProperties.getCurrentPolicyScopeAndPolicyName();
            rootNode.put("policyName", policyName);
            ((ObjectNode) rootNode.get("content")).replace("tca_policy",
                    createPolicyContent(refProp, modelProperties, service, policyName, tca));
            String tcaPolicyReq = rootNode.toString();
            logger.info("tcaPolicyReq=" + tcaPolicyReq);
            return tcaPolicyReq;
        } catch (IOException e) {
            throw new TcaRequestFormatterException("Exception caught when attempting to create the policy JSON", e);
        }
    }

    /**
     * Format Tca Policy Content JSON
     *
     * @param refProp
     *            The refProp generally created by Spring, it's an access on the
     *            clds-references.properties file
     * @param modelProperties
     *            The Model Prop created from BPMN JSON and BPMN properties JSON
     * @param service
     *            The service ID, if not specified getGlobal.getService will be
     *            used
     * @param policyName
     *            The policyName, if not specified the
     *            modelProperties.getCurrentPolicyScopeAndPolicyName will be
     *            used
     * @param tca
     *            The Tca object, if not specified the
     *            modelProperties.setCurrentModelElementId will be used
     * @return The Json node containing what should be sent to policy
     */
    public static JsonNode createPolicyContent(ClampProperties refProp, ModelProperties modelProperties, String service,
            String policyName, Tca tca) {
        try {
            String serviceToUse = service;
            String policyNameToUse = policyName;
            Tca tcaToUse = tca;
            if (serviceToUse == null) {
                serviceToUse = modelProperties.getGlobal().getService();
            }
            if (tcaToUse == null) {
                tcaToUse = modelProperties.getType(Tca.class);
                modelProperties.setCurrentModelElementId(tcaToUse.getId());
            }
            if (policyNameToUse == null) {
                policyNameToUse = modelProperties.getCurrentPolicyScopeAndPolicyName();
            }
            ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("tca.template", serviceToUse);
            ((ObjectNode) rootNode.get("metricsPerEventName").get(0)).put("eventName",
                    tcaToUse.getTcaItem().getEventName());
            ((ObjectNode) rootNode.get("metricsPerEventName").get(0)).put("policyName", policyNameToUse);
            ((ObjectNode) rootNode.get("metricsPerEventName").get(0)).put("controlLoopSchemaType",
                    tcaToUse.getTcaItem().getControlLoopSchemaType());
            ObjectNode thresholdsParent = ((ObjectNode) rootNode.get("metricsPerEventName").get(0));
            addThresholds(refProp, serviceToUse, thresholdsParent, tcaToUse.getTcaItem(), modelProperties);
            logger.info("tcaPolicyContent=" + rootNode.toString());
            return rootNode;
        } catch (IOException e) {
            throw new TcaRequestFormatterException("Exception caught when attempting to create the policy content JSON",
                    e);
        }
    }

    /**
     * Add threshold values to the existing policy JSON.
     *
     * @param refProp
     *            The refProp generally created by Spring, it's an access on the
     *            clds-references.properties file
     * @param service
     *            The Service value extracted from Global section of the Bpmn
     *            Properties JSON
     * @param appendToNode
     *            The JSON structure from where the thresholds section must be
     *            added
     * @param tcaItem
     *            The TCA item contained in the Tca object
     * @param modelProperties
     *            The Model Properties created from BPMN JSON and BPMN
     *            properties JSON
     */
    private static void addThresholds(ClampProperties refProp, String service, ObjectNode appendToNode, TcaItem tcaItem,
            ModelProperties modelProperties) {
        ArrayNode tcaNodes = appendToNode.withArray("thresholds");
        ObjectNode tcaNode;
        try {
            tcaNode = (ObjectNode) refProp.getJsonTemplate("tca.thresholds.template", service);
            for (TcaThreshold tcaThreshold : tcaItem.getTcaThresholds()) {
                tcaNode.put("closedLoopControlName", modelProperties.getControlNameAndPolicyUniqueId());
                tcaNode.put("fieldPath", tcaThreshold.getFieldPath());
                tcaNode.put("thresholdValue", tcaThreshold.getThreshold());
                tcaNode.put("direction", tcaThreshold.getOperator());
                tcaNode.put("closedLoopEventStatus", tcaThreshold.getClosedLoopEventStatus());
                tcaNodes.add(tcaNode);
            }
        } catch (IOException e) {
            throw new TcaRequestFormatterException("Exception caught when attempting to create the thresholds JSON", e);
        }
    }

    /**
     * This method updates the blueprint that is received in the UI with the TCA
     * Json.
     * 
     * @param refProp
     *            * The refProp generally created by Spring, it's an access on
     *            the clds-references.properties file
     * @param modelProperties
     *            The Model Prop created from BPMN JSON and BPMN properties JSON
     * @param yamlValue
     *            The yaml string received from the UI
     * @return The updated YAML as a string
     */
    public static String updatedBlueprintWithConfiguration(ClampProperties refProp, ModelProperties modelProperties,
            String yamlValue) {
        String jsonPolicy = ((ObjectNode) createPolicyContent(refProp, modelProperties, null, null, null)).toString();
        logger.info("Yaml that will be updated:" + yamlValue);
        Yaml yaml = new Yaml();
        Map<String, Object> loadedYaml = (Map<String, Object>) yaml.load(yamlValue);
        Map<String, Object> nodeTemplates = (Map<String, Object>) loadedYaml.get("node_templates");
        Map<String, Object> tcaObject = (Map<String, Object>) nodeTemplates.get("tca_tca");
        Map<String, Object> propsObject = (Map<String, Object>) tcaObject.get("properties");
        Map<String, Object> appPreferences = (Map<String, Object>) propsObject.get("app_preferences");
        appPreferences.put("tca_policy", jsonPolicy);
        String blueprint = yaml.dump(loadedYaml);
        logger.info("Yaml updated:" + blueprint);
        return blueprint;
    }
}