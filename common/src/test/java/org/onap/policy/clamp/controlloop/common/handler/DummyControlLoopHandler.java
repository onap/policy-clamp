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

package org.onap.policy.clamp.controlloop.common.handler;

import java.util.List;
import org.onap.policy.common.endpoints.event.comm.TopicSink;
import org.onap.policy.common.endpoints.listeners.MessageTypeDispatcher;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

public class DummyControlLoopHandler extends ControlLoopHandler {

    public DummyControlLoopHandler(PolicyModelsProviderParameters databaseProviderParameters) {
        super(databaseProviderParameters);
    }

    @Override
    public void startAndRegisterListeners(MessageTypeDispatcher msgDispatcher) {
        // Do nothing on this dummy class
    }

    @Override
    public void startAndRegisterPublishers(List<TopicSink> topicSinks) {
        // Do nothing on this dummy class
    }

    @Override
    public void stopAndUnregisterPublishers() {
        // Do nothing on this dummy class
    }

    @Override
    public void stopAndUnregisterListeners(MessageTypeDispatcher msgDispatcher) {
        // Do nothing on this dummy class
    }

    @Override
    public void startProviders() {
        // Do nothing on this dummy class
    }

    @Override
    public void stopProviders() {
        // Do nothing on this dummy class
    }
}
