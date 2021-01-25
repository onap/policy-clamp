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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Class to represent participant response details.
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ParticipantResponseDetails {

    // The responseTo field should match the original request id in the request.
    private UUID responseTo;
    private ParticipantResponseStatus responseStatus;
    private String responseMessage;

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantResponseDetails(ParticipantResponseDetails source) {
        this.responseMessage = source.responseMessage;
        this.responseStatus = source.responseStatus;
        this.responseTo = source.responseTo;
    }
}
