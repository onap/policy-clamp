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

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.report.DiffToReport;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

// This test class verifies that rollbacks for each Liquibase release tag works correctly.
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LiquibaseRollbackTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("registry.nordix.org/onaptest/postgres:14.1").asCompatibleSubstituteFor("postgres"));

    private Liquibase liquibase;

    @BeforeEach
    void setUp() throws Exception {
        liquibase = initLiquibase();
    }

    @AfterEach
    void tearDown() throws Exception {
        liquibase.dropAll();
        liquibase.close();
    }

    /**
     * This test will apply all changesets up to the latest change, and roll them all back.
     * This simple test detects many issues such as missing rollback instructions in changesets.
     */
    @Test
    void testUpdateAndRollback() {
        Assertions.assertDoesNotThrow(() -> liquibase.updateTestingRollback(null));
    }

    /**
     * This test will apply changesets up to a specific tag, roll back to a previous tag,
     * and then re-apply the changesets to ensure that the rollback was compatible with forward changes.
     */
    @ParameterizedTest
    @MethodSource("rollbackTagProvider")
    void testUpdateAndRollbackForTags(final String previousTag, final String targetTag) {
        // Run all changesets up to the target release tag
        Assertions.assertDoesNotThrow(() -> liquibase.update(targetTag, ""));
        // Roll back to the previous release
        Assertions.assertDoesNotThrow(() -> liquibase.rollback(previousTag, ""));
        // Apply forward changes again to ensure that the rollback was compatible with forwards changes
        Assertions.assertDoesNotThrow(() -> liquibase.update(targetTag, ""));
    }

    /**
     * This test compares the database schema before and after a rollback to ensure they are identical.
     * It works by creating two separate schemas in the database:
     * - one to apply changes up to a certain target tag (the original pre-upgraded schema)
     * - another to apply changes up to a later tag, and roll back to the target tag (the post-rollback schema)
     * The two schemas are then compared for equality. The schemas should be identical if the rollbacks are correct.
     */
    @ParameterizedTest
    @MethodSource("rollbackTagProvider")
    void testSchemaEqualityAfterRollback(final String rollbackToTag, final String rollbackFromTag) throws Exception {
        // Disable column order checking to avoid false positives when columns are dropped and re-added.
        System.setProperty("liquibase.diffColumnOrder", "false");
        // Create two schemas
        try (Connection conn = initConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA pre_upgrade_schema");
            stmt.execute("CREATE SCHEMA post_rollback_schema");
        }
        try (Liquibase liquibaseBefore = initLiquibaseForSchema("pre_upgrade_schema");
             Liquibase liquibaseAfter = initLiquibaseForSchema("post_rollback_schema")) {
            // Apply pre-upgrade schema to pre_upgrade_schema
            liquibaseBefore.update(rollbackToTag, "");

            // Apply upgrade and rollback to post_rollback_schema
            liquibaseAfter.update(rollbackFromTag, "");
            liquibaseAfter.rollback(rollbackToTag, "");

            // Compare the schemas and report any differences
            DiffResult diffResult = liquibaseBefore.diff(liquibaseBefore.getDatabase(), liquibaseAfter.getDatabase(),
                    CompareControl.STANDARD);
            if (!diffResult.areEqual()) {
                DiffToReport diffReport = new DiffToReport(diffResult, new PrintStream(System.out));
                diffReport.print();
            }
            // Fail test if schemas are different
            Assertions.assertTrue(diffResult.areEqual());

        } finally {
            try (Connection conn = initConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS pre_upgrade_schema CASCADE");
                stmt.execute("DROP SCHEMA IF EXISTS post_rollback_schema CASCADE");
            }
        }
    }

    private static Stream<Arguments> rollbackTagProvider() {
        return Stream.of(
                Arguments.of("1400", "1500"),
                Arguments.of("1500", "1600"),
                Arguments.of("1600", "1700"),
                Arguments.of("1700", "1701"),
                Arguments.of("1701", "1702"),
                Arguments.of("1702", "1800"),
                Arguments.of("1800", "1801")
        );
    }

    private Liquibase initLiquibase() throws Exception {
        return initLiquibaseForSchema(null);
    }

    private Liquibase initLiquibaseForSchema(String schema) throws Exception {
        var connection = initConnection();
        var database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
        database.setDefaultSchemaName(schema);
        return new Liquibase("db/changelog/db.changelog-master.yaml", new ClassLoaderResourceAccessor(), database);
    }

    private Connection initConnection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword());
    }

}
