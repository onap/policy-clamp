/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.annotations.NotNull;

@NoArgsConstructor
@Data
@EqualsAndHashCode
public class ParticipantSupportedElementType {

    @NotNull
    private UUID id = UUID.randomUUID();

    @NotNull
    private String typeName;

    @NotNull
    private String typeVersion;

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param otherParticipantSupportedElementType the other element to copy from
     */
    public ParticipantSupportedElementType(final ParticipantSupportedElementType otherParticipantSupportedElementType) {
        this.id = otherParticipantSupportedElementType.getId();
        this.typeName = otherParticipantSupportedElementType.getTypeName();
        this.typeVersion = otherParticipantSupportedElementType.getTypeVersion();
    }
}
