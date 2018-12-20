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

package org.onap.clamp.clds.model.status;

import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.CldsModel;

/**
 * This interface describe the handler of the status in CldsModel, it's normally
 * based on the last event stored in db.
 *
 */
public interface StatusHandler {

    /**
     * This method determines the status of the closed loop based on the last event.
     * 
     * @param event
     *        The last event
     * @return The status
     */
    default String determineStatusOnLastEvent(CldsEvent event) {
        String status = CldsModel.STATUS_UNKNOWN;
        if (event == null || event.getActionCd() == null) {
            status = CldsModel.STATUS_DESIGN;
        } else if (event.isActionStateCd(CldsEvent.ACTION_STATE_ERROR)) {
            status = CldsModel.STATUS_ERROR;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_CREATE, CldsEvent.ACTION_STATE_ANY)) {
            status = CldsModel.STATUS_DESIGN;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DISTRIBUTE, CldsEvent.ACTION_STATE_RECEIVED)
            || event.isActionAndStateCd(CldsEvent.ACTION_UNDEPLOY, CldsEvent.ACTION_STATE_COMPLETED)
            || event.isActionAndStateCd(CldsEvent.ACTION_SUBMIT, CldsEvent.ACTION_STATE_COMPLETED)
            || event.isActionAndStateCd(CldsEvent.ACTION_RESUBMIT, CldsEvent.ACTION_STATE_COMPLETED)) {
            status = CldsModel.STATUS_DISTRIBUTED;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DELETE, CldsEvent.ACTION_STATE_SENT)) {
            status = CldsModel.STATUS_DELETING;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_STATE_COMPLETED)
            || event.isActionAndStateCd(CldsEvent.ACTION_RESTART, CldsEvent.ACTION_STATE_COMPLETED)
            || event.isActionAndStateCd(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STATE_COMPLETED)
            || event.isActionAndStateCd(CldsEvent.ACTION_SUBMITPOLICY, CldsEvent.ACTION_STATE_ANY)) {
            status = CldsModel.STATUS_ACTIVE;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_STOP, CldsEvent.ACTION_STATE_COMPLETED)) {
            status = CldsModel.STATUS_STOPPED;
        }
        return status;
    }
}
