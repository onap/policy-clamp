/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.liquibase;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * This test enables Hibernate validation during context startup.
 * Hibernate validation checks that the database schema matches the JPA entity mappings.
 * It will detect the following issues:
 * - missing tables or columns
 * - incorrect column types
 * It will NOT detect issues related to constraints (e.g. missing NOT NULL constraint),
 * nor will it detect extra tables or columns in the database.
 */
@SpringBootTest
@ActiveProfiles("hibernate-validation")
class HibernateValidationTest extends AbstractLiquibaseTestBase {

    @Autowired
    private SupervisionScanner scanner;

    private static final AtomicInteger parallelCount = new AtomicInteger();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Test
    void createJobTest() {
        var list = List.of(1, 2, 3, 4, 5);
        var id = UUID.randomUUID();
        list.stream().parallel().forEach(
                x -> {
                    var optJob = scanner.createJob(id);
                    if (optJob.isPresent()) {
                        parallelCount.getAndIncrement();
                    }
                }
        );
        assertEquals(1, parallelCount.get());
    }
}
