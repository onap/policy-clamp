/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021,2023,2025 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.acm.runtime.main.parameters;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

/**
 * Parameters for communicating with participants.
 */
@Getter
@Setter
@Validated
public class ParticipantParameters {

    @Min(100)
    private long heartBeatMs = 20000;

    @Min(100)
    private long maxStatusWaitMs = 150000;

    @Min(100)
    private long maxOperationWaitMs = 200000;
}
