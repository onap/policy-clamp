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

package org.onap.clamp.dao.model;

import com.google.gson.annotations.Expose;
import com.vladmihalcea.hibernate.type.json.JsonStringType;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
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

@Entity
@Table(name = "loops")
//@JsonAdapter(JsonLoopAdapter.class)
@TypeDef(name = "json", typeClass = JsonStringType.class)
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
    @Column(name = "svg_representation")
    private String svgRepresentation;

    @Expose
    @Type(type = "json")
    @Column(columnDefinition = "json", name = "global_properties_json")
    private Map<String, Object> globalPropertiesJson;

    @Expose
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDcaeDeploymentId() {
        return dcaeDeploymentId;
    }

    public void setDcaeDeploymentId(String dcaeDeploymentId) {
        this.dcaeDeploymentId = dcaeDeploymentId;
    }

    public String getDcaeDeploymentStatusUrl() {
        return dcaeDeploymentStatusUrl;
    }

    public void setDcaeDeploymentStatusUrl(String dcaeDeploymentStatusUrl) {
        this.dcaeDeploymentStatusUrl = dcaeDeploymentStatusUrl;
    }

    public String getSvgRepresentation() {
        return svgRepresentation;
    }

    public void setSvgRepresentation(String svgRepresentation) {
        this.svgRepresentation = svgRepresentation;
    }

    public String getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(String blueprint) {
        this.blueprint = blueprint;
    }

    public LoopState getLastComputedState() {
        return lastComputedState;
    }

    public void setLastComputedState(LoopState lastComputedState) {
        this.lastComputedState = lastComputedState;
    }

    public Set<OperationalPolicy> getOperationalPolicies() {
        return operationalPolicies;
    }

    public void setOperationalPolicies(Set<OperationalPolicy> operationalPolicies) {
        this.operationalPolicies = operationalPolicies;
    }

    public Set<MicroServicePolicy> getMicroServicePolicies() {
        return microServicePolicies;
    }

    public void setMicroServicePolicies(Set<MicroServicePolicy> microServicePolicies) {
        this.microServicePolicies = microServicePolicies;
    }

    public Map<String, Object> getGlobalPropertiesJson() {
        return globalPropertiesJson;
    }

    public void setGlobalPropertiesJson(Map<String, Object> globalPropertiesJson) {
        this.globalPropertiesJson = globalPropertiesJson;
    }

    public Set<LoopLog> getLoopLogs() {
        return loopLogs;
    }

    public void setLoopLogs(Set<LoopLog> loopLogs) {
        this.loopLogs = loopLogs;
    }

    public void addOperationalPolicy(OperationalPolicy opPolicy) {
        opPolicy.setLoop(this);
        operationalPolicies.add(opPolicy);
    }

    public void addMicroServicePolicy(MicroServicePolicy microServicePolicy) {
        microServicePolicies.add(microServicePolicy);
        microServicePolicy.getUsedByLoops().add(this);
    }

    public void addLog(LoopLog log) {
        loopLogs.add(log);
        log.setLoop(this);
    }
}
