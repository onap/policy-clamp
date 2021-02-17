/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.main.parameters;

import lombok.Getter;
import org.onap.policy.common.parameters.ParameterGroupImpl;
import org.onap.policy.common.parameters.annotations.Min;
import org.onap.policy.common.parameters.annotations.NotBlank;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Parameters for Participant STATE-CHANGE requests.
 */
@NotNull
@NotBlank
@Getter
public class ParticipantStateChangeParameters extends ParameterGroupImpl {

    /**
     * Maximum number of times to re-send a request to a PDP.
     */
    @Min(value = 0)
    private int maxRetryCount;

    /**
     * Maximum time to wait, in milliseconds, for a PDP response.
     */
    @Min(value = 0)
    private long maxWaitMs;

    /**
     * Constructs the object.
     */
    public ParticipantStateChangeParameters() {
        super(ParticipantStateChangeParameters.class.getSimpleName());
    }
}
