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

import java.util.LinkedHashMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class DocToscaPolicy extends DocToscaWithTypeAndStringProperties<ToscaPolicy> {

    private static final long serialVersionUID = 1L;

    // Tags for metadata
    private static final String METADATA_POLICY_ID_TAG = "policy-id";
    private static final String METADATA_POLICY_VERSION_TAG = "policy-version";

    /**
     * Copy constructor.
     *
     * @param copyObject object to copy
     */
    public DocToscaPolicy(@NonNull ToscaPolicy copyObject) {
        this.fromAuthorative(copyObject);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaPolicy(final DocToscaPolicy copyConcept) {
        super(copyConcept);
    }

    @Override
    public ToscaPolicy toAuthorative() {
        var toscaPolicy = new ToscaPolicy();
        super.setToscaEntity(toscaPolicy);
        super.toAuthorative();

        return toscaPolicy;
    }

    @Override
    public void fromAuthorative(ToscaPolicy toscaPolicy) {
        super.fromAuthorative(toscaPolicy);

        // Add the property metadata if it doesn't exist already
        if (toscaPolicy.getMetadata() == null) {
            setMetadata(new LinkedHashMap<>());
        }

        // Add the policy name and version fields to the metadata
        getMetadata().put(METADATA_POLICY_ID_TAG, getKey().getName());
        getMetadata().put(METADATA_POLICY_VERSION_TAG, getKey().getVersion());
    }

    @Override
    public BeanValidationResult validate(String fieldName) {
        var result = super.validate(fieldName);

        validateKeyVersionNotNull(result, "key", getConceptKey());

        return result;
    }
}
