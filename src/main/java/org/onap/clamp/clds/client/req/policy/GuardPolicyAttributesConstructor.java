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

import java.util.HashMap;
import java.util.Map;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.model.properties.PolicyItem;
import org.onap.policy.api.AttributeType;

public class GuardPolicyAttributesConstructor {
    private static final EELFLogger logger = EELFManager.getInstance()
        .getLogger(GuardPolicyAttributesConstructor.class);

    private GuardPolicyAttributesConstructor() {
    }

    public static Map<AttributeType, Map<String, String>> formatAttributes(ClampProperties refProp,
        ModelProperties modelProperties, String modelElementId, PolicyItem policyItem) {
        Map<String, String> matchingAttributes = prepareMatchingAttributes(refProp, policyItem, modelProperties);
        return createAttributesMap(matchingAttributes);
    }

    private static Map<String, String> prepareMatchingAttributes(ClampProperties refProp,
        PolicyItem policyItem, ModelProperties modelProp) {
        logger.info("Preparing matching attributes for guard...");
        Map<String, String> matchingAttributes = new HashMap<>();
        matchingAttributes.put("actor",policyItem.getActor());
        matchingAttributes.put("recipe",policyItem.getRecipe());
        matchingAttributes.put("targets",policyItem.getGuardTargets());
        matchingAttributes.put("clname",modelProp.getControlNameAndPolicyUniqueId());
        if ("MinMax".equals(policyItem.getGuardPolicyType())) {
            matchingAttributes.put("min",policyItem.getMinGuard());
            matchingAttributes.put("max",policyItem.getMaxGuard());
        } else if ("FrequencyLimiter".equals(policyItem.getGuardPolicyType())) {
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
