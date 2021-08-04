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

import java.time.Instant;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@NoArgsConstructor
@Data
@ToString
public class ControlLoopStatistics {

    @NonNull
    private ToscaConceptIdentifier controlLoopId;

    @NonNull
    private Instant timeStamp;

    ClElementStatisticsList clElementStatisticsList;

    private long eventCount;
    private long lastExecutionTime;
    private double averageExecutionTime;
    private long upTime;
    private long lastEnterTime;
    private long lastStart;
}
