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
@Table(name = "looptemplates_to_loopelementmodels")
public class LoopTemplateLoopElementModel implements Serializable, Comparable<LoopTemplateLoopElementModel> {

    /**
     * Serial ID.
     */
    private static final long serialVersionUID = 5924989899078094245L;

    @EmbeddedId
    private LoopTemplateLoopElementModelId loopTemplateLoopElementModelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("loopTemplateName")
    @JoinColumn(name = "loop_template_name")
    private LoopTemplate loopTemplate;

    @Expose
    @ManyToOne(fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH })
    @MapsId("loopElementModelName")
    @JoinColumn(name = "loop_element_model_name")
    private LoopElementModel loopElementModel;

    @Expose
    @Column(nullable = false, name = "flow_order")
    private Integer flowOrder;

    /**
     * Default constructor for serialization.
     */
    public LoopTemplateLoopElementModel() {

    }

    /**
     * Constructor.
     * 
     * @param loopTemplate     The loop template object
     * @param loopElementModel The loopElementModel object
     * @param flowOrder        The position of the micro service in the flow
     */
    public LoopTemplateLoopElementModel(LoopTemplate loopTemplate, LoopElementModel loopElementModel,
            Integer flowOrder) {
        this.loopTemplate = loopTemplate;
        this.loopElementModel = loopElementModel;
        this.flowOrder = flowOrder;
        this.loopTemplateLoopElementModelId = new LoopTemplateLoopElementModelId(loopTemplate.getName(),
                loopElementModel.getName());
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
     * loopElementModel getter.
     * 
     * @return the loopElementModel
     */
    public LoopElementModel getLoopElementModel() {
        return loopElementModel;
    }

    /**
     * loopElementModel setter.
     * 
     * @param loopElementModel the loopElementModel to set
     */
    public void setLoopElementModel(LoopElementModel loopElementModel) {
        this.loopElementModel = loopElementModel;
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
    public int compareTo(LoopTemplateLoopElementModel arg0) {
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
        result = prime * result + ((loopElementModel == null) ? 0 : loopElementModel.hashCode());
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
        LoopTemplateLoopElementModel other = (LoopTemplateLoopElementModel) obj;
        if (loopTemplate == null) {
            if (other.loopTemplate != null) {
                return false;
            }
        } else if (!loopTemplate.equals(other.loopTemplate)) {
            return false;
        }
        if (loopElementModel == null) {
            if (other.loopElementModel != null) {
                return false;
            }
        } else if (!loopElementModel.equals(other.loopElementModel)) {
            return false;
        }
        return true;
    }

}
