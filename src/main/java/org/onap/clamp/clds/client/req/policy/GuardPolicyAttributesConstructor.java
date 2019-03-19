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

public class GuardPolicyAttributesConstructor {
    public static final String ACTOR = "actor";
    public static final String RECIPE = "recipe";
    public static final String TARGETS = "targets";
    public static final String CLNAME = "clname";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String LIMIT = "limit";
    public static final String TIME_WINDOW = "timeWindow";
    public static final String TIME_UNITS = "timeUnits";
    public static final String GUARD_ACTIVE_START = "guardActiveStart";
    public static final String GUARD_ACTIVE_END = "guardActiveEnd";

    private static final EELFLogger logger = EELFManager.getInstance()
        .getLogger(GuardPolicyAttributesConstructor.class);

    private GuardPolicyAttributesConstructor() {
    }

    public static Map<AttributeType, Map<String, String>> formatAttributes(ModelProperties modelProperties,
        PolicyItem policyItem) {
        Map<String, String> matchingAttributes = prepareMatchingAttributes(policyItem, modelProperties);
        return createAttributesMap(matchingAttributes);
    }

    /**
     * Get all the Guard policies from the policy chain.
     * @param policyChain The policy chain
     * @return The list of guard policies
     */
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
        matchingAttributes.put(ACTOR, policyItem.getActor());
        matchingAttributes.put(RECIPE, policyItem.getRecipe());
        matchingAttributes.put(TARGETS, policyItem.getGuardTargets());
        matchingAttributes.put(CLNAME, modelProp.getControlNameAndPolicyUniqueId());
        if ("GUARD_MIN_MAX".equals(policyItem.getGuardPolicyType())) {
            matchingAttributes.put(MIN, policyItem.getMinGuard());
            matchingAttributes.put(MAX, policyItem.getMaxGuard());
        } else if ("GUARD_YAML".equals(policyItem.getGuardPolicyType())) {
            matchingAttributes.put(LIMIT, policyItem.getLimitGuard());
            matchingAttributes.put(TIME_WINDOW, policyItem.getTimeWindowGuard());
            matchingAttributes.put(TIME_UNITS, policyItem.getTimeUnitsGuard());
        }
        matchingAttributes.put(GUARD_ACTIVE_START, policyItem.getGuardActiveStart());
        matchingAttributes.put(GUARD_ACTIVE_END, policyItem.getGuardActiveEnd());

        logger.info("Prepared: " + matchingAttributes);
        return matchingAttributes;
    }

    private static Map<AttributeType, Map<String, String>> createAttributesMap(Map<String, String> matchingAttributes) {
        Map<AttributeType, Map<String, String>> attributes = new HashMap<>();
        attributes.put(AttributeType.MATCHING, matchingAttributes);
        return attributes;
    }
}
