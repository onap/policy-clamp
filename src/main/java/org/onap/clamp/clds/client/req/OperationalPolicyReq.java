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

package org.onap.clamp.clds.client.req;

import org.onap.clamp.clds.model.prop.Global;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Policy;
import org.onap.clamp.clds.model.prop.PolicyItem;
import org.openecomp.policy.controlloop.policy.TargetType;
import org.openecomp.policy.controlloop.policy.PolicyResult;
import org.openecomp.policy.controlloop.policy.Target;
import org.openecomp.policy.controlloop.policy.builder.BuilderException;
import org.openecomp.policy.controlloop.policy.builder.ControlLoopPolicyBuilder;
import org.openecomp.policy.controlloop.policy.builder.Message;
import org.openecomp.policy.controlloop.policy.builder.Results;
import org.openecomp.policy.api.AttributeType;
import org.openecomp.policy.asdc.Resource;
import org.openecomp.policy.asdc.ResourceType;
import org.openecomp.policy.asdc.Service;
import org.onap.clamp.clds.model.refprop.RefProp;
import org.jboss.resteasy.spi.BadRequestException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;


/**
 * Construct an Operational Policy request given CLDS objects.
 */
public class OperationalPolicyReq {
    // currently uses the java.util.logging.Logger like the Camunda engine
    private static final Logger logger = Logger.getLogger(OperationalPolicyReq.class.getName());


    /**
     * Format Operational Policy attributes.
     *
     * @param refProp
     * @param prop
     * @return
     * @throws BuilderException
     * @throws UnsupportedEncodingException
     */
    public static Map<AttributeType, Map<String, String>> formatAttributes(RefProp refProp, ModelProperties prop) throws BuilderException, UnsupportedEncodingException {
        Global global = prop.getGlobal();
        Policy policy = prop.getPolicy();
        prop.setCurrentModelElementId(policy.getId());

        String templateName = refProp.getStringValue("op.templateName", global.getService());
        String recipeTopic = refProp.getStringValue("op.recipeTopic", global.getService());
        String operationTopic = refProp.getStringValue("op.operationTopic", global.getService());
        String notificationTopic = refProp.getStringValue("op.notificationTopic", global.getService());

        // ruleAttributes
        Map<String, String> ruleAttributes = new HashMap<>();

        if (operationTopic == null || operationTopic.length() == 0) {
            logger.info("templateName=" + templateName);
            logger.info("recipeTopic=" + recipeTopic);
            logger.info("notificationTopic=" + notificationTopic);

            // if no operationTopic, then don't format yaml - use first policy from list
            PolicyItem policyItem = policy.getPolicyItems().get(0);

            ruleAttributes.put("templateName", templateName);
            ruleAttributes.put("ClosedLoopControlName", prop.getControlName());
            ruleAttributes.put("RecipeTopic", recipeTopic);
            ruleAttributes.put("NotificationTopic", notificationTopic);

            String recipe = policyItem.getRecipe();
            String maxRetries = String.valueOf(policyItem.getMaxRetries());
            String retryTimeLimit = String.valueOf(policyItem.getRetryTimeLimit());
            logger.info("recipe=" + recipe);
            logger.info("maxRetries=" + maxRetries);
            logger.info("retryTimeLimit=" + retryTimeLimit);
            ruleAttributes.put("Recipe", recipe);
            ruleAttributes.put("MaxRetries", maxRetries);
            ruleAttributes.put("RetryTimeLimit", retryTimeLimit);
        } else {
            logger.info("templateName=" + templateName);
            logger.info("operationTopic=" + operationTopic);
            logger.info("notificationTopic=" + notificationTopic);

            // format yaml
            String yaml = formatYaml(refProp, prop);

            ruleAttributes.put("templateName", templateName);
            ruleAttributes.put("ClosedLoopControlName", prop.getControlName());
            ruleAttributes.put("OperationTopic", operationTopic);
            ruleAttributes.put("NotificationTopic", notificationTopic);

            ruleAttributes.put("ControlLoopYaml", yaml);
        }

        // matchingAttributes
        String controller = refProp.getStringValue("op.controller", global.getService());

        Map<String, String> matchingAttributes = new HashMap<>();
        matchingAttributes.put("controller", controller);

        Map<AttributeType, Map<String, String>> attributes = new HashMap<>();
        attributes.put(AttributeType.RULE, ruleAttributes);
        attributes.put(AttributeType.MATCHING, matchingAttributes);


        return attributes;
    }


