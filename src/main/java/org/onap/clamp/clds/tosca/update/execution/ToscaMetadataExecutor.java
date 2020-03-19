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

package org.onap.clamp.clds.tosca.update.execution;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.onap.clamp.clds.tosca.update.execution.cds.ToscaMetadataCdsProcess;
import org.onap.clamp.clds.tosca.update.execution.target.ToscaMetadataTargetProcess;
import org.onap.clamp.loop.service.Service;
import org.onap.clamp.tosca.Dictionary;
import org.onap.clamp.tosca.DictionaryElement;
import org.onap.clamp.tosca.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used to execute a code based on a key found in the metadata section.
 */
@Component
public class ToscaMetadataExecutor {

    private static final EELFLogger logger =
            EELFManager.getInstance().getLogger(ToscaMetadataExecutor.class);

    @Autowired
    private DictionaryService dictionaryService;

    private Map<String, ToscaMetadataProcess> mapOfProcesses = new HashMap<>();

    /**
     * This method executes the required process specified in processInfo
     *
     * @param processInfo  A String containing the process to execute, like "cds/param1:value1/param2:value2"
     * @param childObject  The jsonObject
     * @param serviceModel The service model associated to do clamp enrichment
     */
    public void executeTheProcess(String processInfo, JsonObject childObject, Service serviceModel) {
        String[] processParameters = (processInfo + "/ ").split("/");
        logger.info("Executing the Tosca clamp process " + processParameters[0] + " with parameters "
                + processParameters[1].trim());
        mapOfProcesses.get(processParameters[0]).executeProcess(processParameters[1].trim(), childObject, serviceModel);
    }

    /**
     * Init method.
     */
    @PostConstruct
    public void init() {
        mapOfProcesses.put("CDS", new ToscaMetadataCdsProcess());
        mapOfProcesses.put("CSAR_RESOURCES", new ToscaMetadataTargetProcess());

        preProvisionDictionaryTable();
    }

    private void preProvisionDictionaryTable() {
        // Set up dictionary elements
        Dictionary actorDictionary = new Dictionary();
        actorDictionary.setName("DefaultActor");
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

        DictionaryElement elementCds = new DictionaryElement();
        elementCds.setName("CDS");
        elementCds.setShortName("CDS");
        elementCds.setType("string");
        elementCds.setDescription("CDS component");
        actorDictionary.addDictionaryElements(elementCds);

        dictionaryService.saveOrUpdateDictionary(actorDictionary);
   }
}
