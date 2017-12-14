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

package org.onap.clamp.clds.client.req.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFLogger.Level;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.PolicyChain;
import org.onap.clamp.clds.model.prop.PolicyItem;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.onap.policy.api.AttributeType;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.ResourceType;
import org.onap.policy.sdc.Service;
import org.onap.policy.controlloop.policy.OperationsAccumulateParams;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.onap.policy.controlloop.policy.builder.ControlLoopPolicyBuilder;
import org.onap.policy.controlloop.policy.builder.Message;
import org.onap.policy.controlloop.policy.builder.Results;

/**
 * Construct an Operational Policy request given CLDS objects.
 */
public final class OperationalPolicyReq {
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(OperationalPolicyReq.class);

    private OperationalPolicyReq() {
    }

    /**
     * Format Operational Policy attributes.
     *
     * @param refProp
     * @param prop
     * @return
     * @throws BuilderException
     * @throws UnsupportedEncodingException
     */
    public static Map<AttributeType, Map<String, String>> formatAttributes(RefProp refProp, ModelProperties prop,
            String modelElementId, PolicyChain policyChain) throws BuilderException, UnsupportedEncodingException {
        Global global = prop.getGlobal();
        prop.setCurrentModelElementId(modelElementId);
        prop.setPolicyUniqueId(policyChain.getPolicyId());
        String templateName = "";
        String operationTopic = "";
        String notificationTopic = "";
        String controller = "";
        Tca tca = prop.getType(Tca.class);
        if (tca != null && tca.isFound()) {
            if (!global.getActionSet().equalsIgnoreCase("enbRecipe")) {
                throw new BadRequestException(
                        "Operation Policy validation problem: action set is not selected properly.");
            }
            templateName = refProp.getStringValue("op.eNodeB.templateName", global.getService());
            operationTopic = refProp.getStringValue("op.eNodeB.operationTopic", global.getService());
            notificationTopic = refProp.getStringValue("op.eNodeB.notificationTopic", global.getService());
            controller = refProp.getStringValue("op.eNodeB.controller", global.getService());
        } else {
            if (!global.getActionSet().equalsIgnoreCase("vnfRecipe")) {
                throw new BadRequestException(
                        "Operation Policy validation problem: Action set is not selected properly.");
            }
            templateName = refProp.getStringValue("op.templateName", global.getService());
            operationTopic = refProp.getStringValue("op.operationTopic", global.getService());
            notificationTopic = refProp.getStringValue("op.notificationTopic", global.getService());
            controller = refProp.getStringValue("op.controller", global.getService());
        }

        String recipeTopic = refProp.getStringValue("op.recipeTopic", global.getService());
        // ruleAttributes
        logger.info("templateName=" + templateName);
        logger.info("notificationTopic=" + notificationTopic);
        Map<String, String> ruleAttributes = new HashMap<>();
        ruleAttributes.put("templateName", templateName);
        ruleAttributes.put("ClosedLoopControlName", prop.getControlNameAndPolicyUniqueId());
        ruleAttributes.put("NotificationTopic", notificationTopic);
        if (operationTopic == null || operationTopic.isEmpty()) {
            logger.info("recipeTopic=" + recipeTopic);
            // if no operationTopic, then don't format yaml - use first policy
            // from list
            PolicyItem policyItem = policyChain.getPolicyItems().get(0);
            ruleAttributes.put("RecipeTopic", recipeTopic);
            String recipe = policyItem.getRecipe();
            String maxRetries = String.valueOf(policyItem.getMaxRetries());
            String retryTimeLimit = String.valueOf(policyItem.getRetryTimeLimit());
            String targetResourceId = String.valueOf(policyItem.getTargetResourceId());
            logger.info("recipe=" + recipe);
            logger.info("maxRetries=" + maxRetries);
            logger.info("retryTimeLimit=" + retryTimeLimit);
            logger.info("targetResourceId=" + targetResourceId);
            ruleAttributes.put("Recipe", recipe);
            ruleAttributes.put("MaxRetries", maxRetries);
            ruleAttributes.put("RetryTimeLimit", retryTimeLimit);
            ruleAttributes.put("ResourceId", targetResourceId);
        } else {
            logger.info("operationTopic=" + operationTopic);
            // format yaml
            String yaml = (tca != null && tca.isFound()) ? formateNodeBYaml(refProp, prop, modelElementId, policyChain)
                    : formatYaml(refProp, prop, modelElementId, policyChain);
            ruleAttributes.put("OperationTopic", operationTopic);
            ruleAttributes.put("ControlLoopYaml", yaml);
        }
        // matchingAttributes
        Map<String, String> matchingAttributes = new HashMap<>();
        matchingAttributes.put("controller", controller);
        Map<AttributeType, Map<String, String>> attributes = new HashMap<>();
        attributes.put(AttributeType.RULE, ruleAttributes);
        attributes.put(AttributeType.MATCHING, matchingAttributes);
        return attributes;
    }

