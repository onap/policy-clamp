/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021,2024 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.concepts;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AutomationCompositions {
    private List<AutomationComposition> automationCompositionList = new ArrayList<>();

    /**
     * Copy constructor, does a deep copy.
     *
     * @param other the other element to copy from
     */
    public AutomationCompositions(final AutomationCompositions other) {
        this.automationCompositionList = PfUtils.mapList(other.automationCompositionList, AutomationComposition::new);
    }
}
