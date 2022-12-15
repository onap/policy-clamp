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

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DocToscaNodeType extends DocToscaWithToscaProperties<ToscaNodeType> {

    private static final long serialVersionUID = 1L;

    private List<Map<String, @Valid DocToscaRequirement>> requirements;

    public DocToscaNodeType(ToscaNodeType toscaNodeType) {
        fromAuthorative(toscaNodeType);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaNodeType(final DocToscaNodeType copyConcept) {
        super(copyConcept);
        this.requirements =
                PfUtils.mapList(copyConcept.requirements, map -> PfUtils.mapMap(map, DocToscaRequirement::new));
    }

    @Override
    public ToscaNodeType toAuthorative() {
        var toscaNodeType = new ToscaNodeType();
        super.setToscaEntity(toscaNodeType);
        super.toAuthorative();
        toscaNodeType.setRequirements(
                PfUtils.mapList(requirements, map -> DocUtil.docMapToMap(map, DocToscaRequirement::toAuthorative)));

        return toscaNodeType;
    }

    @Override
    public void fromAuthorative(ToscaNodeType toscaNodeType) {
        super.fromAuthorative(toscaNodeType);
        requirements = PfUtils.mapList(toscaNodeType.getRequirements(),
                map -> DocUtil.mapToDocMap(map, DocToscaRequirement::new));
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaNodeType> otherConcept) {
        if (this == otherConcept) {
            return 0;
        }

        int result = super.compareTo(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaNodeType) otherConcept;

        return PfUtils.compareCollections(requirements, other.requirements);
    }
}
