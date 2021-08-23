/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019, 2021 AT&T Intellectual Property. All rights
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

package org.onap.policy.clamp.loop.template;

import com.google.gson.annotations.Expose;
import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.SortNatural;
import org.onap.policy.clamp.loop.common.AuditEntity;
import org.onap.policy.clamp.loop.service.Service;

@Entity
@Table(name = "loop_templates")
public class LoopTemplate extends AuditEntity implements Serializable {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = -286522707701388642L;

    @Id
    @Expose
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Column(name = "dcae_blueprint_id")
    private String dcaeBlueprintId;

    /**
     * This field is used when we have a blueprint defining all microservices. The
     * other option would be to have independent blueprint for each microservices.
     * In that case they are stored in each MicroServiceModel
     */
    @Column(columnDefinition = "MEDIUMTEXT", name = "blueprint_yaml")
    private String blueprint;

    @Expose
    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "loopTemplate",
            orphanRemoval = true)
    @SortNatural
    private SortedSet<LoopTemplateLoopElementModel> loopElementModelsUsed = new TreeSet<>();

    @Expose
    @ManyToOne(
            fetch = FetchType.EAGER,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REFRESH})
    @JoinColumn(name = "service_uuid")
    private Service modelService;

    @Expose
    @Column(name = "maximum_instances_allowed")
    private Integer maximumInstancesAllowed;

    @Expose
    @Column(name = "unique_blueprint", columnDefinition = "boolean default false")
    private boolean uniqueBlueprint;

    /**
     * Type of Loop allowed to be created.
     */
    @Expose
    @Column(name = "allowed_loop_type")
    @Convert(converter = LoopTypeConvertor.class)
    private LoopType allowedLoopType = LoopType.CLOSED;

    /**
     * name getter.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * name setter.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * blueprint getter.
     *
     * @return the blueprint
     */
    public String getBlueprint() {
        return blueprint;
    }

    /**
     * dcaeBlueprintId getter.
     *
     * @return the dcaeBlueprintId
     */
    public String getDcaeBlueprintId() {
        return dcaeBlueprintId;
    }

    /**
     * dcaeBlueprintId setter.
     *
     * @param dcaeBlueprintId the dcaeBlueprintId to set
     */
    public void setDcaeBlueprintId(String dcaeBlueprintId) {
        this.dcaeBlueprintId = dcaeBlueprintId;
    }

    /**
     * blueprint setter.
     *
     * @param blueprint the blueprint to set
     */
    public void setBlueprint(String blueprint) {
        this.blueprint = blueprint;
        if (blueprint == null) {
            this.uniqueBlueprint = false;
        } else {
            this.uniqueBlueprint = true;
        }
    }

    /**
     * loopElementModelsUsed getter.
     *
     * @return the loopElementModelsUsed
     */
    public SortedSet<LoopTemplateLoopElementModel> getLoopElementModelsUsed() {
        return loopElementModelsUsed;
    }

    /**
     * maximumInstancesAllowed getter.
     *
     * @return the maximumInstancesAllowed
     */
    public Integer getMaximumInstancesAllowed() {
        return maximumInstancesAllowed;
    }

    /**
     * maximumInstancesAllowed setter.
     *
     * @param maximumInstancesAllowed the maximumInstancesAllowed to set
     */
    public void setMaximumInstancesAllowed(Integer maximumInstancesAllowed) {
        this.maximumInstancesAllowed = maximumInstancesAllowed;
    }

    /**
     * allowedLoopType getter.
     *
     * @return the allowedLoopType Type of Loop allowed to be created
     */
    public LoopType getAllowedLoopType() {
        return allowedLoopType;
    }

    /**
     * allowedLoopType setter.
     *
     * @param allowedLoopType the allowedLoopType to set
     */
    public void setAllowedLoopType(LoopType allowedLoopType) {
        this.allowedLoopType = allowedLoopType;
    }

    /**
     * Add list of loopElements to the current template, each loopElementModel is
     * added at the end of the list so the flowOrder is computed automatically.
     *
     * @param loopElementModels The loopElementModel set to add
     */
    public void addLoopElementModels(Set<LoopElementModel> loopElementModels) {
        for (LoopElementModel loopElementModel : loopElementModels) {
            addLoopElementModel(loopElementModel);
        }
    }

    /**
     * Add a loopElement to the current template, the loopElementModel is added at
     * the end of the list so the flowOrder is computed automatically.
     *
     * @param loopElementModel The loopElementModel to add
     */
    public void addLoopElementModel(LoopElementModel loopElementModel) {
        this.addLoopElementModel(loopElementModel, this.loopElementModelsUsed.size());
    }

    /**
     * Add a loopElement model to the current template, the flow order must be
     * specified manually.
     *
     * @param loopElementModel The loopElementModel to add
     * @param listPosition     The position in the flow
     */
    public void addLoopElementModel(LoopElementModel loopElementModel, Integer listPosition) {
        var jointEntry = new LoopTemplateLoopElementModel(this, loopElementModel, listPosition);
        this.loopElementModelsUsed.add(jointEntry);
        loopElementModel.getUsedByLoopTemplates().add(jointEntry);
    }

    /**
     * modelService getter.
     *
     * @return the modelService
     */
    public Service getModelService() {
        return modelService;
    }

    /**
     * modelService setter.
     *
     * @param modelService the modelService to set
     */
    public void setModelService(Service modelService) {
        this.modelService = modelService;
    }

    /**
     * uniqueBlueprint getter.
     *
     * @return the uniqueBlueprint
     */
    public boolean getUniqueBlueprint() {
        return uniqueBlueprint;
    }

    /**
     * Default constructor for serialization.
     */
    public LoopTemplate() {

    }

    /**
     * Constructor.
     *
     * @param name                The loop template name id
     * @param blueprint           The blueprint containing all microservices (legacy
     *                            case)
     * @param maxInstancesAllowed The maximum number of instances that can be
     *                            created from that template
     * @param service             The service associated to that loop template
     */
    public LoopTemplate(String name, String blueprint, Integer maxInstancesAllowed, Service service) {
        this.name = name;
        this.setBlueprint(blueprint);

        this.maximumInstancesAllowed = maxInstancesAllowed;
        this.modelService = service;
    }

    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
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
        LoopTemplate other = (LoopTemplate) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Generate the loop template name.
     *
     * @param serviceName       The service name
     * @param serviceVersion    The service version
     * @param resourceName      The resource name
     * @param blueprintFileName The blueprint file name
     * @return The generated loop template name
     */
    public static String generateLoopTemplateName(String serviceName, String serviceVersion,
                                                  String resourceName, String blueprintFileName) {
        StringBuilder buffer = new StringBuilder("LOOP_TEMPLATE_").append(serviceName).append("_v")
                .append(serviceVersion).append("_").append(resourceName).append("_")
                .append(blueprintFileName.replaceAll(".yaml", ""));
        return buffer.toString().replace('.', '_').replace(" ", "");
    }
}
