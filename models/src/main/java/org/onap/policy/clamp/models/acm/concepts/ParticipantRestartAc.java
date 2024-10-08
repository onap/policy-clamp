/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ParticipantRestartAc {

    private UUID automationCompositionId;

    private DeployState deployState;
    private LockState lockState;
    private StateChangeResult stateChangeResult;

    private List<AcElementRestart> acElementList = new ArrayList<>();

    /**
     * Copy constructor.
     *
     * @param copyConstructor the participant with updates to copy from
     */
    public ParticipantRestartAc(ParticipantRestartAc copyConstructor) {
        this.automationCompositionId = copyConstructor.automationCompositionId;
        this.deployState = copyConstructor.deployState;
        this.lockState = copyConstructor.lockState;
        this.stateChangeResult = copyConstructor.stateChangeResult;
        this.acElementList = PfUtils.mapList(copyConstructor.acElementList, AcElementRestart::new);
    }
}
