/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceSpecs.hasParticipantId;
import static org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceSpecs.in;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcInstanceSpecsTest {

    @Test
    void testIn() {
        var spec = in(List.of(), "instanceId");
        var builder = mock(CriteriaBuilder.class);
        var predicate = mock(Predicate.class);
        when(builder.conjunction()).thenReturn(predicate);
        var result = spec.toPredicate(mock(Root.class), mock(CriteriaQuery.class), builder);
        assertEquals(predicate, result);

        spec = in(List.of("value"), "instanceId");
        var root = mock(Root.class);
        var path = mock(Path.class);
        when(root.get("instanceId")).thenReturn(path);
        var inClause = mock(CriteriaBuilder.In.class);
        when(builder.in(any())).thenReturn(inClause);
        result = spec.toPredicate(mock(Root.class), mock(CriteriaQuery.class), builder);
        assertEquals(inClause, result);
    }

    @Test
    void testHasParticipantId() {
        var spec = hasParticipantId(null);
        var builder = mock(CriteriaBuilder.class);
        var predicate = mock(Predicate.class);
        when(builder.conjunction()).thenReturn(predicate);
        var result = spec.toPredicate(mock(Root.class), mock(CriteriaQuery.class), builder);
        assertEquals(predicate, result);

        var root = mock(Root.class);
        var elementsJoin = mock(Join.class);
        when(root.join("elements")).thenReturn(elementsJoin);
        var path = mock(Path.class);
        when(elementsJoin.get("participantId")).thenReturn(path);
        var participantId = UUID.randomUUID();
        when(builder.equal(path, participantId.toString())).thenReturn(predicate);
        spec = hasParticipantId(participantId);
        result = spec.toPredicate(root, mock(CriteriaQuery.class), builder);
        assertEquals(predicate, result);
    }
}
