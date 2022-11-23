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
import lombok.Data;
import lombok.NoArgsConstructor;
import org.onap.policy.common.parameters.annotations.NotNull;
import org.onap.policy.models.base.PfAuthorative;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaParameter;

@Data
@NoArgsConstructor
public class DocToscaParameter implements PfAuthorative<ToscaParameter>, Serializable, Comparable<DocToscaParameter> {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String name;

    @NotNull
    private String type;

    @NotNull
    @SerializedName("type_version")
    private String typeVersion;

    private Object value;

    /**
     * Authorative constructor.
     *
     * @param authorativeConcept the authorative concept to copy from
     */
    public DocToscaParameter(final ToscaParameter authorativeConcept) {
        this.fromAuthorative(authorativeConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaParameter(final DocToscaParameter copyConcept) {
        this.name = copyConcept.name;
        this.type = copyConcept.type;
        this.typeVersion = copyConcept.typeVersion;
        this.value = copyConcept.value;
    }

    @Override
    public ToscaParameter toAuthorative() {
        var toscaParameter = new ToscaParameter();

        toscaParameter.setName(name);
        toscaParameter.setType(type);
        toscaParameter.setTypeVersion(typeVersion);
        toscaParameter.setValue(value);

        return toscaParameter;
    }

    @Override
    public void fromAuthorative(ToscaParameter toscaParameter) {
        name = toscaParameter.getName();
        type = toscaParameter.getType();

        if (toscaParameter.getTypeVersion() != null) {
            typeVersion = toscaParameter.getTypeVersion();
        } else {
            typeVersion = PfKey.NULL_KEY_VERSION;
        }

        value = toscaParameter.getValue();
    }

    @Override
    public int compareTo(DocToscaParameter otherConcept) {
        if (otherConcept == null) {
            return -1;
        }
        if (this == otherConcept) {
            return 0;
        }

        int result = name.compareTo(otherConcept.name);
        if (result != 0) {
            return result;
        }

        return PfUtils.compareObjects(value, otherConcept.value);
    }
}
