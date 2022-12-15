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

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRequirement;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DocToscaRequirement extends DocToscaWithTypeAndStringProperties<ToscaRequirement> {

    private static final long serialVersionUID = 1L;

    private String capability;
    private String node;
    private String relationship;
    private List<Object> occurrences;

    public DocToscaRequirement(ToscaRequirement toscaRequirement) {
        fromAuthorative(toscaRequirement);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaRequirement(final DocToscaRequirement copyConcept) {
        super(copyConcept);
        this.capability = copyConcept.capability;
        this.node = copyConcept.node;
        this.relationship = copyConcept.relationship;
        this.occurrences = new ArrayList<>(copyConcept.occurrences);
    }

    @Override
    public ToscaRequirement toAuthorative() {
        var toscaRequirement = new ToscaRequirement();
        super.setToscaEntity(toscaRequirement);
        super.toAuthorative();

        toscaRequirement.setCapability(capability);
        toscaRequirement.setNode(node);
        toscaRequirement.setRelationship(relationship);

        if (occurrences != null) {
            toscaRequirement.setOccurrences(new ArrayList<>(occurrences));
        }

        return toscaRequirement;
    }

    @Override
    public void fromAuthorative(ToscaRequirement toscaRequirement) {
        super.fromAuthorative(toscaRequirement);

        capability = toscaRequirement.getCapability();
        node = toscaRequirement.getNode();
        relationship = toscaRequirement.getRelationship();

        if (toscaRequirement.getOccurrences() != null) {
            occurrences = new ArrayList<>(toscaRequirement.getOccurrences());
        }
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaRequirement> otherConcept) {
        if (this == otherConcept) {
            return 0;
        }

        int result = super.compareTo(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaRequirement) otherConcept;

        result = PfUtils.compareObjects(capability, other.capability);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareObjects(node, other.node);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareObjects(relationship, other.relationship);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareCollections(occurrences, other.occurrences);
    }
}
