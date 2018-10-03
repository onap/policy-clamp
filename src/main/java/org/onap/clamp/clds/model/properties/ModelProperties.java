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
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.camel.Exchange;
import org.onap.clamp.clds.client.req.policy.PolicyClient;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.exception.ModelBpmnException;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.util.JacksonUtils;

/**
 * Parse model properties.
 */
public class ModelProperties {

    protected static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsService.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();
    private ModelBpmn modelBpmn;
    private JsonNode modelJson;
    private final String modelName;
    private final String controlName;
    private final String actionCd;
    // Flag indicate whether it is triggered by Validation Test button from UI
    private final boolean testOnly;
    private Global global;
    private final Map<String, AbstractModelElement> modelElements = new ConcurrentHashMap<>();
    private String currentModelElementId;
    private String policyUniqueId;
    private String guardUniqueId;
    public static final String POLICY_GUARD_SUFFIX = "_Guard_";
    private static final Object lock = new Object();
    private static Map<Class<? extends AbstractModelElement>, String> modelElementClasses = new ConcurrentHashMap<>();
    static {
        synchronized (lock) {
            modelElementClasses.put(Policy.class, Policy.getType());
            modelElementClasses.put(Tca.class, Tca.getType());
            modelElementClasses.put(Holmes.class, Holmes.getType());
        }
    }

    /**
     * Retain data required to parse the ModelElement objects. (Rather than
     * parse them all - parse them on demand if requested.)
     *
     * @param modelName
     *            The model name coming form the UI
     * @param controlName
     *            The closed loop name coming from the UI
     * @param actionCd
     *            Type of operation PUT,UPDATE,DELETE
     * @param isATest
     *            The test flag coming from the UI (for validation only, no
     *            query are physically executed)
     * @param modelBpmnText
     *            The BPMN flow in JSON from the UI
     * @param modelPropText
     *            The BPMN parameters for all boxes defined in modelBpmnTest
     */
    public ModelProperties(String modelName, String controlName, String actionCd, boolean isATest, String modelBpmnText,
        String modelPropText) {
        try {
            this.modelName = modelName;
            this.controlName = controlName;
            this.actionCd = actionCd;
            this.testOnly = isATest;
            modelBpmn = ModelBpmn.create(modelBpmnText);
            modelJson = JacksonUtils.getObjectMapperInstance().readTree(modelPropText);
            instantiateMissingModelElements();
        } catch (IOException e) {
            throw new ModelBpmnException("Exception occurred when trying to decode the BPMN Properties JSON", e);
        }
    }

