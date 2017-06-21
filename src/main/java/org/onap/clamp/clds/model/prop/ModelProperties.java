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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parse model properties.
 */
public class ModelProperties {
    private static final Logger logger = Logger.getLogger(ModelProperties.class.getName());

    private ModelBpmn     modelBpmn;
    private JsonNode      modelJson;

    private final String  modelName;
    private final String  controlName;
    private final String  actionCd;

    private Global        global;
    private Collector     collector;
    private StringMatch   stringMatch;
    private Policy        policy;
    private Tca           tca;

    private String        currentModelElementId;
    private String        policyUniqueId;

    /**
     * Retain data required to parse the ModelElement objects. (Rather than
     * parse them all - parse them on demand if requested.)
     *
     * @param modelName
     * @param controlName
     * @param actionCd
     * @param modelBpmnPropText
     * @param modelPropText
     * @throws JsonProcessingException
     * @throws IOException
     */
    public ModelProperties(String modelName, String controlName, String actionCd, String modelBpmnPropText, String modelPropText) throws IOException {
        this.modelName = modelName;
        this.controlName = controlName;
        this.actionCd = actionCd;
        modelBpmn = ModelBpmn.create(modelBpmnPropText);
        ObjectMapper mapper = new ObjectMapper();
        modelJson = mapper.readTree(modelPropText);
    }

    /**
     * Get the VF for a model. If return null if there is no VF.
     *
     * @param model
     * @return
     */
    public static String getVf(CldsModel model) {
        List<String> vfs = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode modelJson = mapper.readTree(model.getPropText());
            Global global = new Global(modelJson);
            vfs = global.getResourceVf();
        } catch (IOException e) {
            // VF is null
        }
        String vf = null;
        if (vfs != null && !vfs.isEmpty()) {
            vf = vfs.get(0);
        }
        return vf;
    }

    /**
     * Create ModelProperties for Camunda Delegate.
     *
     * @param execution
     * @return
     * @throws JsonProcessingException
     * @throws IOException
     */
    public static ModelProperties create(DelegateExecution execution) throws IOException {
        String modelProp = (String) execution.getVariable("modelProp");
        String modelBpmnProp = (String) execution.getVariable("modelBpmnProp");
        String modelName = (String) execution.getVariable("modelName");
        String controlName = (String) execution.getVariable("controlName");
        String actionCd = (String) execution.getVariable("actionCd");

        return new ModelProperties(modelName, controlName, actionCd, modelBpmnProp, modelProp);
    }

    /**
     * return appropriate model element given the type
     *
     * @param type
     * @return
     */
    public ModelElement getModelElementByType(String type) {
        ModelElement me;
        switch (type) {
            case ModelElement.TYPE_COLLECTOR:
                me = getCollector();
                break;
            case ModelElement.TYPE_STRING_MATCH:
                me = getStringMatch();
                break;
            case ModelElement.TYPE_POLICY:
                me = getPolicy();
                break;
            case ModelElement.TYPE_TCA:
                me = getTca();
                break;
            default:
                throw new IllegalArgumentException("Invalid ModelElement type: " + type);
        }
        return me;
    }

    /**
     * @return the modelName
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * @return the controlName
     */
    public String getControlName() {
        return controlName;
    }

    /**
     * @return the controlNameAndPolicyUniqueId
     */
    public String getControlNameAndPolicyUniqueId() {
        return controlName + "_" + policyUniqueId;
    }

    /**
     * @return the currentPolicyName
     */
    private String getCurrentPolicyName() {
        return normalizePolicyScopeName(controlName + "_" + currentModelElementId);
    }

    /**
     * @return the currentPolicyScopeAndPolicyName
     */
    public String getCurrentPolicyScopeAndPolicyName() {
        return normalizePolicyScopeName(modelName + "." + getCurrentPolicyName());
    }

    /**
     * @return the currentPolicyScopeAndFullPolicyName
     */
    public String getCurrentPolicyScopeAndFullPolicyName(String policyNamePrefix) {
        return normalizePolicyScopeName(modelName + "." + policyNamePrefix + getCurrentPolicyName());
    }

    /**
     * @return the currentPolicyScopeAndFullPolicyNameWithVersion
     */
    public String getCurrentPolicyScopeAndFullPolicyNameWithVersion(String policyNamePrefix, int version) {
        return normalizePolicyScopeName(
                modelName + "." + policyNamePrefix + getCurrentPolicyName() + "." + version + ".xml");
    }

    /**
     * Replace all '-' with '_' within policy scope and name.
     *
     * @param inName
     * @return
     */
    private String normalizePolicyScopeName(String inName) {
        return inName.replaceAll("-", "_");
    }

    /**
     * @return the currentModelElementId
     */
    public String getCurrentModelElementId() {
        return currentModelElementId;
    }

    /**
     * When generating a policy request for a model element, must set the id of
     * that model element using this method. Used to generate the policy name.
     *
     * @param currentModelElementId
     *            the currentModelElementId to set
     */
    public void setCurrentModelElementId(String currentModelElementId) {
        this.currentModelElementId = currentModelElementId;
    }

    /**
     * @return the policyUniqueId
     */
    public String getPolicyUniqueId() {
        return policyUniqueId;
    }

    /**
     * When generating a policy request for a model element, must set the unique
     * id of that policy using this method. Used to generate the policy name.
     * 
     * @param policyUniqueId
     *            the policyUniqueId to set
     */
    public void setPolicyUniqueId(String policyUniqueId) {
        this.policyUniqueId = policyUniqueId;
    }

    /**
     * @return the collector
     */
    public Collector getCollector() {
        if (collector == null) {
            collector = new Collector(this, modelBpmn, modelJson);
        }
        return collector;
    }

    /**
     * @return the actionCd
     */
    public String getActionCd() {
        return actionCd;
    }

    /**
     * @return the isCreateRequest
     */
    public boolean isCreateRequest() {
        switch (actionCd) {
            case CldsEvent.ACTION_SUBMIT:
            case CldsEvent.ACTION_RESTART:
                return true;
        }
        return false;
    }

    public boolean isStopRequest() {
        switch (actionCd) {
            case CldsEvent.ACTION_STOP:
                return true;
        }
        return false;
    }

    /**
     * @return the global
     */
    public Global getGlobal() {
        if (global == null) {
            global = new Global(modelJson);
        }
        return global;
    }

    /**
     * @return the stringMatch
     */
    public StringMatch getStringMatch() {
        if (stringMatch == null) {
            stringMatch = new StringMatch(this, modelBpmn, modelJson);
        }
        return stringMatch;
    }

    /**
     * @return the policy
     */
    public Policy getPolicy() {
        if (policy == null) {
            policy = new Policy(this, modelBpmn, modelJson);
        }
        return policy;
    }

    /**
     * @return the tca
     */
    public Tca getTca() {
        if (tca == null) {
            tca = new Tca(this, modelBpmn, modelJson);
        }
        return tca;
    }
}
