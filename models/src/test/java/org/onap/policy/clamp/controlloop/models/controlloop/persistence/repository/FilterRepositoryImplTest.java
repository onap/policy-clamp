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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ProviderUtils;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.dao.PfDao;
import org.onap.policy.models.dao.PfFilterParameters;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.ModelsProvider;

class FilterRepositoryImplTest {
    private static final String CONTROL_LOOP_JSON = "src/test/resources/providers/TestControlLoops.json";
    private static final Coder CODER = new StandardCoder();
    private static final AtomicInteger dbNameCounter = new AtomicInteger();
    private static final String originalJson = ResourceUtils.getResourceAsString(CONTROL_LOOP_JSON);
    private static List<JpaControlLoop> jpaControlLoops;
    private PfDao pfDao;

    @BeforeEach
    void beforeSetupDao() throws Exception {
        var parameters = new PolicyModelsProviderParameters();
        parameters.setDatabaseDriver("org.h2.Driver");
        parameters.setName("PolicyProviderParameterGroup");
        parameters.setImplementation("org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        parameters.setDatabaseUrl("jdbc:h2:mem:controlLoopProviderTestDb" + dbNameCounter.getAndDecrement());
        parameters.setDatabaseUser("policy");
        parameters.setDatabasePassword("P01icY");
        parameters.setPersistenceUnit("ToscaConceptTest");

        pfDao = ModelsProvider.init(parameters);
        var inputControlLoops = CODER.decode(originalJson, ControlLoops.class);
        jpaControlLoops = ProviderUtils.getJpaAndValidateList(inputControlLoops.getControlLoopList(),
                JpaControlLoop::new, "ControlLoops");

        pfDao.createCollection(jpaControlLoops);
    }

    @Test
    void testGetPfDao() {
        assertThat(new FilterRepositoryImpl().getPfDao()).isNotNull();
    }

    @Test
    void testGetFilteredParams() {
        var filterRepositoryImpl = new FilterRepositoryImpl() {
            @Override
            protected PfDao getPfDao() {
                return pfDao;
            }
        };
        var result = filterRepositoryImpl.getFiltered(JpaControlLoop.class, null, null);
        assertThat(result).hasSize(2);

        result = filterRepositoryImpl.getFiltered(JpaControlLoop.class, jpaControlLoops.get(0).getName(), null);
        assertThat(result).hasSize(1);
    }

    @Test
    void testGetFiltered() {
        var filterRepositoryImpl = new FilterRepositoryImpl() {
            @Override
            protected PfDao getPfDao() {
                return pfDao;
            }
        };

        // @formatter:off
        PfFilterParameters filterParams = PfFilterParameters
                .builder()
                .name(jpaControlLoops.get(0).getName())
                .build();
        // @formatter:on

        var result = filterRepositoryImpl.getFiltered(JpaControlLoop.class, filterParams);
        assertThat(result).hasSize(1);
    }
}