    /**
     * Format Operational Policy yaml.
     *
     * @param refProp
     * @param prop
     * @return
     * @throws BuilderException
     * @throws UnsupportedEncodingException
     */
    private static String formatYaml(RefProp refProp, ModelProperties prop) throws BuilderException, UnsupportedEncodingException {
        // get property objects
        Global global = prop.getGlobal();
        Policy policy = prop.getPolicy();
        prop.setCurrentModelElementId(policy.getId());

        // convert values to ASDC objects
        Service service = new Service(global.getService());
        Resource[] vfResources = convertToResource(global.getResourceVf(), ResourceType.VF);
        Resource[] vfcResources = convertToResource(global.getResourceVfc(), ResourceType.VFC);

        // create builder
        ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory.buildControlLoop(prop.getControlName(), policy.getTimeout(), service, vfResources);
        builder.addResource(vfcResources);

        // process each policy
        HashMap<String, org.openecomp.policy.controlloop.policy.Policy> policyObjMap = new HashMap<>();
        List<PolicyItem> policyItemList = orderParentFirst(policy.getPolicyItems());
        for (int i = 0; i < policyItemList.size(); i++) {
            org.openecomp.policy.controlloop.policy.Policy policyObj;
            PolicyItem policyItem = policyItemList.get(i);
            String policyName = policyItem.getRecipe() + " Policy";
            if (i == 0) {
                String policyDescription = policyItem.getRecipe() + " Policy - the trigger (no parent) policy - created by CLDS";
                policyObj = builder.setTriggerPolicy(
                        policyName,
                        policyDescription,
                        "APPC",
                        new Target(TargetType.VM),
                        policyItem.getRecipe(),
                        new HashMap<>(), //TODO To verify !
                        policyItem.getMaxRetries(),
                        policyItem.getRetryTimeLimit());
            } else {
                org.openecomp.policy.controlloop.policy.Policy parentPolicyObj = policyObjMap.get(policyItem.getParentPolicy());
                String policyDescription = policyItem.getRecipe() + " Policy - triggered conditionally by " + parentPolicyObj.getName() + " - created by CLDS";
                policyObj = builder.setPolicyForPolicyResult(
                        policyName,
                        policyDescription,
                        "APPC",
                        new Target(TargetType.VM),
                        policyItem.getRecipe(),
                        new HashMap<>(), //TODO To verify !
                        policyItem.getMaxRetries(),
                        policyItem.getRetryTimeLimit(),
                        parentPolicyObj.getId(),
                        convertToPolicyResult(policyItem.getParentPolicyConditions()));
                logger.info("policyObj.id=" + policyObj.getId() + "; parentPolicyObj.id=" + parentPolicyObj.getId());
            }
            policyObjMap.put(policyItem.getId(), policyObj);
        }

        //
        // Build the specification
        //
        Results results = builder.buildSpecification();
        if (results.isValid()) {
            logger.info("results.getSpecification()=" + results.getSpecification());
        } else {
            // throw exception with error info
            StringBuilder sb = new StringBuilder();
            sb.append("Operation Policy validation problem: ControlLoopPolicyBuilder failed with following messages: ");
            for (Message message : results.getMessages()) {
                sb.append(message.getMessage());
                sb.append("; ");
            }
            throw new BadRequestException(sb.toString());
        }
        return URLEncoder.encode(results.getSpecification(), "UTF-8");
    }

    /**
     * Order list of PolicyItems so that parents come before any of their children
     *
     * @param inOrigList
     * @return
     */
    private static List<PolicyItem> orderParentFirst(List<PolicyItem> inOrigList) {
        List<PolicyItem> inList = new ArrayList<>();
        inList.addAll(inOrigList);
        List<PolicyItem> outList = new ArrayList<>();
        int prevSize = 0;
        while (!inList.isEmpty()) {
            // check if there's a loop in the policy chain (the inList should have been reduced by at least one)
            if (inList.size() == prevSize) {
                throw new BadRequestException("Operation Policy validation problem: loop in Operation Policy chain");
            }
            prevSize = inList.size();
            // the following loop should remove at least one PolicyItem from the inList
            Iterator<PolicyItem> inListItr = inList.iterator();
            while (inListItr.hasNext()) {
                PolicyItem inItem = inListItr.next();
                // check for trigger policy (no parent)
                String parent = inItem.getParentPolicy();
                if (parent == null || parent.length() == 0) {
                    if (outList.size() > 0) {
                        throw new BadRequestException("Operation Policy validation problem: more than one trigger policy");
                    } else {
                        outList.add(inItem);
                        inListItr.remove();
                    }
                } else {
                    // check if this PolicyItem's parent has been processed
                    for (PolicyItem outItem : outList) {
                        if (outItem.getId().equals(parent)) {
                            // if the inItem parent is already in the outList, then add inItem to outList and remove from inList
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


    /**
     * Convert a List of resource strings to an array of Resource objects.
     *
     * @param rList
     * @param resourceType
     * @return
     */
    private static Resource[] convertToResource(List<String> rList, ResourceType resourceType) {
        int size = 0;
        if (rList != null) {
            size = rList.size();
        }
        Resource[] rArray = new Resource[size];
        for (int i = 0; i < size; i++) {
            String rString = rList.get(i);
            rArray[i] = new Resource(rString, resourceType);
        }
        return rArray;
    }

    /**
     * Convert a List of policy result strings to an array of PolicyResult objects.
     *
     * @param prList
     * @return
     */
    private static PolicyResult[] convertToPolicyResult(List<String> prList) {
        int size = 0;
        if (prList != null) {
            size = prList.size();
        }
        PolicyResult[] prArray = new PolicyResult[size];
        for (int i = 0; i < size; i++) {
            String prString = prList.get(i);
            prArray[i] = PolicyResult.toResult(prString);
        }
        return prArray;
    }

}