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

package org.onap.clamp.clds.sdc.controller.installer;

import com.att.eelf.configuration.EELFLogger;
import com.att.eelf.configuration.EELFManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.onap.clamp.clds.exception.sdc.controller.CsarHandlerException;
import org.onap.clamp.clds.exception.sdc.controller.SdcArtifactInstallerException;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.onap.sdc.api.results.IDistributionClientDownloadResult;
import org.onap.sdc.tosca.parser.api.ISdcCsarHelper;
import org.onap.sdc.tosca.parser.exceptions.SdcToscaParserException;
import org.onap.sdc.tosca.parser.impl.SdcToscaParserFactory;

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
    private Map<String, BlueprintArtifact> mapOfBlueprints = new HashMap<>();
    public static final String CSAR_TYPE = "TOSCA_CSAR";
    public static final String BLUEPRINT_TYPE = "DCAE_INVENTORY_BLUEPRINT";
    private INotificationData sdcNotification;
    public static final String RESOURCE_INSTANCE_NAME_PREFIX = "/Artifacts/Resources/";
    public static final String RESOURCE_INSTANCE_NAME_SUFFIX = "/Deployment/";
    public static final String POLICY_DEFINITION_NAME_SUFFIX = "Definitions/policies.yml";

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
            logger.info("Writing CSAR file to: " + csarFilePath + " UUID " + artifactElement.getArtifactUUID() + ")");
            Path path = Paths.get(csarFilePath);
            Files.createDirectories(path.getParent());
            // Create or replace the file
            try (OutputStream out = Files.newOutputStream(path)) {
                out.write(resultArtifact.getArtifactPayload(), 0, resultArtifact.getArtifactPayload().length);
            }
            sdcCsarHelper = factory.getSdcCsarHelper(csarFilePath);
            this.loadDcaeBlueprint();
        } catch (IOException e) {
            throw new SdcArtifactInstallerException(
                "Exception caught when trying to write the CSAR on the file system to " + csarFilePath, e);
        }
    }

    private IResourceInstance searchForResourceByInstanceName(String blueprintResourceInstanceName)
        throws SdcArtifactInstallerException {
        for (IResourceInstance resource : this.sdcNotification.getResources()) {
            String filteredString = resource.getResourceInstanceName().replaceAll("-", "");
            filteredString = filteredString.replaceAll(" ", "");
            if (filteredString.equalsIgnoreCase(blueprintResourceInstanceName)) {
                return resource;
            }
        }
        throw new SdcArtifactInstallerException("Error when searching for " + blueprintResourceInstanceName
            + " as ResourceInstanceName in Sdc notification and did not find it");
    }

    private void loadDcaeBlueprint() throws IOException, SdcArtifactInstallerException {
        try (ZipFile zipFile = new ZipFile(csarFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().contains(BLUEPRINT_TYPE)) {
                    BlueprintArtifact blueprintArtifact = new BlueprintArtifact();
                    blueprintArtifact.setBlueprintArtifactName(
                        entry.getName().substring(entry.getName().lastIndexOf('/') + 1, entry.getName().length()));
                    blueprintArtifact
                        .setBlueprintInvariantServiceUuid(this.getSdcNotification().getServiceInvariantUUID());
                    try (InputStream stream = zipFile.getInputStream(entry)) {
                        blueprintArtifact.setDcaeBlueprint(IOUtils.toString(stream));
                    }
                    blueprintArtifact.setResourceAttached(searchForResourceByInstanceName(entry.getName().substring(
                        entry.getName().indexOf(RESOURCE_INSTANCE_NAME_PREFIX) + RESOURCE_INSTANCE_NAME_PREFIX.length(),
                        entry.getName().indexOf(RESOURCE_INSTANCE_NAME_SUFFIX))));
                    this.mapOfBlueprints.put(blueprintArtifact.getBlueprintArtifactName(), blueprintArtifact);
                    logger.info("Found a blueprint entry in the CSAR " + blueprintArtifact.getBlueprintArtifactName()
                        + " for resource instance Name "
                        + blueprintArtifact.getResourceAttached().getResourceInstanceName());
                }
            }
            logger.info(this.mapOfBlueprints.size() + " blueprint(s) will be converted to closed loop");
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

    public INotificationData getSdcNotification() {
        return sdcNotification;
    }

    public Map<String, BlueprintArtifact> getMapOfBlueprints() {
        return mapOfBlueprints;
    }

    public Optional<String> getPolicyModelYaml() throws IOException {
        String result = null;
        try (ZipFile zipFile = new ZipFile(csarFilePath)) {
            ZipEntry entry = zipFile.getEntry(POLICY_DEFINITION_NAME_SUFFIX);
            if (entry != null) {
                result = IOUtils.toString(zipFile.getInputStream(entry));
            } else {
                logger.info("Policy model not found inside the CSAR file: " + csarFilePath);
            }
            return Optional.ofNullable(result);
        }
    }
}
