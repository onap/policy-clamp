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

import com.att.aft.dme2.internal.apache.commons.io.IOUtils;
import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.openecomp.sdc.api.notification.IArtifactInfo;
import org.openecomp.sdc.api.notification.INotificationData;
import org.openecomp.sdc.api.notification.IResourceInstance;
import org.openecomp.sdc.api.results.IDistributionClientDownloadResult;
import org.openecomp.sdc.tosca.parser.api.ISdcCsarHelper;
import org.openecomp.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.openecomp.sdc.tosca.parser.impl.SdcToscaParserFactory;

/**
 * CsarDescriptor that will be used to deploy file in CLAMP file system. Some
 * methods can also be used to get some data from it.
 */
public class CsarHandler {

    private static final EELFLogger logger = EELFManager.getInstance().getLogger(CsarHandler.class);
    private IArtifactInfo artifactElement;
    private String csarFilePath;
    private String controllerName;
    private SdcToscaParserFactory factory = SdcToscaParserFactory.getInstance();
    private ISdcCsarHelper sdcCsarHelper;
    private String dcaeBlueprint;
    private String blueprintArtifactName;
    private String blueprintInvariantResourceUuid;
    private String blueprintInvariantServiceUuid;
    public static final String CSAR_TYPE = "TOSCA_CSAR";
    private INotificationData sdcNotification;

    public CsarHandler(INotificationData iNotif, String controller, String clampCsarPath) throws CsarHandlerException {
        this.sdcNotification = iNotif;
        this.controllerName = controller;
        this.artifactElement = searchForUniqueCsar(iNotif);
        this.csarFilePath = buildFilePathForCsar(artifactElement, clampCsarPath);
    }

    private String buildFilePathForCsar(IArtifactInfo artifactElement, String clampCsarPath) {
        return clampCsarPath + "/" + controllerName + "/" + artifactElement.getArtifactName();
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

    public synchronized void save(IDistributionClientDownloadResult resultArtifact)
            throws SdcArtifactInstallerException, SdcToscaParserException {
        try {
            logger.info("Writing CSAR file : " + artifactElement.getArtifactURL() + " UUID "
                    + artifactElement.getArtifactUUID() + ")");
            Path path = Paths.get(csarFilePath);
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (FileOutputStream outFile = new FileOutputStream(csarFilePath)) {
                outFile.write(resultArtifact.getArtifactPayload(), 0, resultArtifact.getArtifactPayload().length);
            }
            sdcCsarHelper = factory.getSdcCsarHelper(csarFilePath);
            this.loadDcaeBlueprint();
            this.loadBlueprintArtifactDetails();
        } catch (IOException e) {
            throw new SdcArtifactInstallerException(
                    "Exception caught when trying to write the CSAR on the file system to " + csarFilePath, e);
        }
    }

    private void loadBlueprintArtifactDetails() {
        blueprintInvariantServiceUuid = this.getSdcNotification().getServiceInvariantUUID();
        for (IResourceInstance resource : this.getSdcNotification().getResources()) {
            if ("VF".equals(resource.getResourceType())) {
                for (IArtifactInfo artifact : resource.getArtifacts()) {
                    if ("DCAE_INVENTORY_BLUEPRINT".equals(artifact.getArtifactType())) {
                        blueprintArtifactName = artifact.getArtifactName();
                        blueprintInvariantResourceUuid = resource.getResourceInvariantUUID();
                    }
                }
            }
        }
    }

    private void loadDcaeBlueprint() throws IOException, SdcArtifactInstallerException {
        List<ZipEntry> listEntries = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(csarFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().contains("DCAE_INVENTORY_BLUEPRINT")) {
                    listEntries.add(entry);
                }
            }
            if (listEntries.size() > 1) {
                throw new SdcArtifactInstallerException("There are multiple entries in the DCAE inventory");
            }
            try (InputStream stream = zipFile.getInputStream(listEntries.get(0))) {
                this.dcaeBlueprint = IOUtils.toString(stream);
            }
        }
    }

    public IArtifactInfo getArtifactElement() {
        return artifactElement;
    }

    public String getFilePath() {
        return csarFilePath;
    }

    public synchronized ISdcCsarHelper getSdcCsarHelper() {
        return sdcCsarHelper;
    }

    public synchronized String getDcaeBlueprint() {
        return dcaeBlueprint;
    }

    public INotificationData getSdcNotification() {
        return sdcNotification;
    }

    public String getBlueprintArtifactName() {
        return blueprintArtifactName;
    }

    public String getBlueprintInvariantResourceUuid() {
        return blueprintInvariantResourceUuid;
    }

    public String getBlueprintInvariantServiceUuid() {
        return blueprintInvariantServiceUuid;
    }
}
