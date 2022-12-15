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
import lombok.ToString;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DocToscaNodeTemplate extends DocToscaWithTypeAndStringProperties<ToscaNodeTemplate> {

    private static final long serialVersionUID = 1L;

    private List<Map<String, @Valid DocToscaRequirement>> requirements;

    private Map<String, @Valid DocToscaCapabilityAssignment> capabilities;

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public DocToscaNodeTemplate(final ToscaNodeTemplate authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaNodeTemplate(final DocToscaNodeTemplate copyConcept) {
        super(copyConcept);
        this.requirements =
                PfUtils.mapList(copyConcept.requirements, map -> PfUtils.mapMap(map, DocToscaRequirement::new));
        this.capabilities = PfUtils.mapMap(copyConcept.capabilities, DocToscaCapabilityAssignment::new);
    }

    @Override
    public ToscaNodeTemplate toAuthorative() {
        var toscaNodeTemplate = new ToscaNodeTemplate();
        super.setToscaEntity(toscaNodeTemplate);
        super.toAuthorative();

        toscaNodeTemplate.setRequirements(
                PfUtils.mapList(requirements, map -> DocUtil.docMapToMap(map, DocToscaRequirement::toAuthorative)));

        toscaNodeTemplate
                .setCapabilities(DocUtil.docMapToMap(capabilities, DocToscaCapabilityAssignment::toAuthorative));

        return toscaNodeTemplate;
    }

    @Override
    public void fromAuthorative(ToscaNodeTemplate toscaNodeTemplate) {
        super.fromAuthorative(toscaNodeTemplate);

        requirements = PfUtils.mapList(toscaNodeTemplate.getRequirements(),
                map -> DocUtil.mapToDocMap(map, DocToscaRequirement::new));

        capabilities = DocUtil.mapToDocMap(toscaNodeTemplate.getCapabilities(), DocToscaCapabilityAssignment::new);
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaNodeTemplate> otherConcept) {
        if (this == otherConcept) {
            return 0;
        }

        int result = super.compareTo(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaNodeTemplate) otherConcept;

        result = DocUtil.compareCollections(requirements, other.requirements);
        if (result != 0) {
            return result;
        }

        return DocUtil.compareMaps(capabilities, other.capabilities);
    }
}
