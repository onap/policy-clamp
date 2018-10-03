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
 *
 */

package org.onap.clamp.clds.client.req.policy;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.PolicyChain;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.policy.api.AttributeType;
import org.onap.policy.api.RuleProvider;

public class GuardPolicyAttributesConstructor {
    private static final EELFLogger logger = EELFManager.getInstance()
        .getLogger(GuardPolicyAttributesConstructor.class);

    private GuardPolicyAttributesConstructor() {
    }

    public static Map<AttributeType, Map<String, String>> formatAttributes(ModelProperties modelProperties, PolicyItem policyItem) {
        Map<String, String> matchingAttributes = prepareMatchingAttributes(policyItem, modelProperties);
        return createAttributesMap(matchingAttributes);
    }

    public static List<PolicyItem> getAllPolicyGuardsFromPolicyChain(PolicyChain policyChain) {
        List<PolicyItem> listItem = new ArrayList<>();
        for (PolicyItem policyItem : policyChain.getPolicyItems()) {
            if ("on".equals(policyItem.getEnableGuardPolicy())) {
                listItem.add(policyItem);
            }
        }
        return listItem;
    }

    private static Map<String, String> prepareMatchingAttributes(PolicyItem policyItem, ModelProperties modelProp) {
        logger.info("Preparing matching attributes for guard...");
        Map<String, String> matchingAttributes = new HashMap<>();
        matchingAttributes.put("actor",policyItem.getActor());
        matchingAttributes.put("recipe",policyItem.getRecipe());
        matchingAttributes.put("targets",policyItem.getGuardTargets());
        matchingAttributes.put("clname",modelProp.getControlNameAndPolicyUniqueId());
        if (RuleProvider.GUARD_MIN_MAX.equals(RuleProvider.valueOf(policyItem.getGuardPolicyType()))) {
            matchingAttributes.put("min",policyItem.getMinGuard());
            matchingAttributes.put("max",policyItem.getMaxGuard());
        } else if (RuleProvider.GUARD_YAML.equals(RuleProvider.valueOf(policyItem.getGuardPolicyType()))) {
            matchingAttributes.put("limit",policyItem.getLimitGuard());
            matchingAttributes.put("timeWindow",policyItem.getTimeWindowGuard());
            matchingAttributes.put("timeUnits",policyItem.getTimeUnitsGuard());
        }
        matchingAttributes.put("guardActiveStart",policyItem.getGuardActiveStart());
        matchingAttributes.put("guardActiveEnd",policyItem.getGuardActiveEnd());

        logger.info("Prepared: " + matchingAttributes);
        return matchingAttributes;
    }

    private static Map<AttributeType, Map<String, String>> createAttributesMap(Map<String, String> matchingAttributes) {
        Map<AttributeType, Map<String, String>> attributes = new HashMap<>();
        attributes.put(AttributeType.MATCHING, matchingAttributes);
        return attributes;
    }
}
