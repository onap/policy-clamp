/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.model.prop;

import java.util.List;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Parse policyConfigurations from Policy json properties.
 * <p>
 * Example json:
 * "Policy_005sny1":[[{"name":"timeout","value":"5"}],{"policyConfigurations":[[
 * {"name":"recipe","value":["restart"]},{"name":"maxRetries","value":["3"]},{
 * "name":"retryTimeLimit","value":["180"]},{"name":"_id","value":["vf3RtPi"]},{
 * "name":"location","value":["san_diego"]},{"name":"resource","value":["vCTS"]}
 * ,{"name":"onMaxRetriesLimit","value":[""]},{"name":"onTimeLimit","value":[""]
 * },{"name":"onOtherFailure","value":[""]},{"name":"policy_parent","value":[""]
 * }],[{"name":"recipe","value":["rebuild"]},{"name":"maxRetries","value":["3"]}
 * ,{"name":"retryTimeLimit","value":["180"]},{"name":"_id","value":["89z8Ncl"]}
 * ,{"name":"location","value":["san_diego"]},{"name":"resource","value":["vCTS"
 * ]},{"name":"onMaxRetriesLimit","value":[""]},{"name":"onTimeLimit","value":[
 * ""]},{"name":"onOtherFailure","value":[""]},{"name":"policy_parent","value":[
 * "vf3RtPi"]}]]}]
 */
public class PolicyItem implements Cloneable {
    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(PolicyItem.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String                  id;
    private String                  recipe;
    private int                     maxRetries;
    private int                     retryTimeLimit;
    private String                  parentPolicy;
    private List<String>            parentPolicyConditions;
    private String                  actor;

    /**
     * Parse Policy given json node.
     *
     * @param node
     */
    public PolicyItem(JsonNode node) {
        id = ModelElement.getValueByName(node, "_id");
        recipe = ModelElement.getValueByName(node, "recipe");
        maxRetries = ModelElement.getIntValueByName(node, "maxRetries");
        retryTimeLimit = ModelElement.getIntValueByName(node, "retryTimeLimit");
        parentPolicy = ModelElement.getValueByName(node, "parentPolicy");
        parentPolicyConditions = ModelElement.getValuesByName(node, "parentPolicyConditions");

    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the recipe
     */
    public String getRecipe() {
        return recipe;
    }

    /**
     * @set the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @set the recipe
     */
    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    /**
     * @set the parentPolicy
     */
    public void setParentPolicy(String parentPolicy) {
        this.parentPolicy = parentPolicy;
    }

    /**
     * @set the maxRetries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * @set the retryTimeLimit
     */
    public void setRetryTimeLimit(int retryTimeLimit) {
        this.retryTimeLimit = retryTimeLimit;
    }

    /**
     * @set the parentPolicyConditions
     */
    public void setParentPolicyConditions(List<String> parentPolicyConditions) {
        this.parentPolicyConditions = parentPolicyConditions;
    }

    /**
     * @return the maxRetries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * @return the retryTimeLimit
     */
    public int getRetryTimeLimit() {
        return retryTimeLimit;
    }

    /**
     * @return the parentPolicy
     */
    public String getParentPolicy() {
        return parentPolicy;
    }

    /**
     * @return the parentPolicyConditions
     */
    public List<String> getParentPolicyConditions() {
        return parentPolicyConditions;
    }

    /**
     * @return the actor
     */
    public String getActor() {
        return actor;
    }

    /**
     * @set the actor
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
