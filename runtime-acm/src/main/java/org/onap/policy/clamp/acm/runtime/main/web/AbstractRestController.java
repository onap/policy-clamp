/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022,2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.runtime.main.web;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Common superclass to provide REST endpoints for the participant simulator.
 */
@RequestMapping(value = "/v2",
    produces = {MediaType.APPLICATION_JSON, AbstractRestController.APPLICATION_YAML})
public abstract class AbstractRestController {
    public static final String APPLICATION_YAML = "application/yaml";

    /**
     * Constructor.
     */
    protected AbstractRestController() {
    }

    protected URI createUri(String str) {
        try {
            return new URI(str);
        } catch (URISyntaxException e) {
            throw new AutomationCompositionRuntimeException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    protected Pageable getPageable(Integer page, Integer size) {
        return page != null && size != null ? PageRequest.of(page, size) : Pageable.unpaged();
    }

    /**
     * Retrieves a {@link Pageable} object with sorting capabilities based on the given page, size, sort field,
     * and sort direction.
     * If the sort field is not provided or is blank, it defaults to "lastMsg".
     * If the sort direction is not provided or is blank, it defaults to "DESC".
     * If either the page or size is null, an unpaged {@link Pageable} is returned.
     *
     * @param page the page number to be retrieved (zero-based indexing); if null, the result will be unpaged
     * @param size the size of the page to be retrieved; if null, the result will be unpaged
     * @param sort the field by which to sort; defaults to "lastMsg" if blank
     * @param sortOrder the direction of sorting, either "ASC" or "DESC"; defaults to "DESC" if blank
     * @return a {@link Pageable} object with the specified paging and sorting parameters,
     *      or unpaged if page or size is null
     */
    protected Pageable getPageableWithSorting(Integer page, Integer size, String sort, String sortOrder) {
        if (StringUtils.isBlank(sort)) {
            sort = "lastMsg";
        }

        if (StringUtils.isBlank(sortOrder)) {
            sortOrder = "DESC";
        }

        var sorting = Sort.by(Sort.Direction.fromString(sortOrder), sort);

        return page != null && size != null ? PageRequest.of(page, size).withSort(sorting) : Pageable.unpaged();
    }
}
