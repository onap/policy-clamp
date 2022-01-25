/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.StandardYamlCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Utility methods supporting tests for Instantiation.
 */
public class InstantiationUtils {

    private static final Coder CODER = new StandardCoder();
    private static final StandardYamlCoder YAML_TRANSLATOR = new StandardYamlCoder();

    /**
     * Gets the AutomationCompositions from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the AutomationCompositions from Resource
     * @throws CoderException if an error occurs
     */
    public static AutomationCompositions getAutomationCompositionsFromResource(final String path, final String suffix)
        throws CoderException {
        AutomationCompositions automationCompositions = CODER.decode(new File(path), AutomationCompositions.class);

        // add suffix to all names
        automationCompositions.getAutomationCompositionList()
            .forEach(automationComposition -> automationComposition.setName(automationComposition.getName() + suffix));
        return automationCompositions;
    }

    /**
     * Gets InstantiationCommand from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in AutomationCompositions
     * @return the InstantiationCommand
     * @throws CoderException if an error occurs
     */
    public static InstantiationCommand getInstantiationCommandFromResource(final String path, final String suffix)
        throws CoderException {
        InstantiationCommand instantiationCommand = CODER.decode(new File(path), InstantiationCommand.class);

        // add suffix to all names
        instantiationCommand.getAutomationCompositionIdentifierList().forEach(ac -> ac.setName(ac.getName() + suffix));
        return instantiationCommand;
    }

    /**
     * Assert that Instantiation Response contains proper AutomationCompositions.
     *
     * @param response InstantiationResponse
     * @param automationCompositions AutomationCompositions
     */
    public static void assertInstantiationResponse(InstantiationResponse response,
        AutomationCompositions automationCompositions) {
        assertThat(response).isNotNull();
        assertThat(response.getErrorDetails()).isNull();
        assertThat(response.getAffectedAutomationCompositions().size())
            .isEqualTo(automationCompositions.getAutomationCompositionList().size());
        for (AutomationComposition automationComposition : automationCompositions.getAutomationCompositionList()) {
            assertTrue(response.getAffectedAutomationCompositions().stream()
                .filter(ac -> ac.equals(automationComposition.getKey().asIdentifier())).findAny().isPresent());
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
        assertEquals(response.getAffectedAutomationCompositions().size(),
            command.getAutomationCompositionIdentifierList().size());
        for (ToscaConceptIdentifier toscaConceptIdentifier : command.getAutomationCompositionIdentifierList()) {
            assertTrue(response.getAffectedAutomationCompositions().stream()
                .filter(ac -> ac.compareTo(toscaConceptIdentifier) == 0).findAny().isPresent());
        }
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
        assertEquals(1, response.getAffectedAutomationCompositions().size());
        assertEquals(0, response.getAffectedAutomationCompositions().get(0)
            .compareTo(automationComposition.getKey().asIdentifier()));
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
