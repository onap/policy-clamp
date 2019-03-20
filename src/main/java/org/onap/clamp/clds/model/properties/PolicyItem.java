/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model.properties;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.onap.clamp.clds.util.JsonUtils;
import org.yaml.snakeyaml.Yaml;

/**
 * Parse policyConfigurations from Policy json properties.
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
 * "vf3RtPi"]},{"name":
 * "targetResourceId","value":["Eace933104d443b496b8.nodes.heat.vpg"]}]]}]
 */
public class PolicyItem implements Cloneable {
    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(PolicyItem.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    private String id;
    private String recipe;
    private int maxRetries;
    private int retryTimeLimit;
    private String parentPolicy;
    private List<String> parentPolicyConditions;
    private String actor;
    private String targetResourceId;
    private String recipeInfo;
    private String recipeLevel;
    private String recipeInput;
    private Map<String, String> recipePayload;
    private String oapRop;
    private String oapLimit;

    private String enableGuardPolicy;
    private String guardPolicyType;
    private String guardTargets;
    private String minGuard;
    private String maxGuard;
    private String limitGuard;
    private String timeUnitsGuard;
    private String timeWindowGuard;
    private String guardActiveStart;
    private String guardActiveEnd;

    /**
     * Parse Policy given json node.
     *
     * @param item
     * @throws IOException
     */
    public PolicyItem(JsonElement item) throws IOException {
        id = JsonUtils.getStringValueByName(item, "_id");
        recipe = JsonUtils.getStringValueByName(item, "recipe");
        maxRetries = JsonUtils.getIntValueByName(item, "maxRetries");
        retryTimeLimit = JsonUtils.getIntValueByName(item, "retryTimeLimit");
        parentPolicy = JsonUtils.getStringValueByName(item, "parentPolicy");
        parentPolicyConditions = JsonUtils.getStringValuesByName(item, "parentPolicyConditions");
        targetResourceId = JsonUtils.getStringValueByName(item, "targetResourceId");
        if (targetResourceId != null && targetResourceId.isEmpty()) {
            this.setTargetResourceId(null);
        }
        recipeInfo = JsonUtils.getStringValueByName(item, "recipeInfo");
        recipeLevel = JsonUtils.getStringValueByName(item, "recipeLevel");
        recipeInput = JsonUtils.getStringValueByName(item, "recipeInput");
        String payload = JsonUtils.getStringValueByName(item, "recipePayload");

        if (payload != null && !payload.isEmpty()) {
            if (payload.trim().startsWith("{") && payload.trim().endsWith("}")) {
                // Seems to be a JSON
                recipePayload = JsonUtils.GSON.fromJson(payload, new TypeToken<Map<String, String>>() {}.getType());
            } else {
                // SHould be a YAML then
                recipePayload = new Yaml().load(payload);
            }
        }
        oapRop = JsonUtils.getStringValueByName(item, "oapRop");
        oapLimit = JsonUtils.getStringValueByName(item, "oapLimit");
        actor = JsonUtils.getStringValueByName(item, "actor");

        enableGuardPolicy = JsonUtils.getStringValueByName(item, "enableGuardPolicy");
        guardPolicyType = JsonUtils.getStringValueByName(item, "guardPolicyType");
        guardTargets = JsonUtils.getStringValueByName(item, "guardTargets");
        minGuard = JsonUtils.getStringValueByName(item, "minGuard");
        maxGuard = JsonUtils.getStringValueByName(item, "maxGuard");
        limitGuard = JsonUtils.getStringValueByName(item, "limitGuard");
        timeUnitsGuard = JsonUtils.getStringValueByName(item, "timeUnitsGuard");
        timeWindowGuard = JsonUtils.getStringValueByName(item, "timeWindowGuard");
        guardActiveStart = JsonUtils.getStringValueByName(item, "guardActiveStart");
        guardActiveEnd = JsonUtils.getStringValueByName(item, "guardActiveEnd");
    }

    /**
     * Get the id.
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the recipe.
     * @return the recipe
     */
    public String getRecipe() {
        return recipe;
    }

    /**
     * Set the id.
     * @param the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Set the recipe.
     * @param the recipe
     */
    public void setRecipe(String recipe) {
        this.recipe = recipe;
    }

    /**
     * Set the parent policy.
     * @param the parentPolicy
     */
    public void setParentPolicy(String parentPolicy) {
        this.parentPolicy = parentPolicy;
    }

    /**
     * Set the max retries.
     * @param the maxRetries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * Set the retry time limit.
     * @param the retryTimeLimit
     */
    public void setRetryTimeLimit(int retryTimeLimit) {
        this.retryTimeLimit = retryTimeLimit;
    }

    /**
     * Set the parent policy conditions.
     * @param the parentPolicyConditions
     */
    public void setParentPolicyConditions(List<String> parentPolicyConditions) {
        this.parentPolicyConditions = parentPolicyConditions;
    }

    /**
     * Get the max retires.
     * @return the maxRetries
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Get the retry time limit.
     * @return the retryTimeLimit
     */
    public int getRetryTimeLimit() {
        return retryTimeLimit;
    }

    /**
     * Get the parent policy.
     * @return the parentPolicy
     */
    public String getParentPolicy() {
        return parentPolicy;
    }

    /**
     * Get the list of parent policy conditions.
     * @return the parentPolicyConditions
     */
    public List<String> getParentPolicyConditions() {
        return parentPolicyConditions;
    }

    /**
     * Get the actor.
     * @return the actor
     */
    public String getActor() {
        return actor;
    }

    /**
     * Set the actor.
     * @param the actor
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getTargetResourceId() {
        return targetResourceId;
    }

    public void setTargetResourceId(String targetResourceId) {
        this.targetResourceId = targetResourceId;
    }

    public String getRecipeInfo() {
        return recipeInfo;
    }

    public String getRecipeLevel() {
        return recipeLevel;
    }

    public String getRecipeInput() {
        return recipeInput;
    }

    public Map<String, String> getRecipePayload() {
        return recipePayload;
    }

    /**
     * Get oap rop.
     * @return The oap rop?
     */
    public String getOapRop() {
        if (oapRop == null) {
            oapRop = "0m";
        }
        return oapRop;
    }

    /**
     * Get oap limit.
     * @return the oap limit
     */
    public String getOapLimit() {
        if (oapLimit == null) {
            oapLimit = "0";
        }
        return oapLimit;
    }

    public String getEnableGuardPolicy() {
        return enableGuardPolicy;
    }

    public String getGuardPolicyType() {
        return guardPolicyType;
    }

    public String getGuardTargets() {
        return guardTargets;
    }

    public String getMinGuard() {
        return minGuard;
    }

    public String getMaxGuard() {
        return maxGuard;
    }

    public String getLimitGuard() {
        return limitGuard;
    }

    public String getTimeUnitsGuard() {
        return timeUnitsGuard;
    }

    public String getTimeWindowGuard() {
        return timeWindowGuard;
    }

    public String getGuardActiveStart() {
        return guardActiveStart;
    }

    public String getGuardActiveEnd() {
        return guardActiveEnd;
    }

}
