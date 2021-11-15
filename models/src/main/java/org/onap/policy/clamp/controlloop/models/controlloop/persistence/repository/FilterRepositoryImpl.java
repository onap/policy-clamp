/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.onap.policy.models.base.PfConcept;
import org.onap.policy.models.dao.PfDao;
import org.onap.policy.models.dao.PfFilterParametersIntfc;
import org.onap.policy.models.dao.impl.ProxyDao;
import org.springframework.stereotype.Repository;

@Repository
public class FilterRepositoryImpl implements FilterRepository {

    @PersistenceContext
    private EntityManager entityManager;

    protected PfDao getPfDao() {
        return new ProxyDao(entityManager);
    }

    @Override
    public <T extends PfConcept> List<T> getFiltered(Class<T> someClass, PfFilterParametersIntfc filterParams) {
        return getPfDao().getFiltered(someClass, filterParams);
    }

    @Override
    public <T extends PfConcept> List<T> getFiltered(Class<T> someClass, String name, String version) {
        return getPfDao().getFiltered(someClass, name, version);
    }
}
