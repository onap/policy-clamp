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

import org.apache.camel.Exchange;

/**
 *
 * SHould be abstract but Gson can't instantiate it if it's an abstract
 *
 */
public class ExternalComponent {
    @Expose
    private ExternalComponentState componentState;

    public void setState(ExternalComponentState newState) {
        this.componentState = newState;
    }

    public ExternalComponentState getState() {
        return this.componentState;
    }

    public String getComponentName() {
        return null;
    }

    public ExternalComponentState computeState(Exchange camelExchange) {
        return new ExternalComponentState("INIT", "no desc");
    }

    public ExternalComponent(ExternalComponentState initialState) {
        setState(initialState);
    }

    public ExternalComponent() {
    }
}
