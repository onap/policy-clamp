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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.sdc.controller.installer;

import com.att.aft.dme2.internal.apache.commons.io.IOUtils;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.json.simple.parser.ParseException;
import org.onap.clamp.clds.client.DcaeInventoryServices;
import org.onap.clamp.clds.config.sdc.BlueprintParserFilesConfiguration;
import org.onap.clamp.clds.config.sdc.BlueprintParserMappingConfiguration;
import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.model.CldsModel;
import org.onap.clamp.clds.model.CldsTemplate;
import org.onap.clamp.clds.service.CldsService;
import org.onap.clamp.clds.service.CldsTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
public class CsarInstallerImpl implements CsarInstaller {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarInstallerImpl.class);
    private Map<String, BlueprintParserFilesConfiguration> bpmnMapping = new HashMap<>();
    public static final String TEMPLATE_NAME_PREFIX = "DCAE-Designer-ClosedLoopTemplate-";
    public static final String MODEL_NAME_PREFIX = "DCAE-Designer-ClosedLoopInstance-";
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

    @PostConstruct
    public void loadConfiguration() throws IOException {
        BlueprintParserMappingConfiguration
                .createFromJson(appContext.getResource(blueprintMappingFile).getInputStream()).stream()
                .forEach(e -> bpmnMapping.put(e.getBlueprintKey(), e.getFiles()));
    }

    @Override
    public boolean isCsarAlreadyDeployed(CsarHandler csar) throws SdcArtifactInstallerException {
        return false;
    }

    @Override
    public void installTheCsar(CsarHandler csar) throws SdcArtifactInstallerException {
        try {
            String serviceTypeId = queryDcaeToGetServiceTypeId(csar);
            String policyName = searchForPolicyName(csar);
            if (policyName.contains("*")) {
            }
            createFakeCldsModel(csar, createFakeCldsTemplate(csar, this.searchForRightMapping(csar)), serviceTypeId);
        } catch (IOException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Csar installation in database", e);
        } catch (ParseException e) {
            throw new SdcArtifactInstallerException("Exception caught during the Dcae query to get ServiceTypeId", e);
        }
    }

    private BlueprintParserFilesConfiguration searchForRightMapping(CsarHandler csar)
            throws SdcArtifactInstallerException {
        List<BlueprintParserFilesConfiguration> listConfig = new ArrayList<>();
        Yaml yaml = new Yaml();
        Map<String, Object> templateNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
                .load(csar.getDcaeBlueprint())).get("node_templates"));
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
        return listConfig.get(0);
    }

    private String searchForPolicyName(CsarHandler csar) throws SdcArtifactInstallerException {
        String policyName = null;
        Yaml yaml = new Yaml();
        List<String> policyNameList = new ArrayList<>();
        Map<String, Object> templateNodes = ((Map<String, Object>) ((Map<String, Object>) yaml
                .load(csar.getDcaeBlueprint())).get("node_templates"));
        templateNodes.entrySet().stream().filter(e -> e.getKey().contains("policy_")).forEach(ef -> {
            String filteredPolicyName = (String) ((Map<String, Object>) ((Map<String, Object>) ef.getValue())
                    .get("properties")).get("policy_filter");
            if (policyName != null) {
                policyNameList.add(filteredPolicyName);
            } else {
                String inputPolicyName = (String) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ef
                        .getValue()).get("properties")).get("policy_id")).get("get_input");
                if (inputPolicyName != null) {
                    policyNameList.add("get_input");
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
        return policyNameList.get(0);
    }

    private String queryDcaeToGetServiceTypeId(CsarHandler csar) throws IOException, ParseException {
        return dcaeInventoryService.getDcaeInformation(csar.getBlueprintArtifactName(),
                csar.getBlueprintInvariantServiceUuid(), csar.getBlueprintInvariantResourceUuid());
    }

    private CldsTemplate createFakeCldsTemplate(CsarHandler csar, BlueprintParserFilesConfiguration configFiles)
            throws IOException, SdcArtifactInstallerException {
        CldsTemplate template = new CldsTemplate();
        template.setBpmnId("Sdc-Generated");
        template.setBpmnText(
                IOUtils.toString(appContext.getResource(configFiles.getBpmnXmlFilePath()).getInputStream()));
        template.setPropText(csar.getDcaeBlueprint());
        template.setImageText(
                IOUtils.toString(appContext.getResource(configFiles.getSvgXmlFilePath()).getInputStream()));
        template.setName(TEMPLATE_NAME_PREFIX + csar.getSdcCsarHelper().getServiceMetadata().getValue("name"));
        template.save(cldsDao, null);
        return template;
    }

    private CldsModel createFakeCldsModel(CsarHandler csar, CldsTemplate cldsTemplate, String serviceTypeId) {
        CldsModel cldsModel = new CldsModel();
        cldsModel.setControlNamePrefix(MODEL_NAME_PREFIX);
        cldsModel.setName(csar.getSdcCsarHelper().getServiceMetadata().getValue("name"));
        cldsModel.setBlueprintText(csar.getDcaeBlueprint());
        cldsModel.setTemplateName(cldsTemplate.getName());
        cldsModel.setTemplateId(cldsTemplate.getId());
        cldsModel.setDocText(cldsTemplate.getPropText());
        cldsModel.setPropText("{}");
        cldsModel.setBpmnText(cldsTemplate.getBpmnText());
        cldsModel.setTypeId(serviceTypeId);
        cldsModel.save(cldsDao, null);
        return cldsModel;
    }
}
