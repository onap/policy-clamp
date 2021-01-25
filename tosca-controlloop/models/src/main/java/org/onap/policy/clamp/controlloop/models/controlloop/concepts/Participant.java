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

package org.onap.policy.clamp.controlloop.models.controlloop.concepts;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.models.base.PfNameVersion;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent details of a running participant instance.
 */
@NoArgsConstructor
@Data
@ToString
public class Participant implements PfNameVersion, Comparable<Participant> {
    @NonNull
    private String name;

    @NonNull
    private String version;

    @NonNull
    private ToscaConceptIdentifier definition;

    @NonNull
    private ParticipantState participantState;

    @NonNull
    private ParticipantHealthStatus healthStatus;

    private String description;

    public String getDefinitionName() {
        return definition.getName();
    }

    public String getDefinitionVersion() {
        return definition.getVersion();
    }

    @Override
    public int compareTo(final Participant other) {
        return compareNameVersion(this, other);
    }
}
