/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
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
}
