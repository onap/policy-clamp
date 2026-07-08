/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAutomationCompositionDefinition;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompositionSpecs {

    /**
     * Has has Field.
     *
     * @param value the value
     * @param field the field of JpaAutomationCompositionDefinition to check
     * @return the Specification for equal
     */
    public static Specification<JpaAutomationCompositionDefinition> hasField(String value, String field) {
        return (root, query, builder) -> {
            if (value == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get(field), value);
        };
    }

    /**
     * Has ParticipantId.
     *
     * @param participantId the participantId
     * @return the Specification for equal
     */
    public static Specification<JpaAutomationCompositionDefinition> hasParticipantId(UUID participantId) {
        return (root, query, builder) -> {
            if (participantId == null) {
                return builder.conjunction();
            }
            query.groupBy(root.get("compositionId"));
            var elementsJoin = root.join("elements");
            return builder.equal(elementsJoin.get("participantId"), participantId.toString());
        };
    }
}
