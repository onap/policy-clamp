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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.onap.policy.models.tosca.authorative.concepts.ToscaRelationshipType;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class DocToscaRelationshipType extends DocToscaWithToscaProperties<ToscaRelationshipType> {

    private static final long serialVersionUID = 1L;

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public DocToscaRelationshipType(final ToscaRelationshipType authorativeConcept) {
        fromAuthorative(authorativeConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaRelationshipType(final DocToscaRelationshipType copyConcept) {
        super(copyConcept);
    }

    @Override
    public ToscaRelationshipType toAuthorative() {
        super.setToscaEntity(new ToscaRelationshipType());
        return super.toAuthorative();
    }
}
