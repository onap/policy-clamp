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

import java.util.HashSet;
import java.util.Set;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;

public class ClampGraphBuilder {
    private Set<OperationalPolicy> policies = new HashSet<>();
    private String collector;
    private Set<MicroServicePolicy> microServices = new HashSet<>();
    private Set<LoopElementModel> loopElementModels = new HashSet<>();
    private final Painter painter;

    public ClampGraphBuilder(Painter painter) {
        this.painter = painter;
    }

    public ClampGraphBuilder collector(String collector) {
        this.collector = collector;
        return this;
    }

    public ClampGraphBuilder addPolicy(OperationalPolicy policy) {
        this.policies.add(policy);
        return this;
    }

    public ClampGraphBuilder addAllPolicies(Set<OperationalPolicy> policies) {
        this.policies.addAll(policies);
        return this;
    }

    public ClampGraphBuilder addMicroService(MicroServicePolicy ms) {
        microServices.add(ms);
        return this;
    }

    public ClampGraphBuilder addAllMicroServices(Set<MicroServicePolicy> msList) {
        microServices.addAll(msList);
        return this;
    }

    /**
     * This method adds all loop element specified in input to the current structure.
     *
     * @param loopElementModels A set of LoopElementModels
     * @return Return the current ClampGraphBuilder
     */
    public ClampGraphBuilder addAllLoopElementModels(Set<LoopElementModel> loopElementModels) {
        for (LoopElementModel elem : loopElementModels) {
            this.addLoopElementModel(elem);
        }
        return this;
    }

    /**
     * This method adds one loop element specified in input to the current structure.
     *
     * @param loopElementModel A LoopElementModels
     * @return Return the current ClampGraphBuilder
     */
    public ClampGraphBuilder addLoopElementModel(LoopElementModel loopElementModel) {
        if (LoopElementModel.MICRO_SERVICE_TYPE.equals(loopElementModel.getLoopElementType())) {
            microServices.add(new MicroServicePolicy(loopElementModel.getName(),
                    loopElementModel.getPolicyModels().first(),
                    false,
                    null));
        } else if (LoopElementModel.OPERATIONAL_POLICY_TYPE.equals(loopElementModel.getLoopElementType())) {
            policies.add(new OperationalPolicy(loopElementModel.getName(), null, null,
                    loopElementModel.getPolicyModels().first()));
        }
        return this;
    }

    /**
     * Build the SVG.
     *
     * @return Clamp graph (SVG)
     */
    public ClampGraph build() {
        return new ClampGraph(painter.doPaint(collector, microServices, policies));
    }
}
