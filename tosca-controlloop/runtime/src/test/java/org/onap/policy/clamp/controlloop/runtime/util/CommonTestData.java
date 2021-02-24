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

import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {
    private static final Coder coder = new StandardCoder();

    private static int dbNum = 0;

    public static void newDb() {
        ++dbNum;
    }

    /**
     * Gets the standard Control Loop parameters.
     *
     * @param port port to be inserted into the parameters
     * @return the standard Control Loop parameters
     */
    public static ClRuntimeParameterGroup geParameterGroup(int port) {
        try {
            return coder.decode(getParameterGroupAsString(port), ClRuntimeParameterGroup.class);

        } catch (CoderException e) {
            throw new RuntimeException("cannot read Control Loop parameters", e);
        }
    }

    /**
     * Gets the standard Control Loop parameters, as a String.
     *
     * @param port port to be inserted into the parameters
     * @return the standard Control Loop parameters
     */
    public static String getParameterGroupAsString(int port) {
        return ResourceUtils.getResourceAsString("src/test/resources/parameters/ConfigParametersStd.json")
                .replace("${port}", String.valueOf(port)).replace("${dbName}", "jdbc:h2:mem:testdb" + dbNum);
    }
}
