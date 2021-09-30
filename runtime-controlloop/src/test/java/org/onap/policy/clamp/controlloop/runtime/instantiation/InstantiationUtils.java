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

package org.onap.policy.clamp.controlloop.runtime.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
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
     * Gets the ControlLoops from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in ControlLoops
     * @return the ControlLoops from Resource
     * @throws CoderException if an error occurs
     */
    public static ControlLoops getControlLoopsFromResource(final String path, final String suffix)
            throws CoderException {
        ControlLoops controlLoops = CODER.decode(new File(path), ControlLoops.class);

        // add suffix to all names
        controlLoops.getControlLoopList().forEach(controlLoop -> controlLoop.setName(controlLoop.getName() + suffix));
        return controlLoops;
    }

    /**
     * Gets InstantiationCommand from Resource.
     *
     * @param path path of the resource
     * @param suffix suffix to add to all names in ControlLoops
     * @return the InstantiationCommand
     * @throws CoderException if an error occurs
     */
    public static InstantiationCommand getInstantiationCommandFromResource(final String path, final String suffix)
            throws CoderException {
        InstantiationCommand instantiationCommand = CODER.decode(new File(path), InstantiationCommand.class);

        // add suffix to all names
        instantiationCommand.getControlLoopIdentifierList().forEach(cl -> cl.setName(cl.getName() + suffix));
        return instantiationCommand;
    }

    /**
     * Assert that Instantiation Response contains proper ControlLoops.
     *
     * @param response InstantiationResponse
     * @param controlLoops ControlLoops
     */
    public static void assertInstantiationResponse(InstantiationResponse response, ControlLoops controlLoops) {
        assertThat(response).isNotNull();
        assertThat(response.getErrorDetails()).isNull();
        assertThat(response.getAffectedControlLoops().size()).isEqualTo(controlLoops.getControlLoopList().size());
        for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
            assertTrue(response.getAffectedControlLoops().stream()
                    .filter(ac -> ac.equals(controlLoop.getKey().asIdentifier())).findAny().isPresent());
        }
    }

    /**
     * Assert that Instantiation Response contains proper ControlLoops.
     *
     * @param response InstantiationResponse
     * @param command InstantiationCommand
     */
    public static void assertInstantiationResponse(InstantiationResponse response, InstantiationCommand command) {
        assertThat(response).isNotNull();
        assertEquals(response.getAffectedControlLoops().size(), command.getControlLoopIdentifierList().size());
        for (ToscaConceptIdentifier toscaConceptIdentifier : command.getControlLoopIdentifierList()) {
            assertTrue(response.getAffectedControlLoops().stream()
                    .filter(ac -> ac.compareTo(toscaConceptIdentifier) == 0).findAny().isPresent());
        }
    }

    /**
     * Assert that Instantiation Response contains ControlLoop equals to controlLoop.
     *
     * @param response InstantiationResponse
     * @param controlLoop ControlLoop
     */
    public static void assertInstantiationResponse(InstantiationResponse response, ControlLoop controlLoop) {
        assertThat(response).isNotNull();
        assertThat(response.getErrorDetails()).isNull();
        assertEquals(1, response.getAffectedControlLoops().size());
        assertEquals(0, response.getAffectedControlLoops().get(0).compareTo(controlLoop.getKey().asIdentifier()));
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
