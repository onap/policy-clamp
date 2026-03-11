/*-
 * ============LICENSE_START=======================================================
 * ONAP Policy Model
 * ================================================================================
 * Copyright (C) 2019-2023,2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.models.tosca.authorative.concepts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.base.PfNameVersion;

/**
 * Class to represent TOSCA data type matching input/output from/to client.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ToscaEntity implements PfNameVersion {
    private String name = PfKey.NULL_KEY_NAME;
    private String version = PfKey.NULL_KEY_VERSION;

    @JsonProperty("derived_from")
    private String derivedFrom;

    private Map<String, Object> metadata;
    private String description;

    /**
     * Copy Constructor.
     *
     * @param copyObject object to copy from
     */
    protected ToscaEntity(@NonNull ToscaEntity copyObject) {
        this.name = copyObject.name;
        this.version = copyObject.version;
        this.derivedFrom = copyObject.derivedFrom;
        this.description = copyObject.description;

        if (copyObject.metadata != null) {
            metadata = new LinkedHashMap<>();
            for (final Entry<String, Object> metadataEntry : copyObject.metadata.entrySet()) {
                metadata.put(metadataEntry.getKey(), metadataEntry.getValue());
            }
        }
    }

    /**
     * Method that should be specialised to return the type of the entity if the entity has a type.
     *
     * @return the type of the entity or null if it has no type
     */
    public String getType() {
        return null;
    }

    /**
     * Method that should be specialised to return the type version of the entity if the entity has a type.
     *
     * @return the type of the entity or null if it has no type
     */
    public String getTypeVersion() {
        return null;
    }
}
