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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Assert;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Utility methods supporting tests for Instantiation.
 */
public class InstantiationUtils {

    private static final Coder CODER = new StandardCoder();

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
     * Checks if two ControlLoops are equal.
     *
     * @param a ControlLoops
     * @param b ControlLoops
     * @return true if a equals b
     */
    public static boolean checkEquals(ControlLoops a, ControlLoops b) {
        return a.toString().equals(b.toString());
    }

    /**
     * Assert that Instantiation Response contains proper ControlLoops.
     *
     * @param response InstantiationResponse
     * @param controlLoops ControlLoops
     */
    public static void assertInstantiationResponse(InstantiationResponse response, ControlLoops controlLoops) {
        assertNotNull(response);
        Assert.assertNull(response.getErrorDetails());
        assertEquals(response.getAffectedControlLoops().size(), controlLoops.getControlLoopList().size());
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
        assertNotNull(response);
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
        assertNotNull(response);
        Assert.assertNull(response.getErrorDetails());
        assertEquals(1, response.getAffectedControlLoops().size());
        assertEquals(0, response.getAffectedControlLoops().get(0).compareTo(controlLoop.getKey().asIdentifier()));
    }
}
