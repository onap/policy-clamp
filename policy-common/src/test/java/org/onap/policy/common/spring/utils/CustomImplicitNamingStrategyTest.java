/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.spring.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class CustomImplicitNamingStrategyTest {

    static CustomImplicitNamingStrategy strategy;

    @Mock
    static ImplicitJoinColumnNameSource source;

    @BeforeAll
    public static void setUpBeforeClass() {
        strategy = new CustomImplicitNamingStrategy();
        source = mock(ImplicitJoinColumnNameSource.class);
    }

    @Test
    void testDetermineJoinColumnName() {
        Identifier identifier = new Identifier("identifier", true);

        MetadataBuildingContext buildingContextMock = mock(MetadataBuildingContext.class);
        InFlightMetadataCollector flightCollectorMock = mock(InFlightMetadataCollector.class);
        Database databaseMock = mock(Database.class);

        when(flightCollectorMock.getDatabase()).thenReturn(databaseMock);
        when(source.getReferencedColumnName()).thenReturn(identifier);
        when(source.getBuildingContext()).thenReturn(buildingContextMock);
        when(buildingContextMock.getMetadataCollector()).thenReturn(flightCollectorMock);

        JdbcEnvironment environmentMock = mock(JdbcEnvironment.class);
        when(databaseMock.getJdbcEnvironment()).thenReturn(environmentMock);

        IdentifierHelper helperMock = mock(IdentifierHelper.class);
        when(environmentMock.getIdentifierHelper()).thenReturn(helperMock);
        when(helperMock.toIdentifier(anyString())).thenReturn(identifier);

        Identifier result = strategy.determineJoinColumnName(source);
        assertEquals(identifier, result);
    }

}