    private static String formatYaml(RefProp refProp, ModelProperties prop, String modelElementId,
            PolicyChain policyChain) throws BuilderException, UnsupportedEncodingException {
        // get property objects
        Global global = prop.getGlobal();
        prop.setCurrentModelElementId(modelElementId);
        prop.setPolicyUniqueId(policyChain.getPolicyId());
        // convert values to SDC objects
        Service service = new Service(global.getService());
        Resource[] vfResources = convertToResource(global.getResourceVf(), ResourceType.VF);
        Resource[] vfcResources = convertToResource(global.getResourceVfc(), ResourceType.VFC);
        // create builder
        ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory.buildControlLoop(prop.getControlName(),
                policyChain.getTimeout(), service, vfResources);
        builder.addResource(vfcResources);
        // process each policy
        Map<String, Policy> policyObjMap = new HashMap<>();
        List<PolicyItem> policyItemList = orderParentFirst(policyChain.getPolicyItems());
        for (PolicyItem policyItem : policyItemList) {
            String policyName = policyItem.getRecipe() + " Policy";
            Target target = new Target();
            target.setType(TargetType.VM);
            target.setResourceID(policyItem.getTargetResourceId());
            Policy policyObj;
            if (policyItemList.indexOf(policyItem) == 0) {
                String policyDescription = policyItem.getRecipe()
                        + " Policy - the trigger (no parent) policy - created by CLDS";
                policyObj = builder.setTriggerPolicy(policyName, policyDescription,
                        refProp.getStringValue("op.policy.appc"), target, policyItem.getRecipe(), null,
                        policyItem.getMaxRetries(), policyItem.getRetryTimeLimit());
            } else {
                Policy parentPolicyObj = policyObjMap.get(policyItem.getParentPolicy());
                String policyDescription = policyItem.getRecipe() + " Policy - triggered conditionally by "
                        + parentPolicyObj.getName() + " - created by CLDS";
                policyObj = builder.setPolicyForPolicyResult(policyName, policyDescription,
                        refProp.getStringValue("op.policy.appc"), target, policyItem.getRecipe(), null,
                        policyItem.getMaxRetries(), policyItem.getRetryTimeLimit(), parentPolicyObj.getId(),
                        convertToPolicyResult(policyItem.getParentPolicyConditions()));
                logger.info("policyObj.id=" + policyObj.getId() + "; parentPolicyObj.id=" + parentPolicyObj.getId());
            }
            policyObjMap.put(policyItem.getId(), policyObj);
        }
        // Build the specification
        Results results = builder.buildSpecification();
        validate(results);
        return URLEncoder.encode(results.getSpecification(), "UTF-8");
    }

    private static String formateNodeBYaml(RefProp refProp, ModelProperties prop, String modelElementId,
            PolicyChain policyChain) throws BuilderException, UnsupportedEncodingException {
        // get property objects
        Global global = prop.getGlobal();
        prop.setCurrentModelElementId(modelElementId);
        prop.setPolicyUniqueId(policyChain.getPolicyId());
        // convert values to SDC objects
        Service service = new Service(global.getService());
        Resource[] vfResources = convertToResource(global.getResourceVf(), ResourceType.VF);
        Resource[] vfcResources = convertToResource(global.getResourceVfc(), ResourceType.VFC);
        // create builder
        ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory.buildControlLoop(prop.getControlName(),
                policyChain.getTimeout(), service, vfResources);
        builder.addResource(vfcResources);
        // process each policy
        Map<String, Policy> policyObjMap = new HashMap<>();
        List<PolicyItem> policyItemList = addAOTSActorRecipe(refProp, global.getService(),
                policyChain.getPolicyItems());
        Policy lastPolicyObj = new Policy();
        for (PolicyItem policyItem : policyItemList) {
            Target target = new Target();
            target.setType(TargetType.VM);
            target.setResourceID(policyItem.getTargetResourceId());
            String policyName = policyItem.getRecipe() + " Policy";
            Policy policyObj;
            if (policyItemList.indexOf(policyItem) == 0) {
                // To set up time window payload for trigger policy
                Map<String, String> payloadMap = new HashMap<>();
                payloadMap.put("timeWindow", refProp.getStringValue("op.eNodeB.timeWindow"));
                String policyDescription = policyItem.getRecipe()
                        + " Policy - the trigger (no parent) policy - created by CLDS";
                policyObj = builder.setTriggerPolicy(policyName, policyDescription, policyItem.getActor(), target,
                        policyItem.getRecipe(), payloadMap, policyItem.getMaxRetries(), policyItem.getRetryTimeLimit());
            } else {
                Policy parentPolicyObj = policyObjMap.get(policyItem.getParentPolicy());
                String policyDescription = policyItem.getRecipe() + " Policy - triggered conditionally by "
                        + parentPolicyObj.getName() + " - created by CLDS";
                policyObj = builder.setPolicyForPolicyResult(policyName, policyDescription, policyItem.getActor(),
                        target, policyItem.getRecipe(), null, policyItem.getMaxRetries(),
                        policyItem.getRetryTimeLimit(), parentPolicyObj.getId(),
                        convertToPolicyResult(policyItem.getParentPolicyConditions()));
                lastPolicyObj = policyObj;
                logger.info("policyObj.id=" + policyObj.getId() + "; parentPolicyObj.id=" + parentPolicyObj.getId());
            }
            policyObjMap.put(policyItem.getId(), policyObj);
        }
        // To set up operations accumulate params
        OperationsAccumulateParams operationsAccumulateParams = new OperationsAccumulateParams();
        operationsAccumulateParams.setLimit(Integer.valueOf(refProp.getStringValue("op.eNodeB.limit")));
        operationsAccumulateParams.setPeriod(refProp.getStringValue("op.eNodeB.period"));
        builder.addOperationsAccumulateParams(lastPolicyObj.getId(), operationsAccumulateParams);
        // Build the specification
        Results results = builder.buildSpecification();
        validate(results);
        return URLEncoder.encode(results.getSpecification(), "UTF-8");
    }

