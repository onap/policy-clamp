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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.dto.AcElementDto;
import org.onap.policy.models.base.PfUtils;

@NoArgsConstructor
@Data
@ToString
public class ParticipantDto {

    private UUID participantId;
    private List<AcElementDto> elementDtos = new ArrayList<>();

    public ParticipantDto(final ParticipantDto participantDto) {
        this.participantId = participantDto.participantId;
        this.elementDtos = PfUtils.mapList(participantDto.elementDtos, AcElementDto::new);
    }
}
