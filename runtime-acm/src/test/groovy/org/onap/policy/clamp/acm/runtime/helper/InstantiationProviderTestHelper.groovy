/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */
package org.onap.policy.clamp.acm.runtime.helper

import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils
import org.onap.policy.clamp.acm.runtime.main.parameters.AcRuntimeParameterGroup
import org.onap.policy.clamp.acm.runtime.main.utils.EncryptionUtils
import org.onap.policy.clamp.acm.runtime.supervision.SupervisionAcHandler
import org.onap.policy.clamp.acm.runtime.util.CommonTestData
import org.onap.policy.clamp.models.acm.concepts.AcTypeState
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition
import org.onap.policy.clamp.models.acm.document.concepts.DocToscaServiceTemplate
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.AcInstanceStateResolver
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider
import org.onap.policy.clamp.models.acm.persistence.provider.ProviderUtils
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate

class InstantiationProviderTestHelper {

    static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json"
    static final String AC_INSTANTIATION_UPDATE_JSON = "src/test/resources/rest/acm/AutomationCompositionUpdate.json"
    static final String AC_MIGRATE_JSON = "src/test/resources/rest/acm/AutomationCompositionMigrate.json"
    static final String AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionElementsNotFound.json"
    static final String AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/acm/AutomationCompositionNotFound.json"
    static final String MIGRATION_SERVICE_TEMPLATE_YAML = "clamp/acm/pmsh/funtional-pmsh-usecase-migration.yaml"

    static final Map<String, String> RESOURCE_PATHS = [
            acCreate        : AC_INSTANTIATION_CREATE_JSON,
            acUpdate        : AC_INSTANTIATION_UPDATE_JSON,
            acMigrate       : AC_MIGRATE_JSON,
            acElementNotFound: AC_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON,
            acDefNotFound   : AC_INSTANTIATION_AC_DEFINITION_NOT_FOUND_JSON
    ]

    static final Map<String, String> ERROR_MESSAGES = [
            elementIdNotPresent       : "Element id not present ",
            notAllowedUpdateDeploying : "Not allowed to UPDATE in the state DEPLOYING",
            notAllowedMigrateUpdating : "Not allowed to MIGRATE in the state UPDATING",
            notAllowedNoneDeploying   : "Not allowed to NONE in the state DEPLOYING",
            doNotMatch                : " do not match with ",
            alreadyDefined            : "already defined",
            notFound                  : "Not found",
            notPrimed                 : "not primed",
            notValidOrderDelete       : "Not valid order DELETE;"
    ]

    final ToscaServiceTemplate serviceTemplate = toAuthorative(CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML)
    final ToscaServiceTemplate serviceTemplateMigration = toAuthorative(MIGRATION_SERVICE_TEMPLATE_YAML)

    AutomationCompositionProvider acProvider
    AcDefinitionProvider acDefinitionProvider
    SupervisionAcHandler supervisionAcHandler
    ParticipantProvider participantProvider

    static def getResourcePath(String key) {
        return RESOURCE_PATHS[key]
    }

    static def getErrorMessage(String key) {
        return ERROR_MESSAGES[key]
    }

    static def toAuthorative(String path) {
        def st = InstantiationUtils.getToscaServiceTemplate(path)
        return ProviderUtils.getJpaAndValidate(st, DocToscaServiceTemplate::new, "toscaServiceTemplate").toAuthorative()
    }

    def createProvider(
            AcRuntimeParameterGroup params = CommonTestData.getTestParamaterGroup(),
            EncryptionUtils encryption = new EncryptionUtils(CommonTestData.getTestParamaterGroup())) {
        return new AutomationCompositionInstantiationProvider(acProvider, acDefinitionProvider,
                new AcInstanceStateResolver(), supervisionAcHandler, participantProvider, params, encryption)
    }

    def createPrimedDefinition(ToscaServiceTemplate template = serviceTemplate) {
        return CommonTestData.createAcDefinition(template, AcTypeState.PRIMED)
    }

    static def loadAc(String resourceKey, String suffix) {
        return InstantiationUtils.getAutomationCompositionFromResource(getResourcePath(resourceKey), suffix)
    }

    static def loadCustomAc(String resourceKey, String suffix, deployState, UUID compositionId) {
        return InstantiationUtils.getCustomAutomationComposition(getResourcePath(resourceKey), suffix, deployState, compositionId)
    }

    def createDeleteProvider(AutomationComposition ac, deployState, lockState,
                             AutomationCompositionProvider localAcProvider,
                             AcDefinitionProvider localAcDefProvider,
                             ParticipantProvider localParticipantProvider,
                             AcRuntimeParameterGroup localParams) {
        ac.deployState = deployState
        ac.lockState = lockState

        def acDef = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED)
        ac.compositionId = acDef.compositionId

        def provider = new AutomationCompositionInstantiationProvider(localAcProvider, localAcDefProvider,
                new AcInstanceStateResolver(), null, localParticipantProvider, localParams, null)

        return [acDef: acDef, provider: provider]
    }
}
