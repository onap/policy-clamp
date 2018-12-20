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

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.onap.clamp.clds.dao.CldsDao;
import org.onap.clamp.clds.model.actions.ActionsHandler;
import org.onap.clamp.clds.model.actions.ActionsHandlerImpl;
import org.onap.clamp.clds.model.status.StatusHandler;
import org.onap.clamp.clds.model.status.StatusHandlerImpl;

/**
 * Represent a CLDS Model.
 */
public class CldsModel {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CldsModel.class);
    private static final int UUID_LENGTH = 36;
    public static final String STATUS_DESIGN = "DESIGN";
    public static final String STATUS_DISTRIBUTED = "DISTRIBUTED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_DELETING = "DELETING";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    private String id;
    private String templateId;
    private String templateName;
    private String name;
    private String controlNamePrefix;
    private String controlNameUuid;
    private String bpmnText;
    private String propText;
    private String imageText;
    private String docText;
    private String blueprintText;
    private CldsEvent event;
    private String status;
    private List<String> permittedActionCd;
    private List<CldsModelInstance> cldsModelInstanceList;
    // This is a transient value used to return the failure message to UI
    private String errorMessageForUi;
    /**
     * The service type Id received from DCAE by querying it
     */
    private String typeId;
    private String typeName;
    private String deploymentId;
    private String deploymentStatusUrl;

    // Set default handlers but this can be changed if needed.
    private static StatusHandler statusHandler = new StatusHandlerImpl();
    private static ActionsHandler actionsHandler = new ActionsHandlerImpl();

    public static synchronized void setStatusHandler(StatusHandler statHandler) {
        statusHandler = statHandler;
    }

    public static synchronized void setActionsHandler(ActionsHandler cdHandler) {
        actionsHandler = cdHandler;
    }

    /**
     * Construct empty model.
     */
    public CldsModel() {
        event = new CldsEvent();
    }

    /**
     * Retrieve from DB.
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

    public boolean canDcaeInventoryCall() {
        boolean canCall = false;
        /* Below checks the clds event is submit/resubmit/distribute */
        if (event.isActionCd(CldsEvent.ACTION_SUBMIT) || event.isActionCd(CldsEvent.ACTION_RESUBMIT)
            || event.isActionCd(CldsEvent.ACTION_DISTRIBUTE) || event.isActionCd(CldsEvent.ACTION_SUBMITDCAE)) {
            canCall = true;
        }
        return canCall;
    }

    /**
     * Save model to DB.
     */
    public CldsModel save(CldsDao cldsDao, String userid) {
        CldsModel cldsModel = cldsDao.setModel(this, userid);
        determineStatus();
        determinePermittedActionCd();
        return cldsModel;
    }

    /**
     * set the status in the model
     */
    public void determineStatus() {
        status = statusHandler.determineStatusOnLastEvent(event);
    }

    /**
     * Determine permittedActionCd list using the actionCd from the current event.
     * It's a states graph, given the next action that can be executed from the one
     * that has been executed (described in the event object). ACTION_CREATE being
     * the first one.
     */
    public void determinePermittedActionCd() {
        permittedActionCd = actionsHandler.determinePermittedActionsOnLastEvent(event, propText);
    }

    /**
     * Validate requestedActionCd - determine permittedActionCd and then check if
     * contained in permittedActionCd Throw IllegalArgumentException if requested
     * actionCd is not permitted.
     */
    public void validateAction(String requestedActionCd) {
        determinePermittedActionCd();
        if (!permittedActionCd.contains(requestedActionCd)) {
            throw new IllegalArgumentException(
                "Invalid requestedActionCd: " + requestedActionCd + ".  Given current actionCd: "
                    + actionsHandler.getCurrentActionCd(event) + ", the permittedActionCd: " + permittedActionCd);
        }
    }

    /**
     * Extract the UUID portion of a given full control name (controlNamePrefix +
     * controlNameUuid). No fields are populated other than controlNamePrefix and
     * controlNameUuid. Throws BadRequestException if length of given control name
     * is less than UUID_LENGTH.
     */
    public static CldsModel createUsingControlName(String fullControlName) {
        if (fullControlName == null || fullControlName.length() < UUID_LENGTH) {
            throw new BadRequestException(
                "closed loop id / control name length, " + (fullControlName != null ? fullControlName.length() : 0)
                    + ", less than the minimum of: " + UUID_LENGTH);
        }
        CldsModel model = new CldsModel();
        model.setControlNamePrefix(fullControlName.substring(0, fullControlName.length() - UUID_LENGTH));
        model.setControlNameUuid(fullControlName.substring(fullControlName.length() - UUID_LENGTH));
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
     */
    public static CldsModel insertModelInstance(CldsDao cldsDao, DcaeEvent dcaeEvent, String userid) {
        String controlName = dcaeEvent.getControlName();
        CldsModel cldsModel = createUsingControlName(controlName);
        cldsModel = cldsDao.getModelByUuid(cldsModel.getControlNameUuid());
        cldsModel.determineStatus();
        if (dcaeEvent.getCldsActionCd().equals(CldsEvent.ACTION_UNDEPLOY)
            || (dcaeEvent.getCldsActionCd().equals(CldsEvent.ACTION_DEPLOY)
                && (cldsModel.getStatus().equals(STATUS_DISTRIBUTED) || cldsModel.getStatus().equals(STATUS_DESIGN)))) {
            CldsEvent.insEvent(cldsDao, dcaeEvent.getControlName(), userid, dcaeEvent.getCldsActionCd(),
                CldsEvent.ACTION_STATE_RECEIVED, null);
        }
        cldsDao.insModelInstance(cldsModel, dcaeEvent.getInstances());
        return cldsModel;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *        the name to set
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
     *        the controlNamePrefix to set
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
     *        the controlNameUuid to set
     */
    public void setControlNameUuid(String controlNameUuid) {
        this.controlNameUuid = controlNameUuid;
    }

    /**
     * @return the propText
     */
    public String getPropText() {
        return propText;
    }

    /**
     * @param propText
     *        the propText to set
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

    /**
     * @param event
     *        the event to set
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
     *        the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public String getBpmnText() {
        return bpmnText;
    }

    public void setBpmnText(String bpmnText) {
        this.bpmnText = bpmnText;
    }

    public String getImageText() {
        return imageText;
    }

    public void setImageText(String imageText) {
        this.imageText = imageText;
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

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public List<String> getPermittedActionCd() {
        return permittedActionCd;
    }

    public String getErrorMessageForUi() {
        return errorMessageForUi;
    }

    public void setErrorMessageForUi(String errorMessageForUi) {
        this.errorMessageForUi = errorMessageForUi;
    }

    public String getDeploymentStatusUrl() {
        return deploymentStatusUrl;
    }

    public void setDeploymentStatusUrl(String deploymentStatusUrl) {
        this.deploymentStatusUrl = deploymentStatusUrl;
    }
}
