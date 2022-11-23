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

package org.onap.policy.clamp.models.acm.document.base;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.onap.policy.common.parameters.annotations.Pattern;
import org.onap.policy.common.utils.validation.Assertions;
import org.onap.policy.models.base.PfConceptKey;
import org.onap.policy.models.base.PfKeyImpl;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DocConceptKey extends PfKeyImpl {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = NAME_REGEXP)
    private String name;

    @Pattern(regexp = VERSION_REGEXP)
    private String version;

    /**
     * Constructor.
     */
    public DocConceptKey() {
        this(NULL_KEY_NAME, NULL_KEY_VERSION);
    }

    @Override
    public String getId() {
        return name + ":" + version;
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocConceptKey(final PfConceptKey copyConcept) {
        super(copyConcept);
    }

    /**
     * Copy constructor.
     *
     * @param copyConcept the concept to copy from
     */
    public DocConceptKey(final DocConceptKey copyConcept) {
        this(copyConcept.getName(), copyConcept.getVersion());
    }

    /**
     * Temporary Constructor to create a key with the specified name and version.
     *
     * @param name the key name
     * @param version the key version
     */
    public DocConceptKey(final String name, final String version) {
        super(name, version);
    }

    @Override
    public void setName(@NonNull final String name) {
        this.name = Assertions.validateStringParameter(NAME_TOKEN, name, getNameRegEx());
    }

    @Override
    public void setVersion(@NonNull final String version) {
        this.version = Assertions.validateStringParameter(VERSION_TOKEN, version, getVersionRegEx());
    }
}
