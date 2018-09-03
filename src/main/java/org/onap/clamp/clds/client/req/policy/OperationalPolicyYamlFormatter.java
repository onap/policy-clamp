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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.BadRequestException;

import org.onap.clamp.clds.model.properties.Global;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.policy.controlloop.policy.Policy;
import org.onap.policy.controlloop.policy.PolicyResult;
import org.onap.policy.controlloop.policy.Target;
import org.onap.policy.controlloop.policy.TargetType;
import org.onap.policy.controlloop.policy.builder.BuilderException;
import org.onap.policy.controlloop.policy.builder.ControlLoopPolicyBuilder;
import org.onap.policy.controlloop.policy.builder.Message;
import org.onap.policy.controlloop.policy.builder.Results;
import org.onap.policy.sdc.Resource;
import org.onap.policy.sdc.ResourceType;
import org.onap.policy.sdc.Service;

public class OperationalPolicyYamlFormatter {
    private static final EELFLogger logger = EELFManager.getInstance().getLogger(OperationalPolicyYamlFormatter.class);

    protected OperationalPolicyYamlFormatter() {
    }
    /**
     * Format Operational OpenLoop Policy yaml.
     *
     * @param prop
     * @param modelElementId
     * @param policyChain
     * @return
     * @throws BuilderException
     * @throws UnsupportedEncodingException
     */
    public static String formatOpenLoopYaml(ModelProperties prop, String modelElementId,
        PolicyChain policyChain) throws BuilderException, UnsupportedEncodingException {
        // get property objects
        Global global = prop.getGlobal();
        prop.setCurrentModelElementId(modelElementId);
        prop.setPolicyUniqueId(policyChain.getPolicyId());
        // convert values to SDC objects
        Service service = new Service(global.getService());
        Resource[] vfResources = convertToResources(global.getResourceVf(), ResourceType.VF);
        // create builder
        ControlLoopPolicyBuilder builder = ControlLoopPolicyBuilder.Factory.buildControlLoop(prop.getControlName(),
            policyChain.getTimeout(), service, vfResources);
        // builder.setTriggerPolicy(refProp.getStringValue("op.openloop.policy"));
        // Build the specification
        Results results = builder.buildSpecification();
        validate(results);
        return URLEncoder.encode(results.getSpecification(), "UTF-8");
    }

    public static String formatYaml(ModelProperties prop, String modelElementId,
        PolicyChain policyChain) throws BuilderException, UnsupportedEncodingException {
        // get property objects
        Global global = prop.getGlobal();
        prop.setCurrentModelElementId(modelElementId);
        prop.setPolicyUniqueId(policyChain.getPolicyId());
        // convert values to SDC objects
        Service service = new Service(global.getService());
        Resource[] vfResources = convertToResources(global.getResourceVf(), ResourceType.VF);
        Resource[] vfcResources = convertToResources(global.getResourceVfc(), ResourceType.VFC);
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
            // We can send target type as VM/VNF for most of recipes
            if (policyItem.getRecipeLevel() != null && !policyItem.getRecipeLevel().isEmpty()) {
                target.setType(TargetType.valueOf(policyItem.getRecipeLevel()));
            }
            target.setResourceID(policyItem.getTargetResourceId());
            String actor = policyItem.getActor();
            Map<String, String> payloadMap = policyItem.getRecipePayload();
            Policy policyObj;
            if (policyItemList.indexOf(policyItem) == 0) {
                String policyDescription = policyItem.getRecipe()
                    + " Policy - the trigger (no parent) policy - created by CLDS";
                policyObj = builder.setTriggerPolicy(policyName, policyDescription, actor, target,
                    policyItem.getRecipe(), payloadMap, policyItem.getMaxRetries(), policyItem.getRetryTimeLimit());
            } else {
                Policy parentPolicyObj = policyObjMap.get(policyItem.getParentPolicy());
                String policyDescription = policyItem.getRecipe() + " Policy - triggered conditionally by "
                    + parentPolicyObj.getName() + " - created by CLDS";
                policyObj = builder.setPolicyForPolicyResult(policyName, policyDescription, actor, target,
                    policyItem.getRecipe(), payloadMap, policyItem.getMaxRetries(), policyItem.getRetryTimeLimit(),
                    parentPolicyObj.getId(), convertToPolicyResults(policyItem.getParentPolicyConditions()));
                logger.info("policyObj.id=" + policyObj.getId() + "; parentPolicyObj.id=" + parentPolicyObj.getId());
            }
            policyObjMap.put(policyItem.getId(), policyObj);
        }
        // Build the specification
        Results results = builder.buildSpecification();
        validate(results);
        return URLEncoder.encode(results.getSpecification(), "UTF-8");
    }

    protected static List<PolicyItem> orderParentFirst(List<PolicyItem> inOrigList) {
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

    protected static void validate(Results results) {
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
    }


    protected static Resource[] convertToResources(List<String> stringList, ResourceType resourceType) {
        if (stringList == null || stringList.isEmpty()) {
            return new Resource[0];
        }
        return stringList.stream().map(stringElem -> new Resource(stringElem, resourceType)).toArray(Resource[]::new);
    }

    protected static PolicyResult[] convertToPolicyResults(List<String> prList) {
        if (prList == null || prList.isEmpty()) {
            return new PolicyResult[0];
        }
        return prList.stream().map(PolicyResult::toResult).toArray(PolicyResult[]::new);
    }

}