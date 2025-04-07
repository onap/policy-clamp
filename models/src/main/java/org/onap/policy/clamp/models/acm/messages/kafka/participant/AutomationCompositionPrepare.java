/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.concepts.ParticipantDeploy;
import org.onap.policy.models.base.PfUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class AutomationCompositionPrepare extends ParticipantMessage {

    private List<ParticipantDeploy> participantList = new ArrayList<>();
    private boolean preDeploy = true;
    private Integer stage = 0;

    /**
     * Constructor for instantiating class with message name.
     *
     */
    public AutomationCompositionPrepare() {
        super(ParticipantMessageType.AUTOMATION_COMPOSITION_PREPARE);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public AutomationCompositionPrepare(AutomationCompositionPrepare source) {
        super(source);
        this.preDeploy = source.preDeploy;
        this.stage = source.stage;
        this.participantList = PfUtils.mapList(source.participantList, ParticipantDeploy::new);
    }
}
