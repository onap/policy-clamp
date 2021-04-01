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

package org.onap.policy.clamp.controlloop.participant.policy.main.parameters;

import java.io.File;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.clamp.controlloop.participant.policy.main.startstop.ParticipantPolicyCommandLineArguments;
import org.onap.policy.common.parameters.GroupValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * This class handles reading, parsing and validating of policy participant parameters from JSON files.
 */
public class ParticipantPolicyParameterHandler {

    private static final Coder CODER = new StandardCoder();

    /**
     * Read the parameters from the parameter file.
     *
     * @param arguments the arguments passed to policy
     * @return the parameters read from the configuration file
     * @throws ControlLoopException on parameter exceptions
     */
    public ParticipantPolicyParameters getParameters(final ParticipantPolicyCommandLineArguments arguments)
            throws ControlLoopException {
        ParticipantPolicyParameters parameters = null;

        // Read the parameters
        try {
            // Read the parameters from JSON
            File file = new File(arguments.getFullConfigurationFilePath());
            parameters = CODER.decode(file, ParticipantPolicyParameters.class);
        } catch (final CoderException e) {
            final String errorMessage = "error reading parameters from \"" + arguments.getConfigurationFilePath()
                    + "\"\n" + "(" + e.getClass().getSimpleName() + ")";
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, errorMessage, e);
        }

        // The JSON processing returns null if there is an empty file
        if (parameters == null) {
            final String errorMessage = "no parameters found in \"" + arguments.getConfigurationFilePath() + "\"";
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, errorMessage);
        }

        // validate the parameters
        final GroupValidationResult validationResult = parameters.validate();
        if (!validationResult.isValid()) {
            String returnMessage =
                    "validation error(s) on parameters from \"" + arguments.getConfigurationFilePath() + "\"\n";
            returnMessage += validationResult.getResult();

            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, returnMessage);
        }

        return parameters;
    }
}
