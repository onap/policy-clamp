/*-
 * ============LICENSE_START=======================================================
 * Modifications Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.notification;

import com.google.gson.annotations.SerializedName;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

@Data
@NoArgsConstructor
public class ControlLoopStatus {
    @SerializedName("control-loop-id")
    private UUID id;

    private ToscaConceptIdentifier definition;

    /**
     * Constructs the object.
     *
     * @param id the ID of the control loop
     * @param definition the TOSCA definition of the control loop
     */
    public ControlLoopStatus(final UUID id, final ToscaConceptIdentifier definition) {
        this.id = id;
        this.definition = definition;
    }

    public void setFailureCount(int size) {
        // TODO Auto-generated method stub

    }

    public void setIncompleteCount(int size) {
        // TODO Auto-generated method stub

    }

    public void setSuccessCount(int size) {
        // TODO Auto-generated method stub

    }
}
