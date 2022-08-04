/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.messages.dmaap.element;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElementMessage {

    private ToscaConceptIdentifier elementId;

    private String message;

    private UUID messageId = UUID.randomUUID();

    /**
     * Time-stamp, in milliseconds, when the message was created. Defaults to the
     * current time.
     */
    private Instant timestamp = Instant.now();
}
