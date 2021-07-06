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

package org.onap.policy.clamp.controlloop.runtime.main.parameters;

import java.io.File;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopException;
import org.onap.policy.common.parameters.ValidationResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;

/**
 * This class handles reading, parsing and validating of control loop runtime parameters from JSON files.
 */
public class ClRuntimeParameterHandler {

    private static final Coder CODER = new StandardCoder();

    /**
     * Read the parameters from the parameter file.
     *
     * @param path the path passed to control loop runtime
     * @return the parameters read from the configuration file
     * @throws ControlLoopException on parameter exceptions
     */
    public ClRuntimeParameterGroup getParameters(final String path) throws ControlLoopException {
        ClRuntimeParameterGroup clRuntimeParameterGroup = null;

        // Read the parameters
        try {
            // Read the parameters from JSON
            File file = new File(path);
            clRuntimeParameterGroup = CODER.decode(file, ClRuntimeParameterGroup.class);
        } catch (final CoderException e) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "error reading parameters from \"" + path + "\"\n" + "(" + e.getClass().getSimpleName() + ")", e);
        }

        // The JSON processing returns null if there is an empty file
        if (clRuntimeParameterGroup == null) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE, "no parameters found in \"" + path + "\"");
        }

        // validate the parameters
        final ValidationResult validationResult = clRuntimeParameterGroup.validate();
        if (!validationResult.isValid()) {
            throw new ControlLoopException(Response.Status.NOT_ACCEPTABLE,
                    "validation error(s) on parameters from \"" + path + "\"\n" + validationResult.getResult());
        }

        return clRuntimeParameterGroup;
    }
}