    /**
     * This method is meant to ensure that one ModelElement instance exists for
     * each ModelElement class. As new ModelElement classes could have been
     * registered after instantiation of this ModelProperties, we need to build
     * the missing ModelElement instances.
     */
    private final void instantiateMissingModelElements() {
        if (modelElementClasses.size() != modelElements.size()) {
            Set<String> missingTypes = new HashSet<>(modelElementClasses.values());
            missingTypes.removeAll(modelElements.keySet());
            // Parse the list of base Model Elements and build up the
            // ModelElements
            modelElementClasses.entrySet().stream().parallel()
            .filter(entry -> (AbstractModelElement.class.isAssignableFrom(entry.getKey())
                && missingTypes.contains(entry.getValue())))
            .forEach(entry -> {
                try {
                    modelElements.put(entry.getValue(),
                        (entry.getKey()
                            .getConstructor(ModelProperties.class, ModelBpmn.class, JsonNode.class)
                            .newInstance(this, modelBpmn, modelJson)));
                } catch (InstantiationException | NoSuchMethodException | IllegalAccessException
                    | InvocationTargetException e) {
                    logger.warn("Unable to instantiate a ModelElement, exception follows: ", e);
                }
            });
        }
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
            JsonNode modelJson = JacksonUtils.getObjectMapperInstance().readTree(model.getPropText());
            Global global = new Global(modelJson);
            vfs = global.getResourceVf();
        } catch (IOException e) {
            logger.warn("no VF found", e);
        }
        String vf = null;
        if (vfs != null && !vfs.isEmpty()) {
            vf = vfs.get(0);
        }
        return vf;
    }

    /**
     * Create ModelProperties extracted from a CamelExchange.
     *
     * @param camelExchange
     *            The camel Exchange object that contains all info provided to
     *            the flow
     * @return A model Properties created from the parameters found in
     *         camelExchange object
     */
    public static ModelProperties create(Exchange camelExchange) {
        String modelProp = (String) camelExchange.getProperty("modelProp");
        String modelBpmnProp = (String) camelExchange.getProperty("modelBpmnProp");
        String modelName = (String) camelExchange.getProperty("modelName");
        String controlName = (String) camelExchange.getProperty("controlName");
        String actionCd = (String) camelExchange.getProperty("actionCd");
        boolean isTest = (boolean) camelExchange.getProperty("isTest");
        return new ModelProperties(modelName, controlName, actionCd, isTest, modelBpmnProp, modelProp);
    }

    /**
     * return appropriate model element given the type
     *
     * @param type
     * @return
     */
    public AbstractModelElement getModelElementByType(String type) {
        AbstractModelElement modelElement = modelElements.get(type);
        if (modelElement == null) {
            throw new IllegalArgumentException("Invalid or not found ModelElement type: " + type);
        }
        return modelElement;
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

    private String createScopeSeparator(String policyScope) {
        return policyScope.contains(".") ? "" : ".";
    }

    /**
     * @return the currentPolicyScopeAndPolicyName
     */
    public String getCurrentPolicyScopeAndPolicyName() {
        return normalizePolicyScopeName(modelName + createScopeSeparator(modelName) + getCurrentPolicyName());
    }

    /**
     * @return The policyName that wil be used in input parameters of DCAE
     *         deploy
     */
    public String getPolicyNameForDcaeDeploy(ClampProperties refProp) {
        return normalizePolicyScopeName(modelName + createScopeSeparator(modelName)
        + refProp.getStringValue(PolicyClient.POLICY_MS_NAME_PREFIX_PROPERTY_NAME) + getCurrentPolicyName());
    }

    /**
     * @return the policyScopeAndNameWithUniqueId
     */
    public String getPolicyScopeAndNameWithUniqueId() {
        return normalizePolicyScopeName(
            modelName + createScopeSeparator(modelName) + getCurrentPolicyName() + "_" + policyUniqueId);
    }

    /**
     * @return the policyScopeAndNameWithUniqueId
     */
    public String getPolicyScopeAndNameWithUniqueGuardId() {
        return normalizePolicyScopeName(
            modelName + createScopeSeparator(modelName) + getCurrentPolicyName() + "_" + policyUniqueId+POLICY_GUARD_SUFFIX+guardUniqueId);
    }

    /**
     * @return the currentPolicyScopeAndFullPolicyName
     */
    public String getCurrentPolicyScopeAndFullPolicyName(String policyNamePrefix) {
        return normalizePolicyScopeName(
            modelName + createScopeSeparator(modelName) + policyNamePrefix + getCurrentPolicyName());
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

    public String getGuardUniqueId() {
        return guardUniqueId;
    }

    public void setGuardUniqueId(String guardUniqueId) {
        this.guardUniqueId = guardUniqueId;
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
     * @return the actionCd
     */
    public String getActionCd() {
        return actionCd;
    }

    /**
     * @return the testOnly
     */
    public boolean isTestOnly() {
        return testOnly;
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

    public static final synchronized void registerModelElement(Class<? extends AbstractModelElement> modelElementClass,
        String type) {
        if (!modelElementClasses.containsKey(modelElementClass.getClass())) {
            modelElementClasses.put(modelElementClass, type);
        }
    }

    public <T extends AbstractModelElement> T getType(Class<T> clazz) {
        instantiateMissingModelElements();
        String type = modelElementClasses.get(clazz);
        return (type != null ? (T) modelElements.get(type) : null);
    }
}
