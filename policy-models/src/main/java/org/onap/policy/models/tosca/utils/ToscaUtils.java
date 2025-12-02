/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.models.tosca.utils;

import java.util.Collection;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKey;

/**
 * Utility class for TOSCA concepts.
 *
 * @author Liam Fallon (liam.fallon@est.tech)
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToscaUtils {
    private static final String ROOT_KEY_NAME_SUFFIX = ".Root";

    // @formatter:off
    private static final Set<PfConceptKey> PREDEFINED_TOSCA_DATA_TYPES = Set.of(
            new PfConceptKey("string",                       PfKey.NULL_KEY_VERSION),
            new PfConceptKey("integer",                      PfKey.NULL_KEY_VERSION),
            new PfConceptKey("float",                        PfKey.NULL_KEY_VERSION),
            new PfConceptKey("boolean",                      PfKey.NULL_KEY_VERSION),
            new PfConceptKey("timestamp",                    PfKey.NULL_KEY_VERSION),
            new PfConceptKey("null",                         PfKey.NULL_KEY_VERSION),
            new PfConceptKey("list",                         PfKey.NULL_KEY_VERSION),
            new PfConceptKey("map",                          PfKey.NULL_KEY_VERSION),
            new PfConceptKey("object",                       PfKey.NULL_KEY_VERSION),
            new PfConceptKey("scalar-unit.size",             PfKey.NULL_KEY_VERSION),
            new PfConceptKey("scalar-unit.time",             PfKey.NULL_KEY_VERSION),
            new PfConceptKey("scalar-unit.frequency",        PfKey.NULL_KEY_VERSION),
            new PfConceptKey("tosca.datatypes.TimeInterval", PfKey.NULL_KEY_VERSION)
        );
    // @formatter:on

    /**
     * Get the predefined policy types.
     *
     * @return the predefined policy types
     */
    public static Collection<PfConceptKey> getPredefinedDataTypes() {
        return PREDEFINED_TOSCA_DATA_TYPES;
    }
}
