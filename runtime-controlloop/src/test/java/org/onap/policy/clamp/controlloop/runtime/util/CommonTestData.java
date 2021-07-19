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

package org.onap.policy.clamp.controlloop.runtime.util;

import javax.ws.rs.core.Response.Status;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {
    private static final Coder coder = new StandardCoder();

    /**
     * Gets the standard Control Loop parameters.
     *
     * @param port port to be inserted into the parameters
     * @param dbName the database name
     * @return the standard Control Loop parameters
     * @throws ControlLoopRuntimeException on errors reading the control loop parameters
     */
    public static ClRuntimeParameterGroup geParameterGroup(final int port, final String dbName) {
        try {
            return coder.decode(getParameterGroupAsString(port, dbName), ClRuntimeParameterGroup.class);

        } catch (CoderException e) {
            throw new ControlLoopRuntimeException(Status.NOT_ACCEPTABLE, "cannot read Control Loop parameters", e);
        }
    }

    /**
     * Gets the standard Control Loop parameters, as a String.
     *
     * @param port port to be inserted into the parameters
     * @param dbName the database name
     * @return the standard Control Loop parameters as string
     */
    public static String getParameterGroupAsString(final int port, final String dbName) {
        return ResourceUtils.getResourceAsString("src/test/resources/parameters/InstantiationConfigParametersStd.json")
                .replace("${port}", String.valueOf(port)).replace("${dbName}", "jdbc:h2:mem:" + dbName);
    }

    /**
     * Create a new PolicyModelsProvider.
     *
     * @param databaseProviderParameters the database Provider Parameters
     * @return a new PolicyModelsProvider
     */
    public static PolicyModelsProvider getPolicyModelsProvider(
            PolicyModelsProviderParameters databaseProviderParameters) {
        try {
            return new PolicyModelsProviderFactory().createPolicyModelsProvider(databaseProviderParameters);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
    }
}
