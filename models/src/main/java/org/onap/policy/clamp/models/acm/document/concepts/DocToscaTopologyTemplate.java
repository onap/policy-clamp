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

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.base.Validated;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocToscaTopologyTemplate extends Validated
        implements PfAuthorative<ToscaTopologyTemplate>, Serializable, Comparable<DocToscaTopologyTemplate> {

    private static final long serialVersionUID = 1L;

    private String description;

    private Map<String, @Valid DocToscaParameter> inputs;

    @SerializedName("node_templates")
    private Map<String, @Valid DocToscaNodeTemplate> nodeTemplates;

    private Map<String, @Valid DocToscaPolicy> policies;

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public DocToscaTopologyTemplate(final ToscaTopologyTemplate authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaTopologyTemplate(final DocToscaTopologyTemplate copyConcept) {
        this.description = copyConcept.description;
        this.inputs = PfUtils.mapMap(copyConcept.inputs, DocToscaParameter::new);
        this.nodeTemplates =
                DocUtil.docMapToMap(copyConcept.nodeTemplates, DocToscaNodeTemplate::new, new LinkedHashMap<>());
        this.policies = DocUtil.docMapToMap(copyConcept.policies, DocToscaPolicy::new, new LinkedHashMap<>());
    }

    @Override
    public ToscaTopologyTemplate toAuthorative() {
        final var toscaTopologyTemplate = new ToscaTopologyTemplate();

        toscaTopologyTemplate.setDescription(description);
        toscaTopologyTemplate.setInputs(PfUtils.mapMap(inputs, DocToscaParameter::toAuthorative));
        toscaTopologyTemplate.setNodeTemplates(DocUtil.docMapToMap(nodeTemplates, DocToscaNodeTemplate::toAuthorative));

        toscaTopologyTemplate.setPolicies(DocUtil.docMapToList(policies, DocToscaPolicy::toAuthorative));

        return toscaTopologyTemplate;
    }

    @Override
    public void fromAuthorative(ToscaTopologyTemplate toscaTopologyTemplate) {
        description = toscaTopologyTemplate.getDescription();

        if (toscaTopologyTemplate.getInputs() != null) {
            inputs = PfUtils.mapMap(toscaTopologyTemplate.getInputs(), DocToscaParameter::new);
            for (var entry : inputs.entrySet()) {
                if (entry.getValue().getName() == null) {
                    entry.getValue().setName(entry.getKey());
                }
            }
        }

        nodeTemplates = DocUtil.mapToDocMap(toscaTopologyTemplate.getNodeTemplates(), DocToscaNodeTemplate::new);

        policies = DocUtil.listToDocMap(toscaTopologyTemplate.getPolicies(), DocToscaPolicy::new);
    }

    @Override
    public int compareTo(DocToscaTopologyTemplate otherConcept) {
        int result = compareToWithoutEntities(otherConcept);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareMaps(inputs, otherConcept.inputs);
        if (result != 0) {
            return result;
        }

        result = PfUtils.compareMaps(nodeTemplates, otherConcept.nodeTemplates);
        if (result != 0) {
            return result;
        }
        return PfUtils.compareMaps(policies, otherConcept.policies);
    }

    /**
     * Compare this topology template to another topology template, ignoring contained entities.
     *
     * @param otherConcept the other topology template
     * @return the result of the comparison
     */
    public int compareToWithoutEntities(final DocToscaTopologyTemplate otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }

        return ObjectUtils.compare(description, otherConcept.description);
    }
}
