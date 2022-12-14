/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.repository;

import java.util.List;
import org.onap.policy.models.base.PfConcept;

public interface FilterRepository {

    /**
     * Get an object from the database, referred to by concept key.
     *
     * @param <T> the type of the object to get, a subclass of {@link PfConcept}
     * @param someClass the class of the object to get, a subclass of {@link PfConcept}, if name is null, all concepts
     *        of type T are returned, if name is not null and version is null, all versions of that concept matching the
     *        name are returned.
     * @param name the name of the object to get, null returns all objects
     * @param version the version the object to get, null returns all objects for a specified name
     * @return the objects that was retrieved from the database
     */
    <T extends PfConcept> List<T> getFiltered(Class<T> someClass, String name, String version);
}
