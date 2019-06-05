/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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
 * ===================================================================
 *
 */

package org.onap.clamp.loop.components.external;

import com.google.gson.annotations.Expose;

/**
 * This is a transient state reflecting the deployment status of a component. It
 * can be Policy, DCAE, or whatever... This is object is generic. Clamp is now
 * stateless, so it triggers the different components at runtime, the status per
 * component is stored here.
 *
 */
public class ExternalComponentState {
    @Expose
    private String stateName;
    @Expose
    private String description;

    public ExternalComponentState(String stateName, String description) {
        this.stateName = stateName;
        this.description = description;
    }

    public ExternalComponentState() {
    }

    public String getStateName() {
        return stateName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return stateName;
    }
}
