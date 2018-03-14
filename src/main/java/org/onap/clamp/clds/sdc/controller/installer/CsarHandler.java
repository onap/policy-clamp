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

package org.onap.clamp.clds.sdc.controller.installer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.tosca.parser.api.ISdcCsarHelper;
import org.openecomp.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.openecomp.sdc.tosca.parser.impl.SdcToscaParserFactory;

/**
 * CsarDescriptor that will be used to deploy in CLAMP.
 */
public class CsarHandler {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarHandler.class);
    private IArtifactInfo artifactElement;
    private String filePath;
    private String controllerName;
    private SdcToscaParserFactory factory = SdcToscaParserFactory.getInstance();
    private ISdcCsarHelper sdcCsarHelper;
    public static final String CSAR_TYPE = "TOSCA_CSAR";
    private String csarPath;

    public CsarHandler(INotificationData iNotif, String controller, String sdcCsarPath) throws CsarHandlerException {
        this.csarPath = sdcCsarPath;
        this.controllerName = controller;
        this.artifactElement = searchForUniqueCsar(iNotif);
        this.filePath = buildFilePathForCsar(artifactElement);
    }

    private String buildFilePathForCsar(IArtifactInfo artifactElement) {
        return csarPath + "/" + controllerName + "/" + artifactElement.getArtifactName();
    }

    private IArtifactInfo searchForUniqueCsar(INotificationData iNotif) throws CsarHandlerException {
        List<IArtifactInfo> serviceArtifacts = iNotif.getServiceArtifacts();
        for (IArtifactInfo artifact : serviceArtifacts) {
            if (artifact.getArtifactType().equals(CSAR_TYPE)) {
                return artifact;
            }
        }
        throw new CsarHandlerException("Unable to find a CSAR in the Sdc Notification");
    }

    public void save(IDistributionClientDownloadResult resultArtifact)
            throws SdcArtifactInstallerException, SdcToscaParserException {
        try {
            logger.info("Writing CSAR file : " + artifactElement.getArtifactURL() + " UUID "
                    + artifactElement.getArtifactUUID() + ")");
            Path path = Paths.get(filePath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (FileOutputStream outFile = new FileOutputStream(filePath)) {
                outFile.write(resultArtifact.getArtifactPayload(), 0, resultArtifact.getArtifactPayload().length);
            }
            sdcCsarHelper = factory.getSdcCsarHelper(filePath);
        } catch (IOException e) {
            throw new SdcArtifactInstallerException(
                    "Exception caught when trying to write the CSAR on the file system to " + filePath, e);
        }
    }

    public IArtifactInfo getArtifactElement() {
        return artifactElement;
    }

    public String getFilePath() {
        return filePath;
    }

    public ISdcCsarHelper getSdcCsarHelper() {
        return sdcCsarHelper;
    }
}
