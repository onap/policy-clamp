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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.common.parameters.annotations.Valid;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DocToscaDataType extends DocToscaWithToscaProperties<ToscaDataType> {

    private static final long serialVersionUID = 1L;

    private List<@NotNull @Valid DocToscaConstraint> constraints;

    public DocToscaDataType(final ToscaDataType toscaDataType) {
        fromAuthorative(toscaDataType);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaDataType(final DocToscaDataType copyConcept) {
        super(copyConcept);
        this.constraints = PfUtils.mapList(copyConcept.constraints, DocToscaConstraint::new);
    }

    @Override
    public ToscaDataType toAuthorative() {
        var toscaDataType = new ToscaDataType();
        super.setToscaEntity(toscaDataType);
        super.toAuthorative();
        toscaDataType.setConstraints(PfUtils.mapList(constraints, DocToscaConstraint::toAuthorative));

        return toscaDataType;
    }

    @Override
    public void fromAuthorative(final ToscaDataType toscaDataType) {
        super.fromAuthorative(toscaDataType);

        constraints = PfUtils.mapList(toscaDataType.getConstraints(), DocToscaConstraint::new);
    }

    @Override
    public int compareTo(DocToscaEntity<ToscaDataType> otherConcept) {
        if (this == otherConcept) {
            return 0;
        }

        int result = super.compareTo(otherConcept);
        if (result != 0) {
            return result;
        }

        final var other = (DocToscaDataType) otherConcept;

        return PfUtils.compareCollections(constraints, other.constraints);
    }
}
