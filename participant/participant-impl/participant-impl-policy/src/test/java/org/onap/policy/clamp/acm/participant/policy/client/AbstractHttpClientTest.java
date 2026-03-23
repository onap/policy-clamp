/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.policy.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.RestClientParameters;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.springframework.web.reactive.function.client.WebClientException;

class AbstractHttpClientTest {

    @Test
    void testEncode() {
        var params = new RestClientParameters();
        var client = new AbstractHttpClient(params, MapperFactory.createJsonMapper()) {};
        var object = new WillNotSerialize();
        assertThrows(WebClientException.class, () -> client.encode(object));
    }

    private static class WillNotSerialize {
        public WillNotSerialize getSelf() {
            return this;
        }
    }
}
