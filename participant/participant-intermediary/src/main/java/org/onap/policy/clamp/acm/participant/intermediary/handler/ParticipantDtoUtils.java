/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.intermediary.handler;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDto;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.clamp.models.acm.dto.InstanceElementDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ParticipantDtoUtils {

    /**
     * Resolve the effective instance element from a DTO.
     * For non-update operations, instanceElement may be null and instanceElementTarget holds the current state.
     *
     * @param dto the AcElementDto
     * @return the effective InstanceElementDto, or null if both are null
     */
    public static InstanceElementDto resolveInstanceElement(AcElementDto dto) {
        return dto.getInstanceElement() != null
                ? dto.getInstanceElement() : dto.getInstanceElementTarget();
    }

    /**
     * Extract a map of element ID to AcElementDto for the given participant from the message's participantDtoList.
     *
     * @param participantDtoList the list of ParticipantDto from the Kafka message
     * @param participantId the current participant's ID
     * @return map of element ID to AcElementDto, or empty map if no pre-built DTOs available
     */
    public static Map<UUID, AcElementDto> getElementDtoMap(List<ParticipantDto> participantDtoList,
            UUID participantId) {
        if (participantDtoList == null || participantDtoList.isEmpty()) {
            return Map.of();
        }
        return participantDtoList.stream()
                .filter(p -> participantId.equals(p.getParticipantId()))
                .flatMap(p -> p.getElementDtos().stream())
                .filter(dto -> resolveInstanceElement(dto) != null)
                .collect(Collectors.toMap(
                        dto -> resolveInstanceElement(dto).elementId(),
                        dto -> dto,
                        (a, b) -> b));
    }
}
