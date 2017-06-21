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

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

/**
 * Log message.
 * Invoked by the log-message-wf example Camunda workflow/bpmn.
 */
public class LogMessageDelegate implements JavaDelegate {

    /**
     * Perform activity.  Log message from running process and set a variable in the running process.
     *
     * @param execution
     */
    public void execute(DelegateExecution execution) throws Exception {
        String logMessageText = (String) execution.getVariable("logMessageText");

        System.out.println("Invoked from processDefinitionId=" + execution.getProcessDefinitionId() + ", processInstanceId=" + execution.getProcessInstanceId() + ", activityInstanceId=" + execution.getActivityInstanceId() + ": logMessageText=" + logMessageText);
        execution.setVariable("isMessageLogComplete", true);
    }
}
