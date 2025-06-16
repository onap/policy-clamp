/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.messages.kafka.participant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;

@Getter
@Setter
@ToString(callSuper = true)
public class AutomationCompositionMigration extends ParticipantMessage {

    private UUID compositionTargetId;
    // A list of updates to AC element properties
    private List<ParticipantDeploy> participantUpdatesList = new ArrayList<>();

    private Boolean precheck = false;
    private Boolean rollback = false;
    private Integer stage = 0;

    public AutomationCompositionMigration() {
        super(ParticipantMessageType.AUTOMATION_COMPOSITION_MIGRATION);
    }
}
