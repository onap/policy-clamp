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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.sdc.controller;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.util.Date;

import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.config.sdc.SdcSingleControllerConfiguration;
import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcControllerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcDownloadException;
import org.onap.clamp.clds.exception.sdc.controller.SdcParametersException;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.clds.sdc.controller.installer.CsarInstaller;
import org.onap.clamp.clds.util.LoggingUtils;
import org.openecomp.sdc.api.IDistributionClient;
import org.openecomp.sdc.api.consumer.IDistributionStatusMessage;
import org.openecomp.sdc.api.consumer.INotificationCallback;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.api.results.IDistributionClientResult;
import org.openecomp.sdc.impl.DistributionClientFactory;
import org.openecomp.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.openecomp.sdc.utils.DistributionActionResultEnum;
import org.openecomp.sdc.utils.DistributionStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class handles one sdc controller defined in the config. It's
 * instantiated by Spring config.
 */
public class SdcSingleController {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(SdcSingleController.class);
    protected boolean isAsdcClientAutoManaged = false;
    @Autowired
    protected CsarInstaller resourceInstaller;
    @Autowired
    protected ClampProperties refProp;
    public static final String CONFIG_SDC_FOLDER = "sdc.csarFolder";

    /**
     * Inner class for Notification callback
     */
    private final class ASDCNotificationCallBack implements INotificationCallback {

        private SdcSingleController asdcController;

        ASDCNotificationCallBack(SdcSingleController controller) {
            asdcController = controller;
        }

        /**
         * This method can be called multiple times at the same moment. The
         * controller must be thread safe !
         */
        @Override
        public void activateCallback(INotificationData iNotif) {
            Date startTime = new Date();
            String event = "Receive a callback notification in ASDC, nb of resources: " + iNotif.getResources().size();
            logger.debug(event);
            asdcController.treatNotification(iNotif);
            LoggingUtils.setTimeContext(startTime, new Date());
            LoggingUtils.setResponseContext("0", "SDC Notification received and processed successfully",
                    this.getClass().getName());
        }
    }

    // ***** Controller STATUS code
    protected int nbOfNotificationsOngoing = 0;

    public int getNbOfNotificationsOngoing() {
        return nbOfNotificationsOngoing;
    }

    private SdcSingleControllerStatus controllerStatus = SdcSingleControllerStatus.STOPPED;

    protected final synchronized void changeControllerStatus(SdcSingleControllerStatus newControllerStatus) {
        switch (newControllerStatus) {
            case BUSY:
                ++this.nbOfNotificationsOngoing;
                this.controllerStatus = newControllerStatus;
                break;
            case IDLE:
                if (this.nbOfNotificationsOngoing > 1) {
                    --this.nbOfNotificationsOngoing;
                } else {
                    this.nbOfNotificationsOngoing = 0;
                    this.controllerStatus = newControllerStatus;
                }
                break;
            default:
                this.controllerStatus = newControllerStatus;
                break;
        }
    }

    public final synchronized SdcSingleControllerStatus getControllerStatus() {
        return this.controllerStatus;
    }

    // ***** END of Controller STATUS code
    protected SdcSingleControllerConfiguration sdcConfig;
    private IDistributionClient distributionClient;

    public SdcSingleController(SdcSingleControllerConfiguration sdcSingleConfig, boolean isClientAutoManaged) {
        this.isAsdcClientAutoManaged = isClientAutoManaged;
        sdcConfig = sdcSingleConfig;
    }

    /**
     * This method initializes the SDC Controller and the SDC Client.
     *
     * @throws SdcControllerException
     *             It throws an exception if the SDC Client cannot be
     *             instantiated or if an init attempt is done when already
     *             initialized
     * @throws SdcParametersException
     *             If there is an issue with the parameters provided
     */
    public void initSdc() throws SdcControllerException {
        logger.debug("Attempt to initialize the SDC Controller");
        if (this.getControllerStatus() != SdcSingleControllerStatus.STOPPED) {
            throw new SdcControllerException("The controller is already initialized, call the closeSDC method first");
        }
        if (this.distributionClient == null) {
            distributionClient = DistributionClientFactory.createDistributionClient();
        }
        IDistributionClientResult result = this.distributionClient.init(sdcConfig, new ASDCNotificationCallBack(this));
        if (!result.getDistributionActionResult().equals(DistributionActionResultEnum.SUCCESS)) {
            logger.error("ASDC distribution client init failed with reason:" + result.getDistributionMessageResult());
            this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
            throw new SdcControllerException("Initialization of the SDC Controller failed with reason: "
                    + result.getDistributionMessageResult());
        }
        result = this.distributionClient.start();
        if (!result.getDistributionActionResult().equals(DistributionActionResultEnum.SUCCESS)) {
            logger.debug("SDC distribution client start failed with reason:" + result.getDistributionMessageResult());
            this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
            throw new SdcControllerException(
                    "Startup of the SDC Controller failed with reason: " + result.getDistributionMessageResult());
        }
        this.changeControllerStatus(SdcSingleControllerStatus.IDLE);
    }

    /**
     * This method closes the SDC Controller and the SDC Client.
     *
     * @throws SdcControllerException
     *             It throws an exception if the SDC Client cannot be closed
     *             because it's currently BUSY in processing notifications.
     */
    public void closeSdc() throws SdcControllerException {
        if (this.getControllerStatus() == SdcSingleControllerStatus.BUSY) {
            throw new SdcControllerException("Cannot close the ASDC controller as it's currently in BUSY state");
        }
        if (this.distributionClient != null) {
            this.distributionClient.stop();
            // If auto managed we can set it to Null, SdcController controls it.
            // In the other case the client of this class has specified it, so
            // we can't reset it
            if (isAsdcClientAutoManaged) {
                // Next init will initialize it with a new Sdc Client
                this.distributionClient = null;
            }
        }
        this.changeControllerStatus(SdcSingleControllerStatus.STOPPED);
    }

