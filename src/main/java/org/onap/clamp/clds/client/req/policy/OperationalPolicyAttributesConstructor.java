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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.clamp.clds.client.req.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.policy.api.AttributeType;
import org.onap.policy.controlloop.policy.builder.BuilderException;

public class OperationalPolicyAttributesConstructor {

    private static final EELFLogger logger = EELFManager.getInstance()
        .getLogger(OperationalPolicyAttributesConstructor.class);
    public static final String TEMPLATE_NAME = "templateName";
    public static final String CLOSED_LOOP_CONTROL_NAME = "closedLoopControlName";
    public static final String NOTIFICATION_TOPIC = "notificationTopic";
    public static final String OPERATION_TOPIC = "operationTopic";
    public static final String CONTROL_LOOP_YAML = "controlLoopYaml";
    public static final String CONTROLLER = "controller";
    public static final String RECIPE = "Recipe";
    public static final String MAX_RETRIES = "MaxRetries";
    public static final String RETRY_TIME_LIMIT = "RetryTimeLimit";
    public static final String RESOURCE_ID = "ResourceId";
    public static final String RECIPE_TOPIC = "RecipeTopic";

    private OperationalPolicyAttributesConstructor() {
    }

    public static Map<AttributeType, Map<String, String>> formatAttributes(ClampProperties refProp,
        ModelProperties modelProperties,
        String modelElementId, PolicyChain policyChain)
            throws BuilderException, UnsupportedEncodingException {
        modelProperties.setCurrentModelElementId(modelElementId);
        modelProperties.setPolicyUniqueId(policyChain.getPolicyId());

        String globalService = modelProperties.getGlobal().getService();

        Map<String, String> ruleAttributes = prepareRuleAttributes(refProp, modelProperties, modelElementId,
            policyChain, globalService);
        Map<String, String> matchingAttributes = prepareMatchingAttributes(refProp, globalService);

        return createAttributesMap(matchingAttributes, ruleAttributes);
    }

    private static Map<String, String> prepareRuleAttributes(ClampProperties clampProperties, ModelProperties modelProperties,
        String modelElementId, PolicyChain policyChain, String globalService)
            throws BuilderException, UnsupportedEncodingException {
        logger.info("Preparing rule attributes...");
        String templateName = clampProperties.getStringValue("op.templateName", globalService);
        String operationTopic = clampProperties.getStringValue("op.operationTopic", globalService);
        String notificationTopic = clampProperties.getStringValue("op.notificationTopic", globalService);

        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put(TEMPLATE_NAME, templateName);
        ruleAttributes.put(CLOSED_LOOP_CONTROL_NAME, modelProperties.getControlNameAndPolicyUniqueId());
        ruleAttributes.put(NOTIFICATION_TOPIC, notificationTopic);

        ImmutableMap<String, String> attributes = createRuleAttributesFromPolicy(clampProperties, modelProperties,
            modelElementId, policyChain, globalService, operationTopic);
        ruleAttributes.putAll(attributes);
        logger.info("Prepared: " + ruleAttributes);
        return ruleAttributes;
    }

    private static Map<String, String> prepareMatchingAttributes(ClampProperties refProp, String globalService) {
        logger.info("Preparing matching attributes...");
        String controller = refProp.getStringValue("op.controller", globalService);
        Map<String, String> matchingAttributes = new HashMap<>();
        matchingAttributes.put(CONTROLLER, controller);
        logger.info("Prepared: " + matchingAttributes);
        return matchingAttributes;
    }

    private static Map<AttributeType, Map<String, String>> createAttributesMap(Map<String, String> matchingAttributes,
        Map<String, String> ruleAttributes) {
        Map<AttributeType, Map<String, String>> attributes = new HashMap<>();
        attributes.put(AttributeType.RULE, ruleAttributes);
        attributes.put(AttributeType.MATCHING, matchingAttributes);
        return attributes;
    }

    private static ImmutableMap<String, String> createRuleAttributesFromPolicy(ClampProperties refProp, ModelProperties modelProperties,
        String modelElementId, PolicyChain policyChain,
        String globalService, String operationTopic)
            throws BuilderException, UnsupportedEncodingException {
        if (Strings.isNullOrEmpty(operationTopic)) {
            // if no operationTopic, then don't format yaml - use first policy
            String recipeTopic = refProp.getStringValue("op.recipeTopic", globalService);
            return createRuleAttributesFromPolicyItem(
                policyChain.getPolicyItems().get(0), recipeTopic);
        } else {
            return createRuleAttributesFromPolicyChain(policyChain, modelProperties,
                modelElementId, operationTopic);
        }
    }

    private static ImmutableMap<String, String> createRuleAttributesFromPolicyItem(PolicyItem policyItem, String recipeTopic) {
        logger.info("recipeTopic=" + recipeTopic);
        return ImmutableMap.<String, String>builder()
            .put(RECIPE_TOPIC, recipeTopic)
            .put(RECIPE, policyItem.getRecipe())
            .put(MAX_RETRIES, String.valueOf(policyItem.getMaxRetries()))
            .put(RETRY_TIME_LIMIT, String.valueOf(policyItem.getRetryTimeLimit()))
            .put(RESOURCE_ID, String.valueOf(policyItem.getTargetResourceId()))
            .build();
    }

    private static ImmutableMap<String, String> createRuleAttributesFromPolicyChain(PolicyChain policyChain,
        ModelProperties modelProperties,
        String modelElementId,
        String operationTopic)
            throws BuilderException, UnsupportedEncodingException {
        logger.info("operationTopic=" + operationTopic);
        String yaml = OperationalPolicyYamlFormatter.formatYaml(modelProperties, modelElementId, policyChain);
        return ImmutableMap.<String, String>builder()
            .put(OPERATION_TOPIC, operationTopic)
            .put(CONTROL_LOOP_YAML, yaml)
            .build();
    }
}