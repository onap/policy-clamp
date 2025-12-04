/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class LiquibaseSessionLockTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("registry.nordix.org/onaptest/postgres:14.1").asCompatibleSubstituteFor("postgres"));

    @Test
    void testChangelogLockIsClearedOnFailure() throws Exception {
        Connection mainConnection = initConnection();
        Liquibase mainLiquibase = initLiquibase(mainConnection);

        // Monitor for Liquibase lock acquisition, then force connection failure.
        var connectionKiller = new Thread(() -> {
            try (Liquibase lockMonitor = initLiquibase(initConnection())) {
                await().atMost(1, TimeUnit.MINUTES)
                        .pollDelay(100, TimeUnit.MILLISECONDS)
                        .until(() -> getLockCount(lockMonitor) > 0);
                mainConnection.close();
            } catch (Exception e) {
                fail("Failed to monitor locks or close connection: " + e.getMessage(), e);
            }
        });
        connectionKiller.start();

        // Start a Liquibase update, but it will fail as the connection will be closed after the lock is established.
        assertThrows(LiquibaseException.class, mainLiquibase::update);

        connectionKiller.join(5000);

        // Main assertion: verify the lock is automatically released.
        try (Liquibase lockVerifier = initLiquibase(initConnection())) {
            assertEquals(0, getLockCount(lockVerifier));
        }
    }

    private static int getLockCount(Liquibase liquibase) throws LiquibaseException {
        return liquibase.listLocks().length;
    }

    private static Liquibase initLiquibase(Connection connection) throws DatabaseException {
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        return new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
    }

    private static Connection initConnection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
    }
}
