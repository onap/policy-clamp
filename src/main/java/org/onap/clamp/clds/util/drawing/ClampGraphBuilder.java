/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * Modifications copyright (c) 2019 AT&T
 * ===================================================================
 *
 */

package org.onap.clamp.clds.util.drawing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.onap.clamp.clds.sdc.controller.installer.BlueprintMicroService;

public class ClampGraphBuilder {
    private String policy;
    private String collector;
    private List<BlueprintMicroService> microServices = new ArrayList<>();
    private final Painter painter;

    public ClampGraphBuilder(Painter painter) {
        this.painter = painter;
    }

    public ClampGraphBuilder collector(String collector) {
        this.collector = collector;
        return this;
    }

    public ClampGraphBuilder policy(String policy) {
        this.policy = policy;
        return this;
    }

    public ClampGraphBuilder addMicroService(BlueprintMicroService ms) {
        microServices.add(ms);
        return this;
    }

    public ClampGraphBuilder addAllMicroServices(List<BlueprintMicroService> msList) {
        microServices.addAll(msList);
        return this;
    }

    /**
     * Build the SVG.
     * 
     * @return Clamp graph (SVG)
     */
    public ClampGraph build() {
        if (microServices.isEmpty()) {
            throw new InvalidStateException("At least one microservice is required");
        }
        if (Objects.isNull(policy) || policy.trim().isEmpty()) {
            throw new InvalidStateException("Policy element must be present");
        }
        return new ClampGraph(painter.doPaint(collector, microServices, policy));
    }
}
