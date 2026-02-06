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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import liquibase.Liquibase;
import liquibase.exception.LiquibaseException;
import org.junit.jupiter.api.Test;

class LiquibaseSessionLockTest extends AbstractLiquibaseTestBase {

    @Test
    void shouldCleanupStaleLockWhenConnectionFails() throws Exception {
        // Given: A Liquibase instance with an active connection
        var mainConnection = initConnection();
        var mainLiquibase = initLiquibase(mainConnection);

        // And: A background thread that will kill the connection once a lock is acquired
        var connectionKiller = createConnectionKiller(mainConnection);

        // When: We start the connection killer and attempt a Liquibase update
        connectionKiller.start();
        assertThrows(LiquibaseException.class, mainLiquibase::update);
        connectionKiller.join(5000);

        // Then: The database lock should be automatically cleared
        verifyLockIsCleared();
    }

    private static Thread createConnectionKiller(Connection connectionToKill) {
        return new Thread(() -> {
            try (var lockMonitor = initLiquibase(initConnection())) {
                await().atMost(5, TimeUnit.SECONDS)
                        .pollDelay(50, TimeUnit.MILLISECONDS)
                        .until(() -> getLockCount(lockMonitor) > 0);
                connectionToKill.close();
            } catch (Exception e) {
                fail("Failed to monitor locks or close connection: " + e.getMessage(), e);
            }
        });
    }

    private static void verifyLockIsCleared() throws SQLException, LiquibaseException {
        try (var lockVerifier = initLiquibase(initConnection())) {
            assertEquals(0, getLockCount(lockVerifier));
        }
    }

    private static int getLockCount(Liquibase liquibase) throws LiquibaseException {
        return liquibase.listLocks().length;
    }
}
