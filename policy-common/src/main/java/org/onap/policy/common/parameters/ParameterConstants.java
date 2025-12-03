/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Ericsson. All rights reserved.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.parameters;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This static class holds the values of constants for parameter handling.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParameterConstants {
    // Indentation is 0 on the left and 2 for each level of hierarchy
    public static final String DEFAULT_INITIAL_RESULT_INDENTATION = "";
    public static final String DEFAULT_RESULT_INDENTATION = "  ";

    // By default we do not show validation results for parameters that are validated as clean
    public static final boolean DO_NOT_SHOW_CLEAN_RESULTS = false;

    // Messages for clean validations
    public static final String PARAMETER_GROUP_HAS_STATUS_MESSAGE = "parameter group has status ";
    public static final String PARAMETER_GROUP_MAP_HAS_STATUS_MESSAGE = "parameter group map has status ";
    public static final String PARAMETER_HAS_STATUS_MESSAGE       = "parameter has status ";
}
