/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.sdc.controller.installer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.xml.transform.TransformerException;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.config.sdc.BlueprintParserFilesConfiguration;
import org.onap.clamp.clds.config.sdc.BlueprintParserMappingConfiguration;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.service.CldsTemplateService;
import org.onap.clamp.clds.transform.XslTransformer;
import org.onap.clamp.clds.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

/**
 * This class will be instantiated by spring config, and used by Sdc Controller. There is no state kept by the bean.
 * It's used to deploy the csar/notification received from SDC in DB.
 */
public class CsarInstallerImpl implements CsarInstaller {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarInstallerImpl.class);
    private Map<String, BlueprintParserFilesConfiguration> bpmnMapping = new HashMap<>();
    public static final String TEMPLATE_NAME_PREFIX = "DCAE-Designer-Template-";
    public static final String CONTROL_NAME_PREFIX = "ClosedLoop-";
    public static final String GET_INPUT_BLUEPRINT_PARAM = "get_input";
    // This will be used later as the policy scope
    public static final String MODEL_NAME_PREFIX = "CLAMP";
    /**
     * The file name that will be loaded by Spring.
     */
    @Value("${clamp.config.sdc.blueprint.parser.mapping:'classpath:/clds/blueprint-parser-mapping.json'}")
    protected String blueprintMappingFile;
    @Autowired
    protected ApplicationContext appContext;
    @Autowired
    private CldsDao cldsDao;
    @Autowired
    CldsTemplateService cldsTemplateService;
    @Autowired
    CldsService cldsService;
    @Autowired
    DcaeInventoryServices dcaeInventoryService;
    @Autowired
    private XslTransformer cldsBpmnTransformer;

    @PostConstruct
    public void loadConfiguration() throws IOException {
        BlueprintParserMappingConfiguration
            .createFromJson(appContext.getResource(blueprintMappingFile).getInputStream()).stream()
            .forEach(e -> bpmnMapping.put(e.getBlueprintKey(), e.getFiles()));
    }

    @Override
    public boolean isCsarAlreadyDeployed(CsarHandler csar) throws SdcArtifactInstallerException {
        boolean alreadyInstalled = true;
        for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
            alreadyInstalled = alreadyInstalled
                && (CldsModel.retrieve(cldsDao, buildModelName(csar, blueprint.getKey()), true).getId() != null) ? true
                : false;
        }
        return alreadyInstalled;
    }

    public static String buildModelName(CsarHandler csar, String resourceInstanceName)
        throws SdcArtifactInstallerException {
        String policyScopePrefix = searchForPolicyScopePrefix(csar.getMapOfBlueprints().get(resourceInstanceName));
        if (policyScopePrefix.contains("*")) {
            // This is policy_filter type
            policyScopePrefix = policyScopePrefix.replaceAll("\\*", "");
        } else {
            // This is normally the get_input case
            policyScopePrefix = MODEL_NAME_PREFIX;
        }
        return policyScopePrefix + csar.getSdcCsarHelper().getServiceMetadata().getValue("name") + "_v"
            + csar.getSdcNotification().getServiceVersion().replace('.', '_') + "_"
            + resourceInstanceName.replaceAll(" ", "");
    }

    @Override
    @Transactional
    public void installTheCsar(CsarHandler csar) throws SdcArtifactInstallerException, InterruptedException {
        try {
            logger.info("Installing the CSAR " + csar.getFilePath());
            for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
                logger.info("Processing blueprint " + blueprint.getValue().getBlueprintArtifactName());
                createFakeCldsModel(csar, blueprint.getValue(),
                    createFakeCldsTemplate(csar, blueprint.getValue(),
                        this.searchForRightMapping(blueprint.getValue())),
                    queryDcaeToGetServiceTypeId(blueprint.getValue()));
            }
            logger.info("Successfully installed the CSAR " + csar.getFilePath());
        } catch (IOException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Csar installation in database", e);
        } catch (ParseException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Dcae query to get ServiceTypeId", e);
        }
    }

    private BlueprintParserFilesConfiguration searchForRightMapping(BlueprintArtifact blueprintArtifact)
        throws SdcArtifactInstallerException {
        List<BlueprintParserFilesConfiguration> listConfig = new ArrayList<>();
        Yaml yaml = new Yaml();
        Map<String, Object> templateNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
            .load(blueprintArtifact.getDcaeBlueprint())).get("node_templates"));
        bpmnMapping.entrySet().forEach(e -> {
            if (templateNodes.keySet().stream().anyMatch(t -> t.contains(e.getKey()))) {
                listConfig.add(e.getValue());
            }
        });
        if (listConfig.size() > 1) {
            throw new SdcArtifactInstallerException(
                "The code does not currently support multiple MicroServices in the blueprint");
        } else if (listConfig.isEmpty()) {
            throw new SdcArtifactInstallerException("There is no recognized MicroService found in the blueprint");
        }
        logger.info("Mapping found for blueprint " + blueprintArtifact.getBlueprintArtifactName() + " is "
            + listConfig.get(0).getBpmnXmlFilePath());
        return listConfig.get(0);
    }

    String getAllBlueprintParametersInJson(BlueprintArtifact blueprintArtifact) {
        JsonObject node = new JsonObject();
        Yaml yaml = new Yaml();
        Map<String, Object> inputsNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
            .load(blueprintArtifact.getDcaeBlueprint())).get("inputs"));
        inputsNodes.entrySet().stream().filter(e -> !e.getKey().contains("policy_id")).forEach(elem -> {
            Object defaultValue = ((Map<String, Object>) elem.getValue()).get("default");
            if (defaultValue != null) {
                addPropertyToNode(node, elem.getKey(), defaultValue);
            } else {
                node.addProperty(elem.getKey(), "");
            }
        });
        node.addProperty("policy_id", "AUTO_GENERATED_POLICY_ID_AT_SUBMIT");
        return node.toString();
    }

    private static String searchForPolicyScopePrefix(BlueprintArtifact blueprintArtifact)
        throws SdcArtifactInstallerException {
        String policyName = null;
        Yaml yaml = new Yaml();
        List<String> policyNameList = new ArrayList<>();
        Map<String, Object> templateNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
            .load(blueprintArtifact.getDcaeBlueprint())).get("node_templates"));
        templateNodes.entrySet().stream().filter(e -> e.getKey().contains("policy")).forEach(ef -> {
            String filteredPolicyName = (String) ((Map<String, Object>) ((Map<String, Object>) ef.getValue())
                .get("properties")).get("policy_filter");
            if (policyName != null) {
                policyNameList.add(filteredPolicyName);
            } else {
                String inputPolicyName = (String) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ef
                    .getValue()).get("properties")).get("policy_id")).get(GET_INPUT_BLUEPRINT_PARAM);
                if (inputPolicyName != null) {
                    policyNameList.add(GET_INPUT_BLUEPRINT_PARAM);
                }
            }
        });
        if (policyNameList.size() > 1) {
            throw new SdcArtifactInstallerException(
                "The code does not currently support multiple Policy MicroServices in the blueprint");
        } else if (policyNameList.isEmpty()) {
            throw new SdcArtifactInstallerException(
                "There is no recognized Policy MicroService found in the blueprint");
        }
        logger.info("policyName found in blueprint " + blueprintArtifact.getBlueprintArtifactName() + " is "
            + policyNameList.get(0));
        return policyNameList.get(0);
    }

    /**
     * This call must be done when deploying the SDC notification as this call get the latest version of the artifact
     * (version can be specified to DCAE call)
     *
     * @return The DcaeInventoryResponse object containing the dcae values
     */
    private DcaeInventoryResponse queryDcaeToGetServiceTypeId(BlueprintArtifact blueprintArtifact)
        throws IOException, ParseException, InterruptedException {
        return dcaeInventoryService.getDcaeInformation(blueprintArtifact.getBlueprintArtifactName(),
            blueprintArtifact.getBlueprintInvariantServiceUuid(),
            blueprintArtifact.getResourceAttached().getResourceInvariantUUID());
    }

    private CldsTemplate createFakeCldsTemplate(CsarHandler csar, BlueprintArtifact blueprintArtifact,
        BlueprintParserFilesConfiguration configFiles) throws IOException, SdcArtifactInstallerException {
        CldsTemplate template = new CldsTemplate();
        template.setBpmnId("Sdc-Generated");
        template
            .setBpmnText(IOUtils.toString(appContext.getResource(configFiles.getBpmnXmlFilePath()).getInputStream()));
        template.setPropText(
            "{\"global\":[{\"name\":\"service\",\"value\":[\"" + blueprintArtifact.getDcaeBlueprint() + "\"]}]}");
        template
            .setImageText(IOUtils.toString(appContext.getResource(configFiles.getSvgXmlFilePath()).getInputStream()));
        template.setName(TEMPLATE_NAME_PREFIX
            + buildModelName(csar, blueprintArtifact.getResourceAttached().getResourceInstanceName()));
        template.save(cldsDao, null);
        logger.info("Fake Clds Template created for blueprint " + blueprintArtifact.getBlueprintArtifactName()
            + " with name " + template.getName());
        return template;
    }

    private CldsModel createFakeCldsModel(CsarHandler csar, BlueprintArtifact blueprintArtifact,
        CldsTemplate cldsTemplate, DcaeInventoryResponse dcaeInventoryResponse) throws SdcArtifactInstallerException {
        try {
            CldsModel cldsModel = new CldsModel();
            cldsModel.setName(buildModelName(csar, blueprintArtifact.getResourceAttached().getResourceInstanceName()));
            cldsModel.setBlueprintText(blueprintArtifact.getDcaeBlueprint());
            cldsModel.setTemplateName(cldsTemplate.getName());
            cldsModel.setTemplateId(cldsTemplate.getId());
            cldsModel.setBpmnText(cldsTemplate.getBpmnText());
            cldsModel.setTypeId(dcaeInventoryResponse.getTypeId());
            cldsModel.setTypeName(dcaeInventoryResponse.getTypeName());
            cldsModel.setControlNamePrefix(CONTROL_NAME_PREFIX);
            // We must save it otherwise object won't be created in db
            // and proptext will always be null
            cldsModel.setPropText("{\"global\":[]}");
            // Must save first to have the generated id available to generate
            // the policyId
            cldsModel = cldsModel.save(cldsDao, null);
            cldsModel = setModelPropText(cldsModel, blueprintArtifact, cldsTemplate);
            logger.info("Fake Clds Model created for blueprint " + blueprintArtifact.getBlueprintArtifactName()
                + " with name " + cldsModel.getName());
            return cldsModel;
        } catch (TransformerException e) {
            throw new SdcArtifactInstallerException("TransformerException when decoding the BpmnText", e);
        }
    }

    private CldsModel setModelPropText(CldsModel cldsModel, BlueprintArtifact blueprintArtifact,
        CldsTemplate cldsTemplate) throws TransformerException {
        // Do a test to validate the BPMN
        new ModelProperties(cldsModel.getName(), cldsModel.getControlName(), "PUT", false,
            cldsBpmnTransformer.doXslTransformToString(cldsTemplate.getBpmnText()), "{}");
        String inputParams = "{\"name\":\"deployParameters\",\"value\":"
            + getAllBlueprintParametersInJson(blueprintArtifact) + "}";
        cldsModel.setPropText("{\"global\":[{\"name\":\"service\",\"value\":[\""
            + blueprintArtifact.getBlueprintInvariantServiceUuid() + "\"]},{\"name\":\"vf\",\"value\":[\""
            + blueprintArtifact.getResourceAttached().getResourceInvariantUUID()
            + "\"]},{\"name\":\"actionSet\",\"value\":[\"vnfRecipe\"]},{\"name\":\"location\",\"value\":[\"DC1\"]},"
            + inputParams + "]}");
        return cldsModel.save(cldsDao, null);
    }

    private void addPropertyToNode(JsonObject node, String key, Object value) {
        if (value instanceof String) {
            node.addProperty(key, (String) value);
        } else if (value instanceof Number) {
            node.addProperty(key, (Number) value);
        } else if (value instanceof Boolean) {
            node.addProperty(key, (Boolean) value);
        } else if (value instanceof Character) {
            node.addProperty(key, (Character) value);
        } else {
            node.addProperty(key, JsonUtils.GSON.toJson(value));
        }
    }
}
