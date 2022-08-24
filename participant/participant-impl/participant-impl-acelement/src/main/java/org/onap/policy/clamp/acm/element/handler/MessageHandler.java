/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.element.handler;

import io.micrometer.core.annotation.Timed;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import org.onap.policy.clamp.acm.element.main.parameters.AcElement;
import org.onap.policy.clamp.acm.element.service.ElementService;
import org.onap.policy.clamp.common.acm.exception.AutomationCompositionRuntimeException;
import org.onap.policy.clamp.models.acm.messages.dmaap.element.ElementMessage;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementConfig;
import org.onap.policy.clamp.models.acm.messages.rest.element.ElementType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

@Component
public class MessageHandler {

    private ElementType elementType;
    private ToscaConceptIdentifier elementId;

    private Map<ElementType, ElementService> map = new HashMap<>();

    /**
     * Constructor.
     *
     * @param acElement       AcElement
     * @param elementServices ElementService list
     */
    public MessageHandler(AcElement acElement, List<ElementService> elementServices) {
        elementId = acElement.getElementId();
        elementServices.stream().forEach(elementService -> map.put(elementService.getType(), elementService));
    }

    /**
     * Active Element Service.
     *
     * @param elementConfig ElementConfig
     */
    public void active(@NonNull ElementConfig elementConfig) {
        this.elementType = elementConfig.getElementType();
        getActiveService().active(elementConfig);
    }

    /**
     * Update configuration.
     *
     * @param elementConfig ElementConfig
     */
    public void update(@NonNull ElementConfig elementConfig) {
        if (elementType == null) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, "ElementType not defined!");
        }
        if (!elementType.equals(elementConfig.getElementType())) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, "wrong ElementType!");
        }
        getActiveService().update(elementConfig);
    }

    /**
     * Get Active Service.
     *
     * @return ElementService
     */
    public ElementService getActiveService() {
        if (elementType == null) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, "ElementType not defined!");
        }
        var service = map.get(elementType);
        if (service == null) {
            throw new AutomationCompositionRuntimeException(Response.Status.CONFLICT, "ElementService not found!");
        }
        return service;
    }

    @Timed(value = "listener.status", description = "STATUS messages received")
    public void handleMessage(ElementMessage message) {
        getActiveService().handleMessage(message);
    }

    public boolean appliesTo(final ToscaConceptIdentifier elementId) {
        return this.elementId.equals(elementId);
    }

    public void deactivateElement() {
        getActiveService().deactivate();
        elementType = null;
    }
}
