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

package org.onap.clamp.clds.workflow;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Log message. Invoked by the log-message-wf example Camunda workflow/bpmn.
 */
public class RestMessageDelegate implements JavaDelegate {
    protected static final EELFLogger       logger      = EELFManager.getInstance().getLogger(RestMessageDelegate.class);
    protected static final EELFLogger auditLogger = EELFManager.getInstance().getAuditLogger();

    /**
     * Perform activity. Log message from running process and set a variable in
     * the running process.
     *
     * @param execution
     */
    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String logMessageText = (String) execution.getVariable("logMessageText");
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> restValues = new HashMap<>();
        restValues.put("procInstId", execution.getProcessInstanceId());
        logger.info("Invoked from processDefinitionId=" + execution.getProcessDefinitionId() + ", processInstanceId="
                + execution.getProcessInstanceId() + ", activityInstanceId=" + execution.getActivityInstanceId()
                + ": logMessageText=" + logMessageText);
        // TODO: this should be fixed - put in temporary solution with existing
        // sysprop and vars - why are we calling our own service?
        String port = System.getProperty("server.port");
        ResponseEntity<String> resp = restTemplate.getForEntity(
                "http://localhost:" + port + "/services/CamundaExample/v1/jaxrsExample/log/histLog/{procInstId}",
                String.class, restValues);
        logger.info("value of resp:" + resp);
        execution.setVariable("isMessageLogComplete", true);
    }
}
