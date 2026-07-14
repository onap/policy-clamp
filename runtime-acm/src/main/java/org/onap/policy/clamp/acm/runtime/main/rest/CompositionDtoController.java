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

package org.onap.policy.clamp.acm.runtime.main.rest;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.policy.clamp.acm.runtime.commissioning.CompositionDtoService;
import org.onap.policy.clamp.acm.runtime.main.rest.gen.CompositionDtoApi;
import org.onap.policy.clamp.acm.runtime.main.web.AbstractRestController;
import org.onap.policy.clamp.models.acm.dto.CompositionDto;
import org.onap.policy.clamp.models.acm.dto.CompositionDtos;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Class to provide REST end points for querying CompositionDtos.
 */
@RestController
@RequiredArgsConstructor
@Profile("default")
public class CompositionDtoController extends AbstractRestController implements CompositionDtoApi {

    private final CompositionDtoService provider;

    @Override
    public ResponseEntity<CompositionDto> getCompositionDto(UUID participantId, UUID compositionId,
            UUID xonapRequestId) {
        return ResponseEntity.ok().body(provider.getCompositionDto(participantId, compositionId));
    }

    @Override
    public ResponseEntity<CompositionDtos> queryCompositionDto(UUID participantId, Integer page, Integer size,
            UUID xonapRequestId) {
        var pageable = getPageable(page, size);
        return ResponseEntity.ok().body(provider.getCompositionDtos(participantId, pageable));
    }
}
