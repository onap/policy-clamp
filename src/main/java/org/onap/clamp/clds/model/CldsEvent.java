/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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

package org.onap.clamp.clds.model;

import org.onap.clamp.clds.dao.CldsDao;

/**
 * Represent a CLDS Event.
 */
public class CldsEvent {
    public static final String ACTION_TEST = "TEST";
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_SUBMIT = "SUBMIT";
    // an update before model is active
    public static final String ACTION_RESUBMIT = "RESUBMIT";
    // For simplified models
    public static final String ACTION_SUBMITDCAE = "SUBMITDCAE";
    public static final String ACTION_SUBMITPOLICY = "SUBMITPOLICY";
    // only from dcae
    public static final String ACTION_DISTRIBUTE = "DISTRIBUTE";
    // only from dcae
    public static final String ACTION_DEPLOY = "DEPLOY";
    // only from dcae
    public static final String ACTION_UNDEPLOY = "UNDEPLOY";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_RESTART = "RESTART";

    public static final String ACTION_STATE_INITIATED = "INITIATED";
    public static final String ACTION_STATE_SENT = "SENT";
    public static final String ACTION_STATE_COMPLETED = "COMPLETED";
    public static final String ACTION_STATE_RECEIVED = "RECEIVED";
    public static final String ACTION_STATE_ERROR = "ERROR";
    public static final String ACTION_STATE_ANY = null;

    private String id;
    private String actionCd;
    private String actionStateCd;
    private String processInstanceId;
    private String userid;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param cldsDao
     * @param controlName
     * @param userid
     * @param actionCd
     * @param actionStateCd
     * @param processInstanceId
     * @return
     */
    public static CldsEvent insEvent(CldsDao cldsDao, String controlName, String userid, String actionCd,
        String actionStateCd, String processInstanceId) {
        CldsModel model = CldsModel.createUsingControlName(controlName);
        return insEvent(cldsDao, model, userid, actionCd, actionStateCd, processInstanceId);
    }

    /**
     * Insert event using controlNameUuid to find the model. This method meant for
     * processing events from dcae.
     *
     * @param cldsDao
     * @param model
     * @param userId
     * @param actionCd
     * @param actionStateCd
     * @param processInstanceId
     * @return
     */
    public static CldsEvent insEvent(CldsDao cldsDao, CldsModel model, String userId, String actionCd,
        String actionStateCd, String processInstanceId) {
        CldsEvent event = new CldsEvent();
        event.setUserid(userId);
        event.setActionCd(actionCd);
        event.setActionStateCd(actionStateCd);
        event.setProcessInstanceId(processInstanceId);
        cldsDao.insEvent(null, model.getControlNamePrefix(), model.getControlNameUuid(), event);
        return event;
    }

    /**
     * Check if actionCd is equal to the supplied checkActionCd checkActionCd should
     * not be null.
     *
     * @param checkActionCd
     * @return
     */
    public boolean isActionCd(String checkActionCd) {
        if (actionCd == null) {
            return false;
        }
        return actionCd.equals(checkActionCd);
    }

    /**
     * Check if actionCd and actionStateCd are equal to the supplied checkActionCd
     * and checkActionStateCd. Treat checkActionStateCd == null as a wildcard
     * checkActionCd should not be null.
     *
     * @param checkActionCd
     * @param checkActionStateCd
     * @return
     */
    public boolean isActionAndStateCd(String checkActionCd, String checkActionStateCd) {
        if (actionCd == null) {
            return false;
        }
        // treat checkActionStateCd == null as a wildcard (same for
        // actionStateCd, although it shouln't be null...)
        if (checkActionStateCd == null || actionStateCd == null) {
            return actionCd.equals(checkActionCd);
        }
        return actionCd.equals(checkActionCd) && actionStateCd.equals(checkActionStateCd);
    }

    /**
     * Check if actionStateCd is equal to the supplied checkActionStateCd.
     * checkActionCd should not be null.
     *
     * @param checkActionStateCd
     * @return
     */
    public boolean isActionStateCd(String checkActionStateCd) {
        return !(checkActionStateCd == null || actionStateCd == null) && actionStateCd.equals(checkActionStateCd);
    }

    /**
     * @return the actionCd
     */
    public String getActionCd() {
        return actionCd;
    }

    /**
     * @param actionCd
     *        the actionCd to set
     */
    public void setActionCd(String actionCd) {
        this.actionCd = actionCd;
    }

    /**
     * @return the actionStateCd
     */
    public String getActionStateCd() {
        return actionStateCd;
    }

    /**
     * @param actionStateCd
     *        the actionStateCd to set
     */
    public void setActionStateCd(String actionStateCd) {
        this.actionStateCd = actionStateCd;
    }

    /**
     * @return the processInstanceId
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * @param processInstanceId
     *        the processInstanceId to set
     */
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * @return the userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     * @param userid
     *        the userid to set
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }
}
