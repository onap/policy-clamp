/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.document.concepts;

import java.io.Serial;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaCapabilityAssignment;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DocToscaCapabilityAssignment extends DocToscaWithTypeAndStringProperties<ToscaCapabilityAssignment> {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<@NotNull String, @NotNull Object> attributes;
    private List<@NotNull Object> occurrences;

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public DocToscaCapabilityAssignment(final ToscaCapabilityAssignment authorativeConcept) {
        fromAuthorative(authorativeConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaCapabilityAssignment(final DocToscaCapabilityAssignment copyConcept) {
        super(copyConcept);
        this.attributes = copyConcept.attributes == null ? null : new LinkedHashMap<>(copyConcept.attributes);
        this.occurrences = copyConcept.occurrences == null ? null : new ArrayList<>(copyConcept.occurrences);
    }

    @Override
    public ToscaCapabilityAssignment toAuthorative() {
        var toscaCapabilityAssignment = new ToscaCapabilityAssignment();
        super.setToscaEntity(toscaCapabilityAssignment);
        super.toAuthorative();

        toscaCapabilityAssignment.setAttributes(PfUtils.mapMap(attributes, attribute -> attribute));
        toscaCapabilityAssignment.setOccurrences(PfUtils.mapList(occurrences, occurrence -> occurrence));

        return toscaCapabilityAssignment;
    }

    @Override
    public void fromAuthorative(ToscaCapabilityAssignment toscaCapabilityAssignment) {
        super.fromAuthorative(toscaCapabilityAssignment);

        attributes = PfUtils.mapMap(toscaCapabilityAssignment.getAttributes(), attribute -> attribute);
        occurrences = PfUtils.mapList(toscaCapabilityAssignment.getOccurrences(), occurrence -> occurrence);
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaCapabilityAssignment> otherConcept) {
        if (this == otherConcept) {
            return 0;
        }

        int result = super.compareTo(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaCapabilityAssignment) otherConcept;

        result = PfUtils.compareMaps(attributes, other.attributes);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareCollections(occurrences, other.occurrences);
    }
}
