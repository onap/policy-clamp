/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2019 AT&T Intellectual Property. All rights
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
    /**
     * The constant ACTION_TEST.
     */
    public static final String ACTION_TEST = "TEST";
    /**
     * The constant ACTION_CREATE.
     */
    public static final String ACTION_CREATE = "CREATE";
    /**
     * The constant ACTION_MODIFY.
     */
    public static final String ACTION_MODIFY = "MODIFY";
    /**
     * The constant ACTION_SUBMIT.
     */
    public static final String ACTION_SUBMIT = "SUBMIT";
    /**
     * The constant ACTION_RESUBMIT.
     */
    // an update before model is active
    public static final String ACTION_RESUBMIT = "RESUBMIT";
    /**
     * The constant ACTION_SUBMITDCAE.
     */
    // For simplified models
    public static final String ACTION_SUBMITDCAE = "SUBMITDCAE";
    /**
     * The constant ACTION_SUBMITPOLICY.
     */
    public static final String ACTION_SUBMITPOLICY = "SUBMITPOLICY";
    /**
     * The constant ACTION_DISTRIBUTE.
     */
    // only from dcae
    public static final String ACTION_DISTRIBUTE = "DISTRIBUTE";
    /**
     * The constant ACTION_DEPLOY.
     */
    // only from dcae
    public static final String ACTION_DEPLOY = "DEPLOY";
    /**
     * The constant ACTION_UNDEPLOY.
     */
    // only from dcae
    public static final String ACTION_UNDEPLOY = "UNDEPLOY";
    /**
     * The constant ACTION_UPDATE.
     */
    public static final String ACTION_UPDATE = "UPDATE";
    /**
     * The constant ACTION_DELETE.
     */
    public static final String ACTION_DELETE = "DELETE";
    /**
     * The constant ACTION_STOP.
     */
    public static final String ACTION_STOP = "STOP";
    /**
     * The constant ACTION_RESTART.
     */
    public static final String ACTION_RESTART = "RESTART";

    /**
     * The constant ACTION_STATE_INITIATED.
     */
    public static final String ACTION_STATE_INITIATED = "INITIATED";
    /**
     * The constant ACTION_STATE_SENT.
     */
    public static final String ACTION_STATE_SENT = "SENT";
    /**
     * The constant ACTION_STATE_COMPLETED.
     */
    public static final String ACTION_STATE_COMPLETED = "COMPLETED";
    /**
     * The constant ACTION_STATE_RECEIVED.
     */
    public static final String ACTION_STATE_RECEIVED = "RECEIVED";
    /**
     * The constant ACTION_STATE_ERROR.
     */
    public static final String ACTION_STATE_ERROR = "ERROR";
    /**
     * The constant ACTION_STATE_ANY.
     */
    public static final String ACTION_STATE_ANY = null;

    private String id;
    private String actionCd;
    private String actionStateCd;
    private String processInstanceId;
    private String userid;

    /**
     * Gets id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Ins event clds event.
     *
     * @param cldsDao           the clds dao
     * @param controlName       the control name
     * @param userid            the userid
     * @param actionCd          the action cd
     * @param actionStateCd     the action state cd
     * @param processInstanceId the process instance id
     * @return clds event
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
     * @param cldsDao           the clds dao
     * @param model             the model
     * @param userId            the user id
     * @param actionCd          the action cd
     * @param actionStateCd     the action state cd
     * @param processInstanceId the process instance id
     * @return clds event
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
     * @param checkActionCd the check action cd
     * @return boolean
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
     * @param checkActionCd      the check action cd
     * @param checkActionStateCd the check action state cd
     * @return boolean
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
     * @param checkActionStateCd the check action state cd
     * @return boolean
     */
    public boolean isActionStateCd(String checkActionStateCd) {
        return !(checkActionStateCd == null || actionStateCd == null) && actionStateCd.equals(checkActionStateCd);
    }

    /**
     * Gets action cd.
     *
     * @return the actionCd
     */
    public String getActionCd() {
        return actionCd;
    }

    /**
     * Sets action cd.
     *
     * @param actionCd the actionCd to set
     */
    public void setActionCd(String actionCd) {
        this.actionCd = actionCd;
    }

    /**
     * Gets action state cd.
     *
     * @return the actionStateCd
     */
    public String getActionStateCd() {
        return actionStateCd;
    }

    /**
     * Sets action state cd.
     *
     * @param actionStateCd the actionStateCd to set
     */
    public void setActionStateCd(String actionStateCd) {
        this.actionStateCd = actionStateCd;
    }

    /**
     * Gets process instance id.
     *
     * @return the processInstanceId
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * Sets process instance id.
     *
     * @param processInstanceId the processInstanceId to set
     */
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * Gets userid.
     *
     * @return the userid
     */
    public String getUserid() {
        return userid;
    }

    /**
     * Sets userid.
     *
     * @param userid the userid to set
     */
    public void setUserid(String userid) {
        this.userid = userid;
    }
}
