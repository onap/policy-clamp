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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.models.acm.messages.dmaap.notification;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutomationCompositionNotification {

    /**
     * Status of automation compositions that are being added to participants.
     */
    @SerializedName("deployed-automation-compositions")
    private List<AutomationCompositionStatus> added = new ArrayList<>();

    /**
     * Status of policies that are being deleted from PDPs.
     */
    @SerializedName("undeployed-automation-compositions")
    private List<AutomationCompositionStatus> deleted = new ArrayList<>();


    /**
     * Determines if the notification is empty (i.e., has no added or delete automation composition
     * notifications).
     *
     * @return {@code true} if the notification is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return (CollectionUtils.isEmpty(added) && CollectionUtils.isEmpty(deleted));
    }
}
