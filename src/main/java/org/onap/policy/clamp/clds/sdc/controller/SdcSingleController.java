/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2018-2019, 2021 AT&T Intellectual Property. All rights
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
 * Modifications copyright (c) 2018 Nokia
 * ===================================================================
 *
 */

package org.onap.policy.clamp.clds.sdc.controller;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map.Entry;
import org.onap.policy.clamp.clds.config.ClampProperties;
import org.onap.policy.clamp.clds.config.sdc.SdcSingleControllerConfiguration;
import org.onap.policy.clamp.clds.exception.sdc.controller.BlueprintParserException;
import org.onap.policy.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcControllerException;
import org.onap.policy.clamp.clds.exception.sdc.controller.SdcDownloadException;
import org.onap.policy.clamp.clds.sdc.controller.installer.BlueprintArtifact;
import org.onap.policy.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.policy.clamp.clds.util.LoggingUtils;
import org.onap.policy.clamp.loop.CsarInstaller;
import org.onap.sdc.api.IDistributionClient;
import org.onap.sdc.api.consumer.IComponentDoneStatusMessage;
import org.onap.sdc.api.consumer.IDistributionStatusMessage;
import org.onap.sdc.api.consumer.INotificationCallback;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.api.results.IDistributionClientResult;
import org.onap.sdc.impl.DistributionClientFactory;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.utils.DistributionActionResultEnum;
import org.onap.sdc.utils.DistributionStatusEnum;

/**
 * This class handles one sdc controller defined in the config.
 */