    /**
     * This method processes the notification received from Sdc.
     * 
     * @param iNotif
     *            The INotificationData
     */
    public void treatNotification(INotificationData iNotif) {
        CsarHandler csar = null;
        try {
            logger.info("Notification received for service UUID:" + iNotif.getServiceUUID());
            this.changeControllerStatus(SdcSingleControllerStatus.BUSY);
            csar = new CsarHandler(iNotif, this.sdcConfig.getSdcControllerName(),
                    refProp.getStringValue(CONFIG_SDC_FOLDER));
            if (resourceInstaller.isCsarAlreadyDeployed(csar)) {
                csar.save(downloadTheArtifact(csar.getArtifactElement()));
                this.sendASDCNotification(NotificationType.DOWNLOAD, csar.getArtifactElement().getArtifactURL(),
                        sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DOWNLOAD_OK, null,
                        System.currentTimeMillis());
                resourceInstaller.installTheCsar(csar);
                this.sendASDCNotification(NotificationType.DEPLOY, csar.getArtifactElement().getArtifactURL(),
                        sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DEPLOY_OK, null,
                        System.currentTimeMillis());
            } else {
                this.sendASDCNotification(NotificationType.DOWNLOAD, csar.getArtifactElement().getArtifactURL(),
                        sdcConfig.getConsumerID(), iNotif.getDistributionID(),
                        DistributionStatusEnum.ALREADY_DOWNLOADED, null, System.currentTimeMillis());
                this.sendASDCNotification(NotificationType.DOWNLOAD, csar.getArtifactElement().getArtifactURL(),
                        sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.ALREADY_DEPLOYED,
                        null, System.currentTimeMillis());
            }
        } catch (SdcArtifactInstallerException e) {
            logger.error("SdcArtifactInstallerException exception caught during the notification processing", e);
            this.sendASDCNotification(NotificationType.DEPLOY, csar.getArtifactElement().getArtifactURL(),
                    sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DEPLOY_ERROR,
                    e.getMessage(), System.currentTimeMillis());
        } catch (SdcDownloadException e) {
            logger.error("SdcDownloadException exception caught during the notification processing", e);
            this.sendASDCNotification(NotificationType.DOWNLOAD, csar.getArtifactElement().getArtifactURL(),
                    sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DOWNLOAD_ERROR,
                    e.getMessage(), System.currentTimeMillis());
        } catch (CsarHandlerException e) {
            logger.error("CsarHandlerException exception caught during the notification processing", e);
            this.sendASDCNotification(NotificationType.DOWNLOAD, csar.getArtifactElement().getArtifactURL(),
                    sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DOWNLOAD_ERROR,
                    e.getMessage(), System.currentTimeMillis());
        } catch (SdcToscaParserException e) {
            this.sendASDCNotification(NotificationType.DEPLOY, csar.getArtifactElement().getArtifactURL(),
                    sdcConfig.getConsumerID(), iNotif.getDistributionID(), DistributionStatusEnum.DEPLOY_ERROR,
                    e.getMessage(), System.currentTimeMillis());
        } catch (RuntimeException e) {
            logger.error("Unexpected exception caught during the notification processing", e);
        } finally {
            this.changeControllerStatus(SdcSingleControllerStatus.IDLE);
        }
    }

    private enum NotificationType {
        DOWNLOAD, DEPLOY
    }

    private IDistributionClientDownloadResult downloadTheArtifact(IArtifactInfo artifact) throws SdcDownloadException {
        logger.debug("Trying to download the artifact : " + artifact.getArtifactURL() + " UUID: "
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
                    + " could not be downloaded from ASDC URL " + artifact.getArtifactURL() + " UUID "
                    + artifact.getArtifactUUID() + ")" + System.lineSeparator() + "Error message is "
                    + downloadResult.getDistributionMessageResult() + System.lineSeparator());
        }
        return downloadResult;
    }

    private void sendASDCNotification(NotificationType notificationType, String artifactURL, String consumerID,
            String distributionID, DistributionStatusEnum status, String errorReason, long timestamp) {
        String event = "Sending " + notificationType.name() + "(" + status.name() + ")"
                + " notification to ASDC for artifact:" + artifactURL;
        if (errorReason != null) {
            event = event + "(" + errorReason + ")";
        }
        logger.info(event);
        String action = "";
        try {
            IDistributionStatusMessage message = new DistributionStatusMessage(artifactURL, consumerID, distributionID,
                    status, timestamp);
            switch (notificationType) {
                case DOWNLOAD:
                    if (errorReason != null) {
                        this.distributionClient.sendDownloadStatus(message, errorReason);
                    } else {
                        this.distributionClient.sendDownloadStatus(message);
                    }
                    action = "sendDownloadStatus";
                    break;
                case DEPLOY:
                    if (errorReason != null) {
                        this.distributionClient.sendDeploymentStatus(message, errorReason);
                    } else {
                        this.distributionClient.sendDeploymentStatus(message);
                    }
                    action = "sendDeploymentdStatus";
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
            logger.warn("Unable to send the Sdc Notification (" + action + ") due to an exception", e);
        }
        logger.info("Sdc Notification sent successfully(" + action + ")");
    }
}
