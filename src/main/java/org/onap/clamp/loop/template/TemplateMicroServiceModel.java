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

package org.onap.clamp.loop.template;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Entity
@Table(name = "templates_microservicemodels")
public class TemplateMicroServiceModel implements Serializable, Comparable<TemplateMicroServiceModel> {

    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 5924989899078094245L;

    @EmbeddedId
    private TemplateMicroServiceModelId templateMicroServiceModelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("loopTemplateName")
    @JoinColumn(name = "loop_template_name")
    private LoopTemplate loopTemplate;

    @Expose
    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
    @MapsId("microServiceModelName")
    @JoinColumn(name = "micro_service_model_name")
    private MicroServiceModel microServiceModel;

    @Expose
    @Column(nullable = false, name = "flow_order")
    private Integer flowOrder;

    /**
     * Default constructor for serialization.
     */
    public TemplateMicroServiceModel() {

    }

    /**
     * Constructor.
     * 
     * @param loopTemplate      The loop template object
     * @param microServiceModel The microServiceModel object
     * @param flowOrder         The position of the micro service in the flow
     */
    public TemplateMicroServiceModel(LoopTemplate loopTemplate, MicroServiceModel microServiceModel,
            Integer flowOrder) {
        this.loopTemplate = loopTemplate;
        this.microServiceModel = microServiceModel;
        this.flowOrder = flowOrder;
        this.templateMicroServiceModelId = new TemplateMicroServiceModelId(loopTemplate.getName(),
                microServiceModel.getName());
    }

    /**
     * loopTemplate getter.
     * 
     * @return the loopTemplate
     */
    public LoopTemplate getLoopTemplate() {
        return loopTemplate;
    }

    /**
     * loopTemplate setter.
     * 
     * @param loopTemplate the loopTemplate to set
     */
    public void setLoopTemplate(LoopTemplate loopTemplate) {
        this.loopTemplate = loopTemplate;
    }

    /**
     * microServiceModel getter.
     * 
     * @return the microServiceModel
     */
    public MicroServiceModel getMicroServiceModel() {
        return microServiceModel;
    }

    /**
     * microServiceModel setter.
     * 
     * @param microServiceModel the microServiceModel to set
     */
    public void setMicroServiceModel(MicroServiceModel microServiceModel) {
        this.microServiceModel = microServiceModel;
    }

    /**
     * flowOrder getter.
     * 
     * @return the flowOrder
     */
    public Integer getFlowOrder() {
        return flowOrder;
    }

    /**
     * flowOrder setter.
     * 
     * @param flowOrder the flowOrder to set
     */
    public void setFlowOrder(Integer flowOrder) {
        this.flowOrder = flowOrder;
    }

    @Override
    public int compareTo(TemplateMicroServiceModel arg0) {
        // Reverse it, so that by default we have the latest
        if (getFlowOrder() == null) {
            return 1;
        }
        if (arg0.getFlowOrder() == null) {
            return -1;
        }
        return arg0.getFlowOrder().compareTo(this.getFlowOrder());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((loopTemplate == null) ? 0 : loopTemplate.hashCode());
        result = prime * result + ((microServiceModel == null) ? 0 : microServiceModel.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TemplateMicroServiceModel other = (TemplateMicroServiceModel) obj;
        if (loopTemplate == null) {
            if (other.loopTemplate != null) {
                return false;
            }
        } else if (!loopTemplate.equals(other.loopTemplate)) {
            return false;
        }
        if (microServiceModel == null) {
            if (other.microServiceModel != null) {
                return false;
            }
        } else if (!microServiceModel.equals(other.microServiceModel)) {
            return false;
        }
        return true;
    }

}
