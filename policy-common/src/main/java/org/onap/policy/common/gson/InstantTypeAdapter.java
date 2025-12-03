/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson;

import java.time.Instant;

/**
 * GSON Type Adapter for "Instant" fields, that uses the standard ISO_INSTANT formatter.
 */
public class InstantTypeAdapter extends StringTypeAdapter<Instant> {

    /**
     * Constructs an adapter.
     */
    public InstantTypeAdapter() {
        super("date", Instant::parse, Instant::toString);
    }
}
