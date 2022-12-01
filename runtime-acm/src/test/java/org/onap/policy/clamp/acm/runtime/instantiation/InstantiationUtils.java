/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Utility methods supporting tests for Instantiation.
 */
public class InstantiationUtils {

    private static final Coder CODER = new StandardCoder();
    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    /**
     * Gets the AutomationComposition from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the AutomationComposition from Resource
     */
    public static AutomationComposition getAutomationCompositionFromResource(final String path, final String suffix) {
        try {
            var automationComposition = CODER.decode(new File(path), AutomationComposition.class);

            // add suffix to name
            automationComposition.setName(automationComposition.getName() + suffix);
            return automationComposition;
        } catch (CoderException e) {
            fail("Cannot read or decode " + path);
            return null;
        }
    }

    /**
     * Gets InstantiationCommand from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the InstantiationCommand
     */
    public static InstantiationCommand getInstantiationCommandFromResource(final String path, final String suffix) {
        try {
            var instantiationCommand = CODER.decode(new File(path), InstantiationCommand.class);

            // add suffix to the name
            var id = instantiationCommand.getAutomationCompositionIdentifier();
            id.setName(id.getName() + suffix);
            return instantiationCommand;
        } catch (CoderException e) {
            fail("Cannot read or decode " + path);
            return null;
        }
    }

    /**
     * Assert that Instantiation Response contains proper AutomationCompositions.
     *
     * @param response InstantiationResponse
     * @param command InstantiationCommand
     */
    public static void assertInstantiationResponse(InstantiationResponse response, InstantiationCommand command) {
        assertThat(response).isNotNull();
        assertEquals(response.getAffectedAutomationComposition(), command.getAutomationCompositionIdentifier());
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
        assertEquals(response.getAffectedAutomationComposition(), automationComposition.getKey().asIdentifier());
    }

    /**
     * Get ToscaServiceTemplate from resource.
     *
     * @param path path of the resource
     */
    public static ToscaServiceTemplate getToscaServiceTemplate(String path) {
        try {
            return YAML_TRANSLATOR.decode(ResourceUtils.getResourceAsStream(path), ToscaServiceTemplate.class);
        } catch (CoderException e) {
            fail("Cannot read or decode " + path);
            return null;
        }
    }
}
