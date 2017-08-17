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

package org.onap.clamp.clds.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.NotFoundException;

import org.jboss.resteasy.spi.BadRequestException;
import org.onap.clamp.clds.dao.CldsDao;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

/**
 * Represent a CLDS Model.
 */
public class CldsModel {
    protected static final EELFLogger logger             = EELFManager.getInstance().getLogger(CldsModel.class);
    protected static final EELFLogger metricsLogger      = EELFManager.getInstance().getMetricsLogger();

    private static final int        UUID_LENGTH        = 36;

    public static final String      STATUS_DESIGN      = "DESIGN";
    public static final String      STATUS_DISTRIBUTED = "DISTRIBUTED";
    public static final String      STATUS_ACTIVE      = "ACTIVE";
    public static final String      STATUS_STOPPED     = "STOPPED";
    public static final String      STATUS_DELETING    = "DELETING";
    public static final String      STATUS_ERROR       = "ERROR";                                             // manual
                                                                                                              // intervention
                                                                                                              // required
    public static final String      STATUS_UNKNOWN     = "UNKNOWN";

    private String                  id;
    private String                  templateId;
    private String                  templateName;
    private String                  name;
    private String                  controlNamePrefix;
    private String                  controlNameUuid;
    private String                  bpmnId;
    private String                  bpmnUserid;
    private String                  bpmnText;
    private String                  propId;
    private String                  propUserid;
    private String                  propText;
    private String                  imageId;
    private String                  imageUserid;
    private String                  imageText;
    private String                  docId;
    private String                  docUserid;
    private String                  docText;
    private String                  blueprintId;
    private String                  blueprintUserid;
    private String                  blueprintText;
    private CldsEvent               event;
    private String                  status;
    private List<String>            permittedActionCd;
    private List<CldsModelInstance> cldsModelInstanceList;

    private String                  typeId;
    private String                  typeName;

    private String                  dispatcherResponse;

    private String                  deploymentId;

    private boolean                 userAuthorizedToUpdate;

    /**
     * Construct empty model.
     */
    public CldsModel() {
        event = new CldsEvent();
    }

    /**
     * Retrieve from DB.
     *
     * @param cldsDao
     * @param name
     * @return
     */
    public static CldsModel retrieve(CldsDao cldsDao, String name, boolean okIfNotFound) {
        // get from db
        CldsModel model = cldsDao.getModelTemplate(name);
        if (model.getId() == null && !okIfNotFound) {
            throw new NotFoundException();
        }
        model.determineStatus();
        model.determinePermittedActionCd();
        return model;
    }

    public boolean canInventoryCall() {
        boolean canCall = false;
        /* Below checks the clds ecent is submit/resubmit */

        if ((event.isActionCd(CldsEvent.ACTION_SUBMIT) || event.isActionCd(CldsEvent.ACTION_RESUBMIT))) {
            canCall = true;
        }
        return canCall;
    }

    /**
     * Save model to DB.
     *
     * @param cldsDao
     * @param userid
     */
    public void save(CldsDao cldsDao, String userid) {
        cldsDao.setModel(this, userid);
        determineStatus();
        determinePermittedActionCd();
    }

    /**
     * Insert a new event for the new action. Throw IllegalArgumentException if
     * requested actionCd is not permitted.
     *
     * @param cldsDao
     * @param userid
     * @param actionCd
     * @param actionStateCd
     */
    public void insEvent(CldsDao cldsDao, String userid, String actionCd, String actionStateCd) {
        validateAction(actionCd);
        event = CldsEvent.insEvent(cldsDao, this, userid, actionCd, actionStateCd, null);
        determineStatus();
        determinePermittedActionCd();
    }

    /**
     * Update event with processInstanceId
     *
     * @param cldsDao
     * @param processInstanceId
     */
    public void updEvent(CldsDao cldsDao, String processInstanceId) {
        cldsDao.updEvent(event.getId(), processInstanceId);
    }

