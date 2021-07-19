/*-
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

import java.util.concurrent.TimeUnit;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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

    /**
     * Default maximum message age, in milliseconds, that should be examined. Any message
     * older than this is discarded.
     */
    public static final long DEFAULT_MAX_AGE_MS = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    @Min(1)
    private long heartBeatMs;

    @Min(1)
    private long maxMessageAgeMs =  DEFAULT_MAX_AGE_MS;

    @Valid
    @NotNull
    private ParticipantUpdateParameters updateParameters;

    @Valid
    @NotNull
    private ParticipantStateChangeParameters stateChangeParameters;

}
