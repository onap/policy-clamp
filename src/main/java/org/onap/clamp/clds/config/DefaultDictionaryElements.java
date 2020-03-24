/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.config;

import javax.annotation.PostConstruct;
import org.onap.clamp.tosca.Dictionary;
import org.onap.clamp.tosca.DictionaryElement;
import org.onap.clamp.tosca.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("default-dictionary-elements")
public class DefaultDictionaryElements {

    @Autowired
    private DictionaryService dictionaryService;

    /**
     * Init method.
     */
    @PostConstruct
    public void init() {
        preProvisionDefaultActors();
        preProvisionDefaultOperations();
    }

    private void preProvisionDefaultActors() {
        // Set up dictionary elements
        Dictionary actorDictionary = new Dictionary();
        actorDictionary.setName("DefaultActors");
        actorDictionary.setSecondLevelDictionary(0);
        actorDictionary.setSubDictionaryType("");

        DictionaryElement elementSo = new DictionaryElement();
        elementSo.setName("SO");
        elementSo.setShortName("SO");
        elementSo.setType("string");
        elementSo.setDescription("SO component");
        actorDictionary.addDictionaryElements(elementSo);

        DictionaryElement elementSdnc = new DictionaryElement();
        elementSdnc.setName("SDNC");
        elementSdnc.setShortName("SDNC");
        elementSdnc.setType("string");
        elementSdnc.setDescription("SDNC component");
        actorDictionary.addDictionaryElements(elementSdnc);

        DictionaryElement elementAppc = new DictionaryElement();
        elementAppc.setName("APPC");
        elementAppc.setShortName("APPC");
        elementAppc.setType("string");
        elementAppc.setDescription("APPC component");
        actorDictionary.addDictionaryElements(elementAppc);

        DictionaryElement elementVfc = new DictionaryElement();
        elementVfc.setName("VFC");
        elementVfc.setShortName("VFC");
        elementVfc.setType("string");
        elementVfc.setDescription("VFC component");
        actorDictionary.addDictionaryElements(elementVfc);

        DictionaryElement elementSdnr = new DictionaryElement();
        elementSdnr.setName("SDNR");
        elementSdnr.setShortName("SDNR");
        elementSdnr.setType("string");
        elementSdnr.setDescription("SDNR component");
        actorDictionary.addDictionaryElements(elementSdnr);

        dictionaryService.saveOrUpdateDictionary(actorDictionary);
    }

    private void preProvisionDefaultOperations() {
        // Set up dictionary elements
        Dictionary operationDictionary = new Dictionary();
        operationDictionary.setName("DefaultOperations");
        operationDictionary.setSecondLevelDictionary(0);
        operationDictionary.setSubDictionaryType("");

        DictionaryElement elementRestart = new DictionaryElement();
        elementRestart.setName("Restart");
        elementRestart.setShortName("Restart (APPC operation)");
        elementRestart.setType("string");
        elementRestart.setDescription("APPC operation");
        operationDictionary.addDictionaryElements(elementRestart);

        DictionaryElement elementRebuild = new DictionaryElement();
        elementRebuild.setName("Rebuild");
        elementRebuild.setShortName("Rebuild (APPC operation)");
        elementRebuild.setType("string");
        elementRebuild.setDescription("APPC operation");
        operationDictionary.addDictionaryElements(elementRebuild);

        DictionaryElement elementMigrate = new DictionaryElement();
        elementMigrate.setName("Migrate");
        elementMigrate.setShortName("Migrate (APPC operation)");
        elementMigrate.setType("string");
        elementMigrate.setDescription("APPC operation");
        operationDictionary.addDictionaryElements(elementMigrate);

        DictionaryElement elementHealthCheck = new DictionaryElement();
        elementHealthCheck.setName("Health-Check");
        elementHealthCheck.setShortName("Health-Check (APPC operation)");
        elementHealthCheck.setType("string");
        elementHealthCheck.setDescription("APPC operation");
        operationDictionary.addDictionaryElements(elementHealthCheck);

        DictionaryElement elementModifyConfig = new DictionaryElement();
        elementModifyConfig.setName("ModifyConfig");
        elementModifyConfig.setShortName("ModifyConfig (APPC/VFC operation)");
        elementModifyConfig.setType("string");
        elementModifyConfig.setDescription("APPC/VFC operation");
        operationDictionary.addDictionaryElements(elementModifyConfig);

        DictionaryElement elementVfModuleCreate = new DictionaryElement();
        elementVfModuleCreate.setName("VF Module Create");
        elementVfModuleCreate.setShortName("VF Module Create (SO operation)");
        elementVfModuleCreate.setType("string");
        elementVfModuleCreate.setDescription("SO operation");
        operationDictionary.addDictionaryElements(elementVfModuleCreate);

        DictionaryElement elementVfModuleDelete = new DictionaryElement();
        elementVfModuleDelete.setName("VF Module Delete");
        elementVfModuleDelete.setShortName("VF Module Delete (SO operation)");
        elementVfModuleDelete.setType("string");
        elementVfModuleDelete.setDescription("SO operation");
        operationDictionary.addDictionaryElements(elementVfModuleDelete);

        DictionaryElement elementReroute = new DictionaryElement();
        elementReroute.setName("Reroute");
        elementReroute.setShortName("Reroute (SDNC operation)");
        elementReroute.setType("string");
        elementReroute.setDescription("SDNC operation");
        operationDictionary.addDictionaryElements(elementReroute);

        DictionaryElement elementBandwidthOnDemand = new DictionaryElement();
        elementBandwidthOnDemand.setName("BandwidthOnDemand");
        elementBandwidthOnDemand.setShortName("BandwidthOnDemand (SDNC operation)");
        elementBandwidthOnDemand.setType("string");
        elementBandwidthOnDemand.setDescription("SDNC operation");
        operationDictionary.addDictionaryElements(elementBandwidthOnDemand);

        dictionaryService.saveOrUpdateDictionary(operationDictionary);
    }
}
