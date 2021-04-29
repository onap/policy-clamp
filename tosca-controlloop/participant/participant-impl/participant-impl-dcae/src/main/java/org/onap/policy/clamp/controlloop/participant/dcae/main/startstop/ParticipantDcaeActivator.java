/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.participant.dcae.main.startstop;

import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.onap.policy.clamp.controlloop.participant.dcae.main.handler.DcaeHandler;
import org.onap.policy.clamp.controlloop.participant.dcae.main.parameters.ParticipantDcaeParameters;
import org.onap.policy.common.utils.services.ServiceManagerContainer;

/**
 * This class activates the control loop runtime component as a complete service together with all its controllers,
 * listeners & handlers.
 */
public class ParticipantDcaeActivator extends ServiceManagerContainer {
    @Getter
    private final ParticipantDcaeParameters parameters;

    /**
     * Instantiate the activator for the dcae as a complete service.
     *
     * @param parameters the parameters for the control loop runtime service
     */
    public ParticipantDcaeActivator(final ParticipantDcaeParameters parameters) {
        this.parameters = parameters;

        final AtomicReference<DcaeHandler> dcaeHandler = new AtomicReference<>();

        // @formatter:off
        addAction("Dcae Handler",
            () -> dcaeHandler.set(new DcaeHandler(parameters)),
            () -> dcaeHandler.get().close());

        addAction("Dcae Providers",
            () -> dcaeHandler.get().startProviders(),
            () -> dcaeHandler.get().stopProviders());

        // @formatter:on
    }
}
