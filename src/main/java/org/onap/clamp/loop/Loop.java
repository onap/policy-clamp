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

package org.onap.clamp.loop;

import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.dao.model.jsontype.StringJsonUserType;

@Entity
@Table(name = "loops")
@TypeDefs({ @TypeDef(name = "json", typeClass = StringJsonUserType.class) })
public class Loop implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -286522707701388642L;

    @Id
    @Expose
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Expose
    @Column(name = "dcae_deployment_id")
    private String dcaeDeploymentId;

    @Expose
    @Column(name = "dcae_deployment_status_url")
    private String dcaeDeploymentStatusUrl;

    @Expose
    @Column(name = "dcae_blueprint_id")
    private String dcaeBlueprintId;

    @Column(name = "svg_representation")
    private String svgRepresentation;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "global_properties_json")
    private JsonObject globalPropertiesJson;

    @Column(nullable = false, name = "blueprint_yaml")
    private String blueprint;

    @Expose
    @Column(nullable = false, name = "last_computed_state")
    @Enumerated(EnumType.STRING)
    private LoopState lastComputedState;

    @Expose
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "loop")
    private Set<OperationalPolicy> operationalPolicies = new HashSet<>();

    @Expose
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "loops_microservicepolicies", joinColumns = @JoinColumn(name = "loop_id"), inverseJoinColumns = @JoinColumn(name = "microservicepolicy_id"))
    private Set<MicroServicePolicy> microServicePolicies = new HashSet<>();

    @Expose
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "loop")
    private Set<LoopLog> loopLogs = new HashSet<>();

    public Loop() {
    }

    public Loop(String name, String blueprint, String svgRepresentation) {
        this.name = name;
        this.svgRepresentation = svgRepresentation;
        this.blueprint = blueprint;
        this.lastComputedState = LoopState.DESIGN;
        this.globalPropertiesJson = new JsonObject();
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getDcaeDeploymentId() {
        return dcaeDeploymentId;
    }

    void setDcaeDeploymentId(String dcaeDeploymentId) {
        this.dcaeDeploymentId = dcaeDeploymentId;
    }

    public String getDcaeDeploymentStatusUrl() {
        return dcaeDeploymentStatusUrl;
    }

    void setDcaeDeploymentStatusUrl(String dcaeDeploymentStatusUrl) {
        this.dcaeDeploymentStatusUrl = dcaeDeploymentStatusUrl;
    }

    public String getSvgRepresentation() {
        return svgRepresentation;
    }

    void setSvgRepresentation(String svgRepresentation) {
        this.svgRepresentation = svgRepresentation;
    }

    public String getBlueprint() {
        return blueprint;
    }

    void setBlueprint(String blueprint) {
        this.blueprint = blueprint;
    }

    LoopState getLastComputedState() {
        return lastComputedState;
    }

    void setLastComputedState(LoopState lastComputedState) {
        this.lastComputedState = lastComputedState;
    }

    Set<OperationalPolicy> getOperationalPolicies() {
        return operationalPolicies;
    }

    void setOperationalPolicies(Set<OperationalPolicy> operationalPolicies) {
        this.operationalPolicies = operationalPolicies;
    }

    Set<MicroServicePolicy> getMicroServicePolicies() {
        return microServicePolicies;
    }

    void setMicroServicePolicies(Set<MicroServicePolicy> microServicePolicies) {
        this.microServicePolicies = microServicePolicies;
    }

    JsonObject getGlobalPropertiesJson() {
        return globalPropertiesJson;
    }

    void setGlobalPropertiesJson(JsonObject globalPropertiesJson) {
        this.globalPropertiesJson = globalPropertiesJson;
    }

    Set<LoopLog> getLoopLogs() {
        return loopLogs;
    }

    void setLoopLogs(Set<LoopLog> loopLogs) {
        this.loopLogs = loopLogs;
    }

    void addOperationalPolicy(OperationalPolicy opPolicy) {
        operationalPolicies.add(opPolicy);
        opPolicy.setLoop(this);
    }

    void addMicroServicePolicy(MicroServicePolicy microServicePolicy) {
        microServicePolicies.add(microServicePolicy);
        microServicePolicy.getUsedByLoops().add(this);
    }

    void addLog(LoopLog log) {
        loopLogs.add(log);
        log.setLoop(this);
    }

    public String getDcaeBlueprintId() {
        return dcaeBlueprintId;
    }

    public void setDcaeBlueprintId(String dcaeBlueprintId) {
        this.dcaeBlueprintId = dcaeBlueprintId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Loop other = (Loop) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
