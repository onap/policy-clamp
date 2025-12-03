/*-
 * ============LICENSE_START=========================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
 * ==================================================================================
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

package org.onap.policy.common.parameters;

/**
 * This interface acts as a base interface for all parameter groups in the ONAP Policy Framework. All parameter group
 * POJOs are implementations of the parameter group interface and can be used with the {@link ParameterService}.
 *
 * @author Liam Fallon (liam.fallon@ericsson.com)
 */
public interface ParameterGroup {
    /**
     * Get the group name.
     *
     * @return the group name
     */
    String getName();

    /**
     * Set the group name.
     *
     * @param name the group name
     */
    void setName(final String name);

    /**
     * Validate parameters.
     *
     * @return the result of the parameter validation
     */
    BeanValidationResult validate();

    /**
     * Check if the parameters are valid.
     *
     * @return true if the parameters are valid
     */
    default boolean isValid() {
        return validate().getStatus().isValid();
    }
}
