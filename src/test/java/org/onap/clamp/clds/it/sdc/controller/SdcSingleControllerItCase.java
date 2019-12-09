/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Modifications Copyright (c) 2019 Samsung
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

package org.onap.clamp.clds.it.sdc.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.config.sdc.SdcSingleControllerConfigurationTest;
import org.onap.clamp.clds.exception.sdc.controller.SdcControllerException;
import org.onap.clamp.clds.sdc.controller.SdcSingleController;
import org.onap.clamp.clds.sdc.controller.SdcSingleControllerStatus;
import org.onap.clamp.clds.sdc.controller.installer.CsarHandler;
import org.onap.clamp.loop.CsarInstaller;
import org.onap.sdc.api.notification.IArtifactInfo;
import org.onap.sdc.api.notification.INotificationData;
import org.onap.sdc.api.notification.IResourceInstance;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles(profiles = "clamp-default,clamp-default-user,clamp-sdc-controller")
public class SdcSingleControllerItCase {

    private static final String CSAR_ARTIFACT_NAME = "testArtifact.csar";
    private static final String SERVICE_UUID = "serviceUUID";
    private static final String RESOURCE1_UUID = "resource1UUID";
    private static final String RESOURCE1_INSTANCE_NAME = "sim-1802 0";

    @Autowired
    private ClampProperties clampProp;

    private SdcSingleController sdcSingleController;

    private CsarInstaller csarInstaller = mock(CsarInstaller.class);

    private INotificationData buildFakeSdcNotification() {
        // BUild what is needed for CSAR
        IArtifactInfo serviceArtifact = mock(IArtifactInfo.class);
        Mockito.when(serviceArtifact.getArtifactType()).thenReturn(CsarHandler.CSAR_TYPE);
        Mockito.when(serviceArtifact.getArtifactName()).thenReturn(CSAR_ARTIFACT_NAME);
        List<IArtifactInfo> servicesList = new ArrayList<>();
        servicesList.add(serviceArtifact);
        INotificationData notifData = mock(INotificationData.class);
        Mockito.when(notifData.getServiceArtifacts()).thenReturn(servicesList);
        // Build what is needed for UUID
        Mockito.when(notifData.getServiceInvariantUUID()).thenReturn(SERVICE_UUID);
        // Build fake resource with one artifact BLUEPRINT
        IResourceInstance resource1 = mock(IResourceInstance.class);
        Mockito.when(resource1.getResourceType()).thenReturn("VF");
        Mockito.when(resource1.getResourceInvariantUUID()).thenReturn(RESOURCE1_UUID);
        Mockito.when(resource1.getResourceInstanceName()).thenReturn(RESOURCE1_INSTANCE_NAME);
        // Create a fake artifact for resource
        IArtifactInfo blueprintArtifact = mock(IArtifactInfo.class);
        Mockito.when(blueprintArtifact.getArtifactType()).thenReturn(CsarHandler.BLUEPRINT_TYPE);
        List<IArtifactInfo> artifactsListForResource = new ArrayList<>();
        artifactsListForResource.add(blueprintArtifact);
        Mockito.when(resource1.getArtifacts()).thenReturn(artifactsListForResource);
        List<IResourceInstance> resourcesList = new ArrayList<>();
        resourcesList.add(resource1);
        Mockito.when(notifData.getResources()).thenReturn(resourcesList);
        return notifData;
    }

    /**
     * Initialization method.
     */
    @Before
    public void init() {
        sdcSingleController = new SdcSingleController(clampProp, csarInstaller, SdcSingleControllerConfigurationTest
                .loadControllerConfiguration("clds/sdc-controller-config-TLS.json", "sdc-controller1"), null) {
        };
    }

    @Test
    public void testTreatNotification() {
        // when
        sdcSingleController.treatNotification(buildFakeSdcNotification());
        // then
        Assertions.assertThat(sdcSingleController.getNbOfNotificationsOngoing()).isEqualTo(0);
    }

    @Test
    public void testCloseSdc() throws SdcControllerException {
        // when
        sdcSingleController.closeSdc();
        // then
        assertThat(sdcSingleController.getControllerStatus()).isEqualTo(SdcSingleControllerStatus.STOPPED);
    }

    @Test
    public void testActivateCallback() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
            InstantiationException, ClassNotFoundException {
        // given

        Class<?> innerClass = Class
                .forName("org.onap.clamp.clds.sdc.controller.SdcSingleController$SdcNotificationCallBack");
        Constructor<?> constructor = innerClass.getDeclaredConstructor(SdcSingleController.class,
                SdcSingleController.class);
        constructor.setAccessible(true);
        Object child = constructor.newInstance(sdcSingleController, sdcSingleController);
        Method method = child.getClass().getDeclaredMethod("activateCallback", INotificationData.class);
        method.setAccessible(true);
        MDCAdapter mdcAdapter = MDC.getMDCAdapter();
        // when
        method.invoke(child, buildFakeSdcNotification());
        // then
        assertThat(mdcAdapter.get("ResponseCode")).isEqualTo("0");
        assertThat(mdcAdapter.get("StatusCode")).isEqualTo("COMPLETE");
        assertThat(mdcAdapter.get("ResponseDescription"))
                .isEqualTo("SDC Notification received and processed successfully");
        assertThat(mdcAdapter.get("ClassName")).isEqualTo(child.getClass().getName());
    }
}
