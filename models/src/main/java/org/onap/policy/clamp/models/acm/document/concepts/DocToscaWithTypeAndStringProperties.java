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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.onap.policy.clamp.models.acm.document.base.DocConceptKey;
import org.onap.policy.clamp.models.acm.document.base.DocUtil;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaWithTypeAndObjectProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString
public class DocToscaWithTypeAndStringProperties<T extends ToscaWithTypeAndObjectProperties> extends DocToscaEntity<T> {

    private static final long serialVersionUID = 1L;

    private String type;

    @SerializedName("type_version")
    private String typeVersion;

    private Map<String, Object> properties;

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocToscaWithTypeAndStringProperties(final DocToscaWithTypeAndStringProperties<T> copyConcept) {
        super(copyConcept);
        this.type = copyConcept.type;
        this.typeVersion = copyConcept.typeVersion;
        this.properties = (copyConcept.properties != null ? new LinkedHashMap<>(copyConcept.properties) : null);
    }

    @Override
    public T toAuthorative() {
        var tosca = super.toAuthorative();

        tosca.setType(type);
        tosca.setTypeVersion(typeVersion);

        tosca.setProperties(PfUtils.mapMap(properties, x -> x));

        return tosca;
    }

    @Override
    public void fromAuthorative(T authorativeConcept) {
        super.fromAuthorative(authorativeConcept);
        if (authorativeConcept.getType() == null) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Type not specified, the type of this TOSCA entity must be specified in the type field");
        }
        var key = DocUtil.createDocConceptKey(authorativeConcept.getType(), authorativeConcept.getTypeVersion());
        type = key.getName();
        typeVersion = key.getVersion();

        if (PfKey.NULL_KEY_VERSION.equals(typeVersion)) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST,
                    "Version not specified, the version of this TOSCA entity must be specified"
                            + " in the type_version field");
        }

        properties = PfUtils.mapMap(authorativeConcept.getProperties(), x -> x);
    }

    public DocConceptKey getTypeDocConceptKey() {
        return new DocConceptKey(type, typeVersion);
    }
}
