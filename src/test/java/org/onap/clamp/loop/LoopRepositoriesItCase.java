/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
 * ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.loop.log.LogType;
import org.onap.clamp.loop.log.LoopLog;
import org.onap.clamp.loop.log.LoopLogRepository;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.loop.service.ServicesRepository;
import org.onap.clamp.loop.template.LoopElementModel;
import org.onap.clamp.loop.template.LoopElementModelsRepository;
import org.onap.clamp.loop.template.LoopTemplate;
import org.onap.clamp.loop.template.LoopTemplatesRepository;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelId;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.microservice.MicroServicePolicyService;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.onap.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopRepositoriesItCase {

    private Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create();

    @Autowired
    private LoopsRepository loopRepository;

    @Autowired
    private MicroServicePolicyService microServicePolicyService;

    @Autowired
    private OperationalPolicyService operationalPolicyService;

    @Autowired
    private LoopLogRepository loopLogRepository;

    @Autowired
    private LoopTemplatesRepository loopTemplateRepository;

    @Autowired
    private LoopElementModelsRepository microServiceModelsRepository;

    @Autowired
    private PolicyModelsRepository policyModelsRepository;

    @Autowired
    private ServicesRepository servicesRepository;

    private Service getService(String serviceDetails, String resourceDetails) {
        return new Service(serviceDetails, resourceDetails);
    }

    private OperationalPolicy getOperationalPolicy(String configJson, String name, PolicyModel policyModel) {
        return new OperationalPolicy(name, null, new Gson().fromJson(configJson, JsonObject.class), policyModel);
    }

    private LoopElementModel getLoopElementModel(String yaml, String name, String policyType, String createdBy,
                                                 PolicyModel policyModel) {
        LoopElementModel model = new LoopElementModel(name, policyType, yaml);
        model.addPolicyModel(policyModel);
        return model;
    }

    private PolicyModel getPolicyModel(String policyType, String policyModelTosca, String version,
                                       String policyAcronym) {
        return new PolicyModel(policyType, policyModelTosca, version, policyAcronym);
    }

    private LoopTemplate getLoopTemplate(String name, String blueprint, String svgRepresentation, String createdBy,
                                         Integer maxInstancesAllowed) {
        LoopTemplate template = new LoopTemplate(name, blueprint, svgRepresentation, maxInstancesAllowed, null);
        template.addLoopElementModel(getLoopElementModel("yaml", "microService1", "org.onap.policy.drools", createdBy,
                getPolicyModel("org.onap.policy.drools", "yaml", "1.0.0", "Drools")));
        loopTemplateRepository.save(template);
        return template;
    }

    private Loop getLoop(String name, String svgRepresentation, String blueprint, String globalPropertiesJson,
                         String dcaeId, String dcaeUrl, String dcaeBlueprintId) {
        Loop loop = new Loop();
        loop.setName(name);
        loop.setSvgRepresentation(svgRepresentation);
        loop.setGlobalPropertiesJson(new Gson().fromJson(globalPropertiesJson, JsonObject.class));
        loop.setLastComputedState(LoopState.DESIGN);
        loop.setDcaeDeploymentId(dcaeId);
        loop.setDcaeDeploymentStatusUrl(dcaeUrl);
        loop.setLoopTemplate(getLoopTemplate("templateName", "yaml", "svg", "toto", 1));
        return loop;
    }

    private MicroServicePolicy getMicroServicePolicy(String name, String jsonRepresentation, String jsonProperties,
                                                     boolean shared, PolicyModel policyModel) {
        MicroServicePolicy microService = new MicroServicePolicy(name, policyModel, shared,
                gson.fromJson(jsonRepresentation, JsonObject.class), new HashSet<>());
        microService.setConfigurationsJson(new Gson().fromJson(jsonProperties, JsonObject.class));
        return microService;
    }

    private LoopLog getLoopLog(LogType type, String message, Loop loop) {
        return new LoopLog(message, type, "CLAMP", loop);
    }

    /**
     * This method does a crud test and save a loop template and a loop object in db.
     */
    @Test
    @Transactional
    public void crudTest() {
        // Setup
        Loop loopTest = getLoop("ControlLoopTest", "<xml></xml>", "yamlcontent", "{\"testname\":\"testvalue\"}",
                "123456789", "https://dcaetest.org", "UUID-blueprint");
        OperationalPolicy opPolicy = this.getOperationalPolicy("{\"type\":\"GUARD\"}", "GuardOpPolicyTest",
                getPolicyModel("org.onap.policy.drools", "yaml", "1.0.0", "Drools"));
        loopTest.addOperationalPolicy(opPolicy);
        MicroServicePolicy microServicePolicy = getMicroServicePolicy("configPolicyTest", "{\"configtype\":\"json\"}",
                "{\"param1\":\"value1\"}", true, getPolicyModel("org.onap.policy.drools", "yaml", "1.0.0", "Drools"));
        loopTest.addMicroServicePolicy(microServicePolicy);
        LoopLog loopLog = getLoopLog(LogType.INFO, "test message", loopTest);
        loopTest.addLog(loopLog);
        Service service = getService(
                "{\"name\": \"vLoadBalancerMS\", \"UUID\": \"63cac700-ab9a-4115-a74f-7eac85e3fce0\"}", "{\"CP\": {}}");
        loopTest.setModelService(service);

        // Attempt to save into the database the entire loop
        Loop loopInDb = loopRepository.save(loopTest);
        assertThat(loopInDb).isNotNull();
        assertThat(loopRepository.findById(loopInDb.getName()).get()).isNotNull();
        assertThat(loopInDb.getCreatedDate()).isNotNull();
        assertThat(loopInDb.getUpdatedDate()).isNotNull();
        assertThat(loopInDb.getUpdatedDate()).isEqualTo(loopInDb.getCreatedDate());
        assertThat(loopInDb.getName()).isEqualTo("ControlLoopTest");
        // Autogen id so now set the ID in the previous model so that we can compare the
        // objects
        loopLog.setId(((LoopLog) loopInDb.getLoopLogs().toArray()[0]).getId());

        assertThat(loopInDb).isEqualToIgnoringGivenFields(loopTest, "components", "createdDate", "updatedDate",
                "createdBy", "updatedBy");
        assertThat(loopRepository.existsById(loopTest.getName())).isEqualTo(true);
        assertThat(operationalPolicyService.isExisting(opPolicy.getName())).isEqualTo(true);
        assertThat(microServicePolicyService.isExisting(microServicePolicy.getName())).isEqualTo(true);
        assertThat(loopLogRepository.existsById(loopLog.getId())).isEqualTo(true);
        assertThat(loopTemplateRepository.existsById(loopInDb.getLoopTemplate().getName())).isEqualTo(true);
        assertThat(loopTemplateRepository.existsById(loopInDb.getLoopTemplate().getName())).isEqualTo(true);
        assertThat(servicesRepository.existsById(loopInDb.getModelService().getServiceUuid())).isEqualTo(true);
        assertThat(microServiceModelsRepository.existsById(
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getName()))
                .isEqualTo(true);
        assertThat(policyModelsRepository.existsById(new PolicyModelId(
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getPolicyModels()
                        .first().getPolicyModelType(),
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getPolicyModels()
                        .first().getVersion()))).isEqualTo(true);

        // Now attempt to read from database
        Loop loopInDbRetrieved = loopRepository.findById(loopTest.getName()).get();
        assertThat(loopInDbRetrieved).isEqualToIgnoringGivenFields(loopTest, "components", "createdDate", "updatedDate",
                "createdBy", "updatedBy");
        assertThat(loopInDbRetrieved).isEqualToComparingOnlyGivenFields(loopInDb, "createdDate", "updatedDate",
                "createdBy", "updatedBy");
        assertThat((LoopLog) loopInDbRetrieved.getLoopLogs().toArray()[0]).isEqualToComparingFieldByField(loopLog);
        assertThat((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0])
                .isEqualToIgnoringGivenFields(opPolicy, "createdDate", "updatedDate", "createdBy", "updatedBy");
        assertThat(((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0]).getCreatedDate())
                .isNotNull();
        assertThat(((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0]).getUpdatedDate())
                .isNotNull();
        assertThat(((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0]).getCreatedBy())
                .isNotNull();
        assertThat(((OperationalPolicy) loopInDbRetrieved.getOperationalPolicies().toArray()[0]).getUpdatedBy())
                .isNotNull();

        assertThat((MicroServicePolicy) loopInDbRetrieved.getMicroServicePolicies().toArray()[0])
                .isEqualToIgnoringGivenFields(microServicePolicy, "createdDate", "updatedDate", "createdBy",
                        "updatedBy");

        // Attempt an update
        ((LoopLog) loopInDbRetrieved.getLoopLogs().toArray()[0]).setLogInstant(Instant.now());
        loopInDbRetrieved.setSvgRepresentation("");
        Loop loopInDbRetrievedUpdated = loopRepository.saveAndFlush(loopInDbRetrieved);
        // Loop loopInDbRetrievedUpdated =
        // loopRepository.findById(loopTest.getName()).get();
        assertThat((LoopLog) loopInDbRetrievedUpdated.getLoopLogs().toArray()[0])
                .isEqualToComparingFieldByField(loopInDbRetrieved.getLoopLogs().toArray()[0]);
        // UpdatedDate should have been changed
        assertThat(loopInDbRetrievedUpdated.getUpdatedDate()).isNotEqualTo(loopInDbRetrievedUpdated.getCreatedDate());
        // createdDate should have NOT been changed
        assertThat(loopInDbRetrievedUpdated.getCreatedDate()).isEqualTo(loopInDb.getCreatedDate());
        // other audit are the same
        assertThat(loopInDbRetrievedUpdated.getCreatedBy()).isEqualTo("");
        assertThat(loopInDbRetrievedUpdated.getUpdatedBy()).isEqualTo("");

        // Attempt to delete the object and check it has well been cascaded

        loopRepository.delete(loopInDbRetrieved);
        assertThat(loopRepository.existsById(loopTest.getName())).isEqualTo(false);
        assertThat(operationalPolicyService.isExisting(opPolicy.getName())).isEqualTo(false);
        assertThat(microServicePolicyService.isExisting(microServicePolicy.getName())).isEqualTo(true);
        assertThat(loopLogRepository.existsById(loopLog.getId())).isEqualTo(false);
        assertThat(loopTemplateRepository.existsById(loopInDb.getLoopTemplate().getName())).isEqualTo(true);
        assertThat(servicesRepository.existsById(loopInDb.getModelService().getServiceUuid())).isEqualTo(true);
        assertThat(microServiceModelsRepository.existsById(
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getName()))
                .isEqualTo(true);

        assertThat(policyModelsRepository.existsById(new PolicyModelId(
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getPolicyModels()
                        .first().getPolicyModelType(),
                loopInDb.getLoopTemplate().getLoopElementModelsUsed().first().getLoopElementModel().getPolicyModels()
                        .first().getVersion()))).isEqualTo(true);

    }
}
