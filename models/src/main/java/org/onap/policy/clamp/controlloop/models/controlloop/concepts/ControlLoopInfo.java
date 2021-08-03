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
import lombok.ToString;

/**
 * Class to represent a control loop info instance.
 */
@NoArgsConstructor
@Data
@ToString
public class ControlLoopInfo {

    private ControlLoopState state = ControlLoopState.UNINITIALISED;

    private ControlLoopStatistics controlLoopStatistics;

    /**
     * Copy constructor, does a deep copy but as all fields here are immutable, it's just a regular copy.
     *
     * @param otherElement the other element to copy from
     */
    public ControlLoopInfo(final ControlLoopInfo otherElement) {
        this.state = otherElement.state;
        this.controlLoopStatistics = otherElement.controlLoopStatistics;
    }
}