    private static void validate (Results results) {
        if (results.isValid()) {
            logger.info("results.getSpecification()=" + results.getSpecification());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Operation Policy validation problem: ControlLoopPolicyBuilder failed with following messages: ");
            for (Message message : results.getMessages()) {
                sb.append(message.getMessage());
                sb.append("; ");
            }
            throw new BadRequestException(sb.toString());
        }
    }

     // Adding AOTS actor and other recipe for yaml
     private static List<PolicyItem> addAOTSActorRecipe(RefProp refProp, String service, List<PolicyItem> inOrigList) {
        List<PolicyItem> outList = new ArrayList<>();
        try {
            PolicyItem policyItem = inOrigList.get(0);
            ObjectNode rootNode = (ObjectNode) refProp.getJsonTemplate("op.eNodeB.recipe", service);
            Iterator<JsonNode> itr = rootNode.get("eNodeBRecipes").elements();
            while (itr.hasNext()) {
                PolicyItem policyItemObj = (PolicyItem) policyItem.clone();
                JsonNode recipeNode = itr.next();
                policyItemObj.setId(recipeNode.path("Recipe").asText());
                policyItemObj.setActor(recipeNode.path("Actor").asText());
                policyItemObj.setRecipe(recipeNode.path("Recipe").asText());
                policyItemObj.setParentPolicy(recipeNode.path("ParentPolicy").asText());
                if (!recipeNode.path("Retry").asText().isEmpty()) {
                    policyItemObj.setMaxRetries(Integer.parseInt(recipeNode.path("Retry").asText()));
                }
                if (!recipeNode.path("TimeLimit").asText().isEmpty()) {
                    policyItemObj.setRetryTimeLimit(Integer.parseInt(recipeNode.path("TimeLimit").asText()));
                }
                if (!recipeNode.path("PPConditions").asText().isEmpty()) {
                    List<String> parentPolicyConditions = new ArrayList<>();
                    for (String ppCondition : recipeNode.path("PPConditions").asText().split(",")) {
                        parentPolicyConditions.add(ppCondition);
                    }
                    policyItemObj.setParentPolicyConditions(parentPolicyConditions);
                }
                outList.add(policyItemObj);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, "Error", e);
        }
        return outList;
    }

     // Order list of PolicyItems so that parents come before any of their children
     private static List<PolicyItem> orderParentFirst(List<PolicyItem> inOrigList) {
        List<PolicyItem> inList = new ArrayList<>();
        inList.addAll(inOrigList);
        List<PolicyItem> outList = new ArrayList<>();
        int prevSize = 0;
        while (!inList.isEmpty()) {
            // check if there's a loop in the policy chain (the inList should
            // have been reduced by at least one)
            if (inList.size() == prevSize) {
                throw new BadRequestException("Operation Policy validation problem: loop in Operation Policy chain");
            }
            prevSize = inList.size();
            // the following loop should remove at least one PolicyItem from the
            // inList
            Iterator<PolicyItem> inListItr = inList.iterator();
            while (inListItr.hasNext()) {
                PolicyItem inItem = inListItr.next();
                // check for trigger policy (no parent)
                String parent = inItem.getParentPolicy();
                if (parent == null || parent.length() == 0) {
                    if (!outList.isEmpty()) {
                        throw new BadRequestException(
                                "Operation Policy validation problem: more than one trigger policy");
                    } else {
                        outList.add(inItem);
                        inListItr.remove();
                    }
                } else {
                    // check if this PolicyItem's parent has been processed
                    for (PolicyItem outItem : outList) {
                        if (outItem.getId().equals(parent)) {
                            // if the inItem parent is already in the outList,
                            // then add inItem to outList and remove from inList
                            outList.add(inItem);
                            inListItr.remove();
                            break;
                        }
                    }
                }
            }
        }
        return outList;
    }

    private static Resource[] convertToResource(List<String> stringList, ResourceType resourceType) {
        if (stringList == null || stringList.isEmpty()) {
            return new Resource[0];
        }
        return stringList.stream().map(stringElem -> new Resource(stringElem, resourceType)).toArray(Resource[]::new);
    }

    private static PolicyResult[] convertToPolicyResult(List<String> prList) {
        if (prList == null || prList.isEmpty()) {
            return new PolicyResult[0];
        }
        return prList.stream().map(stringElem -> PolicyResult.toResult(stringElem)).toArray(PolicyResult[]::new);
    }
}