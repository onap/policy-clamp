/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.policy.main.startstop;

import java.util.Arrays;
import javax.ws.rs.core.Response;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.common.ControlLoopConstants;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameterHandler;
import org.onap.policy.clamp.controlloop.participant.policy.main.parameters.ParticipantPolicyParameters;
import org.onap.policy.common.utils.resources.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class initiates Policy Participant.
 */
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private ParticipantPolicyActivator activator;

    @Getter
    private ParticipantPolicyParameters parameterGroup;

    /**
     * Instantiates policy participant.
     *
     * @param args the command line arguments
     */
    public Main(final String[] args) {
        final String argumentString = Arrays.toString(args);
        LOGGER.info("Starting the control loop participant service with arguments - {}", argumentString);

        // Check the arguments
        final ParticipantPolicyCommandLineArguments arguments = new ParticipantPolicyCommandLineArguments();
        try {
            // The arguments return a string if there is a message to print and we should exit
            final String argumentMessage = arguments.parse(args);
            if (argumentMessage != null) {
                LOGGER.info(argumentMessage);
                return;
            }
            // Validate that the arguments are sane
            arguments.validate();

            // Read the parameters
            parameterGroup = new ParticipantPolicyParameterHandler().getParameters(arguments);

            // Now, create the activator for the service
            activator = new ParticipantPolicyActivator(parameterGroup);

            // Start the activator
            activator.start();
        } catch (Exception exp) {
            throw new ControlLoopRuntimeException(Response.Status.BAD_REQUEST,
                String.format(MessageConstants.START_FAILURE_MSG, MessageConstants.POLICY_CLAMP), exp);
        }

        // Add a shutdown hook to shut everything down in an orderly manner
        Runtime.getRuntime().addShutdownHook(new ClRuntimeShutdownHookClass());
        String successMsg = String.format(MessageConstants.START_SUCCESS_MSG, MessageConstants.POLICY_CLAMP);
        LOGGER.info(successMsg);
    }

    /**
     * Check if main is running.
     */
    public boolean isRunning() {
        return activator != null && activator.isAlive();
    }

    /**
     * Shut down Execution.
     *
     * @throws ControlLoopException on shutdown errors
     */
    public void shutdown() throws ControlLoopException {
        // clear the parameterGroup variable
        parameterGroup = null;

        // clear the cl participant activator
        if (activator != null) {
            activator.stop();
        }
    }

    /**
     * The Class ClRuntimeShutdownHookClass terminates the policy participant
     * when its run method is called.
     */
    private class ClRuntimeShutdownHookClass extends Thread {
        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            try {
                // Shutdown the control loop participant service and wait for everything to stop
                shutdown();
            } catch (final RuntimeException | ControlLoopException e) {
                LOGGER.warn("error occured during shut down of the policy participant", e);
            }
        }
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(final String[] args) {      // NOSONAR
        /*
         * NOTE: arguments are validated by the constructor, thus sonar is disabled.
         */

        new Main(args);
    }
}