public class SdcSingleController {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcSingleController.class);
    private boolean isSdcClientAutoManaged = false;
    private CsarInstaller csarInstaller;
    private ClampProperties refProp;
    /**
     * The constant CONFIG_SDC_FOLDER.
     */
    public static final String CONFIG_SDC_FOLDER = "sdc.csarFolder";
    private int nbOfNotificationsOngoing = 0;
    private SdcSingleControllerStatus controllerStatus = SdcSingleControllerStatus.STOPPED;
    private SdcSingleControllerConfiguration sdcConfig;
    private IDistributionClient distributionClient;

    /**
     * Inner class for Notification callback.
     */
    private final class SdcNotificationCallBack implements INotificationCallback {

        private SdcSingleController sdcController;

        /**
         * Instantiates a new Sdc notification call back.
         *
         * @param controller the controller
         */
        SdcNotificationCallBack(SdcSingleController controller) {
            sdcController = controller;
        }

        /**
         * This method can be called multiple times at the same moment. The controller
         * must be thread safe !
         */
        @Override
        public void activateCallback(INotificationData notificationData) {
            Date startTime = new Date();
            logger.info("Receive a callback notification in SDC, nb of resources: "
                    + notificationData.getResources().size());
            sdcController.treatNotification(notificationData);
            LoggingUtils.setTimeContext(startTime, new Date());
            LoggingUtils.setResponseContext("0", "SDC Notification received and processed successfully",
                    this.getClass().getName());
        }
    }

    /**
     * Gets nb of notifications ongoing.
     *
     * @return the nb of notifications ongoing
     */
    public int getNbOfNotificationsOngoing() {
        return nbOfNotificationsOngoing;
    }

    private void changeControllerStatusIdle() {
        if (this.nbOfNotificationsOngoing > 1) {
            --this.nbOfNotificationsOngoing;
        } else {
            this.nbOfNotificationsOngoing = 0;
            this.controllerStatus = SdcSingleControllerStatus.IDLE;
        }
    }

    /**
     * Change controller status.
     *
     * @param newControllerStatus the new controller status
     */
    protected final synchronized void changeControllerStatus(SdcSingleControllerStatus newControllerStatus) {
        switch (newControllerStatus) {
            case BUSY:
                ++this.nbOfNotificationsOngoing;
                this.controllerStatus = newControllerStatus;
                break;
            case IDLE:
                this.changeControllerStatusIdle();
                break;
            default:
                this.controllerStatus = newControllerStatus;
                break;
        }
    }

    /**
     * Gets controller status.
     *
     * @return the controller status
     */
    public final synchronized SdcSingleControllerStatus getControllerStatus() {
        return this.controllerStatus;
    }

    /**
     * Instantiates a new Sdc single controller.
     *
     * @param clampProp          the clamp prop
     * @param csarInstaller      the csar installer
     * @param sdcSingleConfig    the sdc single config
     * @param distributionClient the distribution client
     */
    public SdcSingleController(ClampProperties clampProp, CsarInstaller csarInstaller,
                               SdcSingleControllerConfiguration sdcSingleConfig,
                               IDistributionClient distributionClient) {
        this.distributionClient = distributionClient;
        isSdcClientAutoManaged = (distributionClient == null);
        this.sdcConfig = sdcSingleConfig;
        this.refProp = clampProp;
        this.csarInstaller = csarInstaller;
    }

    /**
     * This method initializes the SDC Controller and the SDC Client.
     *
     * @throws SdcControllerException It throws an exception if the SDC Client
     *                                cannot be instantiated or if an init attempt
     *                                is done when already initialized
     */
    public void initSdc() throws SdcControllerException {
        logger.info("Attempt to initialize the SDC Controller: " + sdcConfig.getSdcControllerName());
        if (this.getControllerStatus() != SdcSingleControllerStatus.STOPPED) {
            throw new SdcControllerException("The controller is already initialized, call the closeSDC method first");
        }
        if (distributionClient == null) {
            distributionClient = DistributionClientFactory.createDistributionClient();
        }
        IDistributionClientResult result = distributionClient.init(sdcConfig, new SdcNotificationCallBack(this));
        if (!result.getDistributionActionResult().equals(DistributionActionResultEnum.SUCCESS)) {
            logger.error("SDC distribution client init failed with reason:" + result.getDistributionMessageResult());
            this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
            throw new SdcControllerException("Initialization of the SDC Controller failed with reason: "
                    + result.getDistributionMessageResult());
        }
        logger.info("SDC Controller successfully initialized: " + sdcConfig.getSdcControllerName());
        logger.info("Attempt to start the SDC Controller: " + sdcConfig.getSdcControllerName());
        result = this.distributionClient.start();
        if (!result.getDistributionActionResult().equals(DistributionActionResultEnum.SUCCESS)) {
            logger.error("SDC distribution client start failed with reason:" + result.getDistributionMessageResult());
            this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
            throw new SdcControllerException(
                    "Startup of the SDC Controller failed with reason: " + result.getDistributionMessageResult());
        }
        logger.info("SDC Controller successfully started: " + sdcConfig.getSdcControllerName());
        this.changeControllerStatus(SdcSingleControllerStatus.IDLE);
    }

    /**
     * This method closes the SDC Controller and the SDC Client.
     *
     * @throws SdcControllerException It throws an exception if the SDC Client
     *                                cannot be closed because it's currently BUSY
     *                                in processing notifications.
     */
    public void closeSdc() throws SdcControllerException {
        if (this.getControllerStatus() == SdcSingleControllerStatus.BUSY) {
            throw new SdcControllerException("Cannot close the SDC controller as it's currently in BUSY state");
        }
        if (this.distributionClient != null) {
            this.distributionClient.stop();
            // If auto managed we can set it to Null, SdcController controls it.
            // In the other case the client of this class has specified it, so
            // we can't reset it
            if (isSdcClientAutoManaged) {
                // Next init will initialize it with a new SDC Client
                this.distributionClient = null;
            }
        }
        this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
    }

    private void sendAllNotificationForCsarHandler(INotificationData notificationData, CsarHandler csar,
                                                   NotificationType notificationType,
                                                   DistributionStatusEnum distributionStatus, String errorMessage) {
        if (csar != null) {
            // Notify for the CSAR
            this.sendSdcNotification(notificationType, csar.getArtifactElement().getArtifactURL(),
                    sdcConfig.getConsumerID(), notificationData.getDistributionID(), distributionStatus, errorMessage,
                    System.currentTimeMillis());
            // Notify for all VF resources found
            for (Entry<String, BlueprintArtifact> blueprint : csar.getMapOfBlueprints().entrySet()) {
                // Normally always 1 artifact in resource for Clamp as we
                // specified
                // only VF_METADATA type
                this.sendSdcNotification(notificationType,
                        blueprint.getValue().getResourceAttached().getArtifacts().get(0).getArtifactURL(),
                        sdcConfig.getConsumerID(), notificationData.getDistributionID(), distributionStatus,
                        errorMessage, System.currentTimeMillis());
            }
        } else {
            this.sendSdcNotification(notificationType, null, sdcConfig.getConsumerID(),
                    notificationData.getDistributionID(), distributionStatus, errorMessage, System.currentTimeMillis());
        }
    }

    /**
     * This method processes the notification received from Sdc.
     *
     * @param notificationData The INotificationData
     */
    public void treatNotification(INotificationData notificationData) {
        CsarHandler csar = null;
        try {
            // wait for a random time, so that 2 running Clamp will not treat
            // the same Notification at the same time
            Thread.sleep((new SecureRandom().nextInt(10) + 1) * 1000L);
            logger.info("Notification received for service UUID:" + notificationData.getServiceUUID());
            this.changeControllerStatus(SdcSingleControllerStatus.BUSY);
            csar = new CsarHandler(notificationData, this.sdcConfig.getSdcControllerName(),
                    refProp.getStringValue(CONFIG_SDC_FOLDER));
            csar.save(downloadTheArtifact(csar.getArtifactElement()));
            if (csarInstaller.isCsarAlreadyDeployed(csar)) {
                sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DOWNLOAD,
                        DistributionStatusEnum.ALREADY_DOWNLOADED, null);
                sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                        DistributionStatusEnum.ALREADY_DEPLOYED, null);
            } else {
                sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DOWNLOAD,
                        DistributionStatusEnum.DOWNLOAD_OK, null);
                csarInstaller.installTheCsar(csar);
                sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                        DistributionStatusEnum.DEPLOY_OK, null);
            }
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_OK, null);
        } catch (SdcArtifactInstallerException | SdcToscaParserException e) {
            logger.error("SdcArtifactInstallerException exception caught during the notification processing", e);
            sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                    DistributionStatusEnum.DEPLOY_ERROR, e.getMessage());
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_ERROR, e.getMessage());
        } catch (SdcDownloadException | CsarHandlerException e) {
            logger.error("SdcDownloadException exception caught during the notification processing", e);
            sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DOWNLOAD,
                    DistributionStatusEnum.DOWNLOAD_ERROR, e.getMessage());
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_ERROR, e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Interrupt exception caught during the notification processing", e);
            sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                    DistributionStatusEnum.DEPLOY_ERROR, e.getMessage());
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_ERROR, e.getMessage());
            Thread.currentThread().interrupt();
        } catch (BlueprintParserException e) {
            logger.error("BlueprintParser exception caught during the notification processing", e);
            sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                    DistributionStatusEnum.DEPLOY_ERROR, e.getMessage());
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_ERROR, e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Unexpected exception caught during the notification processing", e);
            sendAllNotificationForCsarHandler(notificationData, csar, NotificationType.DEPLOY,
                    DistributionStatusEnum.DEPLOY_ERROR, e.getMessage());
            this.sendComponentStatus(notificationData, DistributionStatusEnum.COMPONENT_DONE_ERROR, e.getMessage());
        } finally {
            this.changeControllerStatus(SdcSingleControllerStatus.IDLE);
        }
    }

    private enum NotificationType {
        /**
         * Download notification type.
         */
        DOWNLOAD,
        /**
         * Deploy notification type.
         */
        DEPLOY
    }

    private IDistributionClientDownloadResult downloadTheArtifact(IArtifactInfo artifact) throws SdcDownloadException {
        logger.info("Trying to download the artifact : " + artifact.getArtifactURL() + " UUID: "
                + artifact.getArtifactUUID());
        IDistributionClientDownloadResult downloadResult;
        try {
            downloadResult = distributionClient.download(artifact);
            if (null == downloadResult) {
                logger.info("downloadResult is Null for: " + artifact.getArtifactUUID());
                return null;
            }
        } catch (RuntimeException e) {
            throw new SdcDownloadException("Exception caught when downloading the artifact", e);
        }
        if (DistributionActionResultEnum.SUCCESS.equals(downloadResult.getDistributionActionResult())) {
            logger.info("Successfully downloaded the artifact " + artifact.getArtifactURL() + " UUID "
                    + artifact.getArtifactUUID() + "Size of payload " + downloadResult.getArtifactPayload().length);
        } else {
            throw new SdcDownloadException("Artifact " + artifact.getArtifactName()
                    + " could not be downloaded from SDC URL " + artifact.getArtifactURL() + " UUID "
                    + artifact.getArtifactUUID() + ")" + System.lineSeparator() + "Error message is "
                    + downloadResult.getDistributionMessageResult() + System.lineSeparator());
        }
        return downloadResult;
    }

    private void sendSdcNotification(NotificationType notificationType, String artifactUrl, String consumerId,
                                     String distributionId, DistributionStatusEnum status, String errorReason,
                                     long timestamp) {
        String event = "Sending " + notificationType.name() + "(" + status.name() + ")"
                + " notification to SDC for artifact:" + artifactUrl;
        if (errorReason != null) {
            event = event + "(" + errorReason + ")";
        }
        logger.info(event);
        String action = "";
        try {
            IDistributionStatusMessage message = new DistributionStatusMessage(artifactUrl, consumerId, distributionId,
                    status, timestamp);
            switch (notificationType) {
                case DOWNLOAD:
                    this.sendDownloadStatus(message, errorReason);
                    action = "sendDownloadStatus";
                    break;
                case DEPLOY:
                    this.sendDeploymentStatus(message, errorReason);
                    action = "sendDeploymentdStatus";
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to send the SDC Notification (" + action + ") due to an exception", e);
        }
        logger.info("SDC Notification sent successfully(" + action + ")");
    }

    private void sendComponentStatus(INotificationData notificationData, DistributionStatusEnum status,
                                     String errorReason) {
        try {
            IComponentDoneStatusMessage message = new IComponentDoneStatusMessage() {

                @Override
                public String getDistributionID() {
                    return notificationData.getDistributionID();
                }

                @Override
                public String getConsumerID() {
                    return sdcConfig.getConsumerID();
                }

                @Override
                public long getTimestamp() {
                    return System.currentTimeMillis();
                }

                @Override
                public DistributionStatusEnum getStatus() {
                    return status;
                }

                @Override
                public String getComponentName() {
                    return sdcConfig.getUser();
                }
            };

            if (errorReason != null) {
                this.distributionClient.sendComponentDoneStatus(message, errorReason);
            } else {
                this.distributionClient.sendComponentDoneStatus(message);
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to send the SDC Notification (" + status.name() + ") due to an exception", e);
        }
        logger.info("SDC Notification sent successfully(" + status.name() + ")");
    }

    private void sendDownloadStatus(IDistributionStatusMessage message, String errorReason) {
        if (errorReason != null) {
            this.distributionClient.sendDownloadStatus(message, errorReason);
        } else {
            this.distributionClient.sendDownloadStatus(message);
        }
    }

    private void sendDeploymentStatus(IDistributionStatusMessage message, String errorReason) {
        if (errorReason != null) {
            this.distributionClient.sendDeploymentStatus(message, errorReason);
        } else {
            this.distributionClient.sendDeploymentStatus(message);
        }
    }
}