    /**
     * set the status in the model
     */
    private void determineStatus() {

        status = STATUS_UNKNOWN;
        if (event == null || event.getActionCd() == null) {
            status = STATUS_DESIGN;
        } else if (event.isActionStateCd(CldsEvent.ACTION_STATE_ERROR)) {
            status = STATUS_ERROR;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_CREATE, CldsEvent.ACTION_STATE_ANY)
                || event.isActionAndStateCd(CldsEvent.ACTION_SUBMIT, CldsEvent.ACTION_STATE_ANY)
                || event.isActionAndStateCd(CldsEvent.ACTION_RESUBMIT, CldsEvent.ACTION_STATE_ANY)
                || event.isActionAndStateCd(CldsEvent.ACTION_DELETE, CldsEvent.ACTION_STATE_RECEIVED)) {
            status = STATUS_DESIGN;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DISTRIBUTE, CldsEvent.ACTION_STATE_RECEIVED)
                || event.isActionAndStateCd(CldsEvent.ACTION_UNDEPLOY, CldsEvent.ACTION_STATE_RECEIVED)) {
            status = STATUS_DISTRIBUTED;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DELETE, CldsEvent.ACTION_STATE_SENT)) {
            status = STATUS_DELETING;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_STATE_RECEIVED)
                || event.isActionAndStateCd(CldsEvent.ACTION_RESTART, CldsEvent.ACTION_STATE_ANY)
                || event.isActionAndStateCd(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STATE_ANY)
                || event.isActionAndStateCd(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_STATE_ANY)) {
            status = STATUS_ACTIVE;
        } else if (event.isActionAndStateCd(CldsEvent.ACTION_STOP, CldsEvent.ACTION_STATE_ANY)) {
            status = STATUS_STOPPED;
        }

    }

    /**
     * Get the actionCd from current event. If none, default value is
     * CldsEvent.ACTION_CREATE
     *
     * @return
     */
    private String getCurrentActionCd() {
        // current default actionCd is CREATE
        String actionCd = CldsEvent.ACTION_CREATE;
        if (event != null && event.getActionCd() != null) {
            actionCd = event.getActionCd();
        }
        return actionCd;
    }

    /**
     * Get the actionStateCd from current event. If none, default value is
     * CldsEvent.ACTION_STATE_COMPLETED
     *
     * @return
     */
    private String getCurrentActionStateCd() {
        // current default actionStateCd is CREATE
        String actionStateCd = CldsEvent.ACTION_STATE_COMPLETED;
        if (event != null && event.getActionStateCd() != null) {
            actionStateCd = event.getActionStateCd();
        }
        return actionStateCd;
    }

    /**
     * Determine permittedActionCd list using the actionCd from the current
     * event.
     */
    private void determinePermittedActionCd() {
        String actionCd = getCurrentActionCd();
        switch (actionCd) {
            case CldsEvent.ACTION_CREATE:
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_SUBMIT, CldsEvent.ACTION_TEST);
                break;
            case CldsEvent.ACTION_SUBMIT:
            case CldsEvent.ACTION_RESUBMIT:
                // for 1702 delete is not currently implemented (and resubmit
                // requires manually deleting artifact from sdc
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_RESUBMIT);
                break;
            case CldsEvent.ACTION_DISTRIBUTE:
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_RESUBMIT);
                break;
            case CldsEvent.ACTION_UNDEPLOY:
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_RESUBMIT);
                break;
            case CldsEvent.ACTION_DEPLOY:
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_UNDEPLOY, CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP);
                break;
            case CldsEvent.ACTION_RESTART:
            case CldsEvent.ACTION_UPDATE:
                // for 1702 delete is not currently implemented
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_DEPLOY, CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_STOP, CldsEvent.ACTION_UNDEPLOY);
                break;
            case CldsEvent.ACTION_DELETE:
                if (getCurrentActionStateCd().equals(CldsEvent.ACTION_STATE_SENT)) {
                    permittedActionCd = Arrays.asList();
                } else {
                    permittedActionCd = Arrays.asList(CldsEvent.ACTION_SUBMIT);
                }
                break;
            case CldsEvent.ACTION_STOP:
                // for 1702 delete is not currently implemented
                permittedActionCd = Arrays.asList(CldsEvent.ACTION_UPDATE, CldsEvent.ACTION_RESTART,
                        CldsEvent.ACTION_UNDEPLOY);
                break;
            default:
                logger.warn("Invalid current actionCd: " + actionCd);
        }
    }

    /**
     * Validate requestedActionCd - determine permittedActionCd and then check
     * if contained in permittedActionCd Throw IllegalArgumentException if
     * requested actionCd is not permitted.
     *
     * @param requestedActionCd
     */
    public void validateAction(String requestedActionCd) {
        determinePermittedActionCd();
        if (!permittedActionCd.contains(requestedActionCd)) {
            throw new IllegalArgumentException(
                    "Invalid requestedActionCd: " + requestedActionCd + ".  Given current actionCd: "
                            + getCurrentActionCd() + ", the permittedActionCd: " + permittedActionCd);
        }
    }

    /**
     * Extract the UUID portion of a given full control name (controlNamePrefix
     * + controlNameUuid). No fields are populated other than controlNamePrefix
     * and controlNameUuid. Throws BadRequestException if length of given
     * control name is less than UUID_LENGTH.
     *
     * @param fullControlName
     * @return
     */
    public static CldsModel createUsingControlName(String fullControlName) {

        int len = 0;

        if (fullControlName != null) {
            len = fullControlName.length();
        }
        if (len < UUID_LENGTH) {
            throw new BadRequestException(
                    "closed loop id / control name length, " + len + ", less than the minimum of: " + UUID_LENGTH);
        }
        CldsModel model = new CldsModel();
        model.setControlNamePrefix(fullControlName.substring(0, len - UUID_LENGTH));
        model.setControlNameUuid(fullControlName.substring(len - UUID_LENGTH));
        return model;
    }

    /**
     * @return the controlName (controlNamePrefix + controlNameUuid)
     */
    public String getControlName() {
        return controlNamePrefix + controlNameUuid;
    }

    /**
     * To insert modelInstance to the database
     *
     * @param cldsDao
     * @param dcaeEvent
     */
    public static CldsModel insertModelInstance(CldsDao cldsDao, DcaeEvent dcaeEvent, String userid) {
        String controlName = dcaeEvent.getControlName();
        CldsModel cldsModel = createUsingControlName(controlName);
        cldsModel = cldsDao.getModelByUuid(cldsModel.getControlNameUuid());
        cldsModel.determineStatus();
        if (dcaeEvent.getCldsActionCd().equals(CldsEvent.ACTION_UNDEPLOY) || (dcaeEvent.getCldsActionCd()
                .equals(CldsEvent.ACTION_DEPLOY)
                && (cldsModel.getStatus().equals(STATUS_DISTRIBUTED) || cldsModel.getStatus().equals(STATUS_DESIGN)))) {
            CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userid, dcaeEvent.getCldsActionCd(),
                    CldsEvent.ACTION_STATE_RECEIVED, null);
        }
        cldsDao.insModelInstance(cldsModel, dcaeEvent.getInstances());
        return cldsModel;
    }

    /**
     * To remove modelInstance from the database This method is defunct - DCAE
     * Proxy will not undeploy individual instances. It will send an empty list
     * of deployed instances to indicate all have been removed. Or it will send
     * an updated list to indicate those that are still deployed with any not on
     * the list considered undeployed.
     *
     * @param cldsDao
     * @param dcaeEvent
     */
    @SuppressWarnings("unused")
    private static CldsModel removeModelInstance(CldsDao cldsDao, DcaeEvent dcaeEvent) {
        String controlName = dcaeEvent.getControlName();
        // cldsModel = cldsDao.delModelInstance(cldsModel.getControlNameUuid(),
        // dcaeEvent.getInstances() );
        return createUsingControlName(controlName);
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the typeName
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @param name
     *            the typeName to set
     */
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
     * @return the controlNamePrefix
     */
    public String getControlNamePrefix() {
        return controlNamePrefix;
    }

    /**
     * @param controlNamePrefix
     *            the controlNamePrefix to set
     */
    public void setControlNamePrefix(String controlNamePrefix) {
        this.controlNamePrefix = controlNamePrefix;
    }

    /**
     * @return the controlNameUuid
     */
    public String getControlNameUuid() {
        return controlNameUuid;
    }

    /**
     * @param controlNameUuid
     *            the controlNameUuid to set
     */
    public void setControlNameUuid(String controlNameUuid) {
        this.controlNameUuid = controlNameUuid;
    }

    /**
     * @return the propUserid
     */
    public String getPropUserid() {
        return propUserid;
    }

    /**
     * @param propUserid
     *            the propUserid to set
     */
    public void setPropUserid(String propUserid) {
        this.propUserid = propUserid;
    }

    /**
     * @return the propText
     */
    public String getPropText() {
        return propText;
    }

    /**
     * @param propText
     *            the propText to set
     */
    public void setPropText(String propText) {
        this.propText = propText;
    }

    /**
     * @return the event
     */
    public CldsEvent getEvent() {
        return event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getPropId() {
        return propId;
    }

    public void setPropId(String propId) {
        this.propId = propId;
    }

    /**
     * @param event
     *            the event to set
     */
    public void setEvent(CldsEvent event) {
        this.event = event;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the permittedActionCd
     */
    public List<String> getPermittedActionCd() {
        return permittedActionCd;
    }

    /**
     * @param permittedActionCd
     *            the permittedActionCd to set
     */
    public void setPermittedActionCd(List<String> permittedActionCd) {
        this.permittedActionCd = permittedActionCd;
    }

    public String getBlueprintId() {
        return blueprintId;
    }

    public void setBlueprintId(String blueprintId) {
        this.blueprintId = blueprintId;
    }

    public String getBlueprintUserid() {
        return blueprintUserid;
    }

    public void setBlueprintUserid(String blueprintUserid) {
        this.blueprintUserid = blueprintUserid;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public String getBpmnId() {
        return bpmnId;
    }

    public void setBpmnId(String bpmnId) {
        this.bpmnId = bpmnId;
    }

    public String getBpmnUserid() {
        return bpmnUserid;
    }

    public void setBpmnUserid(String bpmnUserid) {
        this.bpmnUserid = bpmnUserid;
    }

    public String getBpmnText() {
        return bpmnText;
    }

    public void setBpmnText(String bpmnText) {
        this.bpmnText = bpmnText;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getImageUserid() {
        return imageUserid;
    }

    public void setImageUserid(String imageUserid) {
        this.imageUserid = imageUserid;
    }

    public String getImageText() {
        return imageText;
    }

    public void setImageText(String imageText) {
        this.imageText = imageText;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getDocUserid() {
        return docUserid;
    }

    public void setDocUserid(String docUserid) {
        this.docUserid = docUserid;
    }

    public String getDocText() {
        return docText;
    }

    public void setDocText(String docText) {
        this.docText = docText;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public List<CldsModelInstance> getCldsModelInstanceList() {
        if (cldsModelInstanceList == null) {
            cldsModelInstanceList = new ArrayList<>();
        }
        return cldsModelInstanceList;
    }

    public void setCldsModelInstanceList(List<CldsModelInstance> cldsModelInstanceList) {
        this.cldsModelInstanceList = cldsModelInstanceList;
    }

    public void setDispatcherResponse(String dispatcherResponse) {
        this.dispatcherResponse = dispatcherResponse;

    }

    public String getDispatcherResponse() {
        return this.dispatcherResponse;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public boolean isUserAuthorizedToUpdate() {
        return userAuthorizedToUpdate;
    }

    public void setUserAuthorizedToUpdate(boolean userAuthorizedToUpdate) {
        this.userAuthorizedToUpdate = userAuthorizedToUpdate;
    }
}
