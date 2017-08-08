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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.service.CldsService;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parse model properties.
 */
public class ModelProperties {
    protected static final EELFLogger                                 logger              = EELFManager.getInstance()
            .getLogger(CldsService.class);
    protected static final EELFLogger                           auditLogger         = EELFManager.getInstance()
            .getAuditLogger();

    private ModelBpmn                                         modelBpmn;
    private JsonNode                                          modelJson;

    private final String                                      modelName;
    private final String                                      controlName;
    private final String                                      actionCd;
    // Flag indicate whether it is triggered by Validation Test button from UI
    private final boolean 									  isTest;

    private Global                                            global;
    private Tca                                               tca;

    private final Map<String, ModelElement>                   modelElements       = new ConcurrentHashMap<>();

    private String                                            currentModelElementId;
    private String                                            policyUniqueId;

    private static final Object                               lock                = new Object();
    private static Map<Class<? extends ModelElement>, String> modelElementClasses = new ConcurrentHashMap<>();

    static {
        synchronized (lock) {
            modelElementClasses.put(Collector.class, Collector.getType());
            modelElementClasses.put(Policy.class, Policy.getType());
            modelElementClasses.put(StringMatch.class, StringMatch.getType());
            modelElementClasses.put(Tca.class, Tca.getType());
        }
    }

    /**
     * Retain data required to parse the ModelElement objects. (Rather than
     * parse them all - parse them on demand if requested.)
     *
     * @param modelName
     * @param controlName
     * @param actionCd
     * @param isTest
     * @param modelBpmnPropText
     * @param modelPropText
     * @throws JsonProcessingException
     * @throws IOException
     */
    public ModelProperties(String modelName, String controlName, String actionCd, boolean isTest, String modelBpmnPropText,
            String modelPropText) throws IOException {
        this.modelName = modelName;
        this.controlName = controlName;
        this.actionCd = actionCd;
        this.isTest = isTest;
        modelBpmn = ModelBpmn.create(modelBpmnPropText);
        modelJson = new ObjectMapper().readTree(modelPropText);

        instantiateMissingModelElements();
    }

    /**
     * This method is meant to ensure that one ModelElement instance exists for
     * each ModelElement class.
     *
     * As new ModelElement classes could have been registered after
     * instantiation of this ModelProperties, we need to build the missing
     * ModelElement instances.
     */
    private final void instantiateMissingModelElements() {
        if (modelElementClasses.size() != modelElements.size()) {
            Set<String> missingTypes = new HashSet<>(modelElementClasses.values());
            missingTypes.removeAll(modelElements.keySet());
            // Parse the list of base Model Elements and build up the
            // ModelElements
            modelElementClasses.entrySet().stream().parallel()
                    .filter(entry -> (ModelElement.class.isAssignableFrom(entry.getKey())
                            && missingTypes.contains(entry.getValue())))
                    .forEach(entry -> {
                        try {
                            modelElements.put(entry.getValue(),
                                    (entry.getKey()
                                            .getConstructor(ModelProperties.class, ModelBpmn.class, JsonNode.class)
                                            .newInstance(this, modelBpmn, modelJson)));
                        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException
                                | InvocationTargetException e) {
                            logger.warn("Unable to instantiate a ModelElement, exception follows: " + e);
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
        // String modelProp = (String) execution.getVariable("modelProp");
        String modelProp = new String((byte[]) execution.getVariable("modelProp"));
        String modelBpmnProp = (String) execution.getVariable("modelBpmnProp");
        String modelName = (String) execution.getVariable("modelName");
        String controlName = (String) execution.getVariable("controlName");
        String actionCd = (String) execution.getVariable("actionCd");
        boolean isTest = (boolean)execution.getVariable("isTest");

        return new ModelProperties(modelName, controlName, actionCd, isTest, modelBpmnProp, modelProp);
    }

    /**
     * return appropriate model element given the type
     *
     * @param type
     * @return
     */
    public ModelElement getModelElementByType(String type) {
        ModelElement modelElement = modelElements.get(type);
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

    /**
     * @return the currentPolicyScopeAndPolicyName
     */
    public String getCurrentPolicyScopeAndPolicyName() {
        return normalizePolicyScopeName(modelName + "." + getCurrentPolicyName());
    }

    /**
     * @return the policyScopeAndNameWithUniqueId
     */
    public String getPolicyScopeAndNameWithUniqueId() {
        return normalizePolicyScopeName(modelName + "." + getCurrentPolicyName() + "_" + policyUniqueId);
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
     * @return the actionCd
     */
    public String getActionCd() {
        return actionCd;
    }

	/**
	 * @return the isTest
	 */
	public boolean isTest() {
		return isTest;
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

    public static final synchronized void registerModelElement(Class<? extends ModelElement> modelElementClass,
            String type) {
        if (!modelElementClasses.containsKey(modelElementClass.getClass())) {
            modelElementClasses.put(modelElementClass, type);
        }
    }

    public <T extends ModelElement> T getType(Class<T> clazz) {
        instantiateMissingModelElements();
        String type = modelElementClasses.get(clazz);
        return (type != null ? (T) modelElements.get(type) : null);
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
