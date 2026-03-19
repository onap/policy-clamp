/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Utility methods supporting tests for Instantiation.
 */
public class InstantiationUtils {

    /**
     * Gets the AutomationComposition from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the AutomationComposition from Resource
     */
    public static AutomationComposition getAutomationCompositionFromResource(final String path, final String suffix) {
        var automationComposition = CommonTestData.getObjectFromJsonFile(path, AutomationComposition.class);

        // add suffix to name
        automationComposition.setName(automationComposition.getName() + suffix);
        return automationComposition;
    }

    /**
     * Gets the AutomationComposition from Resource in yaml format.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the AutomationComposition from Resource
     */
    public static AutomationComposition getAutomationCompositionFromYaml(final String path, final String suffix) {
        var automationComposition = CommonTestData.getObjectFromYamlFile(path, AutomationComposition.class);

        // add suffix to name
        automationComposition.setName(automationComposition.getName() + suffix);
        return automationComposition;
    }

    /**
     * Assert that Instantiation Response contains AutomationComposition equals to automationComposition.
     *
     * @param response InstantiationResponse
     * @param automationComposition AutomationComposition
     */
    public static void assertInstantiationResponse(InstantiationResponse response,
            AutomationComposition automationComposition) {
        assertThat(response).isNotNull();
        assertThat(response.getErrorDetails()).isNull();
        assertEquals(response.getAffectedAutomationComposition(), PfUtils.getKey(automationComposition).asIdentifier());
    }

    /**
     * Get ToscaServiceTemplate from resource.
     *
     * @param path path of the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplate(String path) {
        return CommonTestData
                .getObjectFromYaml(ResourceUtils.getResourceAsString(path), ToscaServiceTemplate.class);
    }
}
