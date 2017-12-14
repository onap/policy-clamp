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

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.onap.clamp.clds.AbstractItCase;
import org.onap.clamp.clds.client.req.sdc.SdcCatalogServices;
import org.onap.clamp.clds.model.CldsAlarmCondition;
import org.onap.clamp.clds.model.CldsSdcResource;
import org.onap.clamp.clds.model.CldsSdcResourceBasicInfo;
import org.onap.clamp.clds.model.CldsSdcServiceInfo;
import org.onap.clamp.clds.model.CldsServiceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test SDC Catalog Service class by mocking the SDC answers.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class SdcCatalogServicesItCase extends AbstractItCase {
    @Autowired
    private SdcCatalogServices sdcCatalogWired = new SdcCatalogServices();

    @Test
    public void removeDuplicateServicesTest() {

        CldsSdcServiceInfo service1a = new CldsSdcServiceInfo();
        service1a.setName("service1");
        service1a.setVersion("1.0");
        service1a.setInvariantUUID("invariantUUID1.0");

        List<CldsSdcServiceInfo> rawCldsSdcServiceList = new LinkedList<>();
        rawCldsSdcServiceList.add(service1a);
        rawCldsSdcServiceList.add(service1a);

        CldsSdcServiceInfo service1b = new CldsSdcServiceInfo();
        service1b.setName("service1");
        service1b.setVersion("1.1");
        service1b.setInvariantUUID("invariantUUID1.1");
        rawCldsSdcServiceList.add(service1b);

        CldsSdcServiceInfo service1c = new CldsSdcServiceInfo();
        service1c.setName("service1");
        service1c.setVersion("1.2");
        service1c.setInvariantUUID("invariantUUID1.2");
        rawCldsSdcServiceList.add(service1c);

        CldsSdcServiceInfo service2 = new CldsSdcServiceInfo();
        service2.setName("service2");
        service2.setVersion("1.0");
        service2.setInvariantUUID("invariantUUID2.0");
        rawCldsSdcServiceList.add(service2);

        SdcCatalogServices catalogServices = new SdcCatalogServices();
        List<CldsSdcServiceInfo> resultList = catalogServices.removeDuplicateServices(rawCldsSdcServiceList);

        assertTrue(resultList.size() == 2);

        CldsSdcServiceInfo res1;
        CldsSdcServiceInfo res2;
        if ("service1".equals(resultList.get(0).getName())) {
            res1 = resultList.get(0);
            res2 = resultList.get(1);
        } else {
            res1 = resultList.get(1);
            res2 = resultList.get(0);
        }

        assertTrue("service1".equals(res1.getName()));
        assertTrue("1.2".equals(res1.getVersion()));

        assertTrue("service2".equals(res2.getName()));
        assertTrue("1.0".equals(res2.getVersion()));

    }

    @Test
    public void removeDuplicateSdcResourceInstancesTest() {

        List<CldsSdcResource> rawCldsSdcResourceList = new LinkedList<>();

        CldsSdcResource sdcResource1a = new CldsSdcResource();
        sdcResource1a.setResourceInstanceName("resource1");
        sdcResource1a.setResourceVersion("1.0");
        rawCldsSdcResourceList.add(sdcResource1a);

        CldsSdcResource sdcResource1b = new CldsSdcResource();
        sdcResource1b.setResourceInstanceName("resource1");
        sdcResource1b.setResourceVersion("1.1");
        rawCldsSdcResourceList.add(sdcResource1b);

        CldsSdcResource sdcResource1c = new CldsSdcResource();
        sdcResource1c.setResourceInstanceName("resource1");
        sdcResource1c.setResourceVersion("1.2");
        rawCldsSdcResourceList.add(sdcResource1c);

        CldsSdcResource sdcResource2 = new CldsSdcResource();
        sdcResource2.setResourceInstanceName("resource2");
        sdcResource2.setResourceVersion("1.0");
        rawCldsSdcResourceList.add(sdcResource2);

        SdcCatalogServices catalogServices = new SdcCatalogServices();
        List<CldsSdcResource> resultList = catalogServices.removeDuplicateSdcResourceInstances(rawCldsSdcResourceList);

        CldsSdcResource res1;
        CldsSdcResource res2;
        if ("resource1".equals(resultList.get(0).getResourceInstanceName())) {
            res1 = resultList.get(0);
            res2 = resultList.get(1);
        } else {
            res1 = resultList.get(1);
            res2 = resultList.get(0);
        }

        assertTrue("resource1".equals(res1.getResourceInstanceName()));
        assertTrue("1.2".equals(res1.getResourceVersion()));

        assertTrue("resource2".equals(res2.getResourceInstanceName()));
        assertTrue("1.0".equals(res2.getResourceVersion()));

    }

    @Test
    public void removeDuplicateSdcResourceBasicInfoTest() {

        List<CldsSdcResourceBasicInfo> rawCldsSdcResourceList = new LinkedList<>();

        CldsSdcResourceBasicInfo sdcResource1a = new CldsSdcResourceBasicInfo();
        sdcResource1a.setName("resource1");
        sdcResource1a.setVersion("1.0");
        rawCldsSdcResourceList.add(sdcResource1a);

        CldsSdcResourceBasicInfo sdcResource1b = new CldsSdcResourceBasicInfo();
        sdcResource1b.setName("resource1");
        sdcResource1b.setVersion("1.1");
        rawCldsSdcResourceList.add(sdcResource1b);

        CldsSdcResourceBasicInfo sdcResource1c = new CldsSdcResourceBasicInfo();
        sdcResource1c.setName("resource1");
        sdcResource1c.setVersion("1.2");
        rawCldsSdcResourceList.add(sdcResource1c);

        CldsSdcResourceBasicInfo sdcResource2 = new CldsSdcResourceBasicInfo();
        sdcResource2.setName("resource2");
        sdcResource2.setVersion("1.0");
        rawCldsSdcResourceList.add(sdcResource2);

        SdcCatalogServices catalogServices = new SdcCatalogServices();
        List<CldsSdcResourceBasicInfo> resultList = catalogServices
                .removeDuplicateSdcResourceBasicInfo(rawCldsSdcResourceList);

        CldsSdcResourceBasicInfo res1;
        CldsSdcResourceBasicInfo res2;
        if ("resource1".equals(resultList.get(0).getName())) {
            res1 = resultList.get(0);
            res2 = resultList.get(1);
        } else {
            res1 = resultList.get(1);
            res2 = resultList.get(0);
        }

        assertTrue("resource1".equals(res1.getName()));
        assertTrue("1.2".equals(res1.getVersion()));

        assertTrue("resource2".equals(res2.getName()));
        assertTrue("1.0".equals(res2.getVersion()));

    }

    @Test
    public void getServiceUuidFromServiceInvariantIdTest() throws Exception {
        SdcCatalogServices spy = Mockito.spy(sdcCatalogWired);

        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcServicesListExample.json"),
                "UTF-8")).when(spy).getSdcServicesInformation(null);
        // Try the vcts4 version 1.0, this one should be replaced by 1.1 so it
        // should not exist, returning empty string
        String resUuidVcts4Null = spy.getServiceUuidFromServiceInvariantId("a33ed748-3477-4434-b3f3-b5560f5e7d9b");
        assertTrue("".equals(resUuidVcts4Null));

        // Try the vcts4 version 1.1, this one should be there as it replaces
        // the vcts4 v1.0
        String resUuidVcts4Latest = spy.getServiceUuidFromServiceInvariantId("a33ed748-3477-4434-b3f3-b5560f5e7d9c");
        assertTrue("29018914-966c-442d-9d08-251b9dc45b8f".equals(resUuidVcts4Latest));

        // Try the vcts5 version 1.0, this one should be there
        String resUuidVcts5 = spy.getServiceUuidFromServiceInvariantId("a33ed748-3477-4434-b3f3-b5560f5e7d8c");
        assertTrue("29018914-966c-442d-9d08-251b9dc45b7f".equals(resUuidVcts5));

        // try one that does not exist at all
        String resUuidUnknown = spy.getServiceUuidFromServiceInvariantId("testuuid");
        assertTrue("".equals(resUuidUnknown));

    }

    @Test
    public void getCldsServiceDataWithAlarmConditionsTest() throws Exception {
        SdcCatalogServices spy = Mockito.spy(sdcCatalogWired);

        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcServicesListExample.json"),
                "UTF-8")).when(spy).getSdcServicesInformation(null);

        // This invariant uuid is the one from vcts4 v1.1
        String serviceResourceDetailUrl = refProp.getStringValue("sdc.serviceUrl")
                + "/29018914-966c-442d-9d08-251b9dc45b8f/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcServiceDetailsExample.json"),
                "UTF-8")).when(spy).getCldsServicesOrResourcesBasedOnURL(serviceResourceDetailUrl);

        String resourceDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/585822c7-4027-4f84-ba50-e9248606f136/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcResourceDetailsExample.json"),
                "UTF-8")).when(spy).getCldsServicesOrResourcesBasedOnURL(resourceDetailUrl);

        String securityRulesDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/d57e57d2-e3c6-470d-8d16-e6ea05f536c5/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcSecurityRules.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(securityRulesDetailUrl);

        String cinderVolumeDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/b4288e07-597a-44a2-aa98-ad36e551a39d/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcCinderVolume.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(cinderVolumeDetailUrl);

        String vfcGenericDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/2c8f1219-8000-4001-aa13-496a0396d40f/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcVFCGenericWithAlarms.json"),
                "UTF-8")).when(spy).getCldsServicesOrResourcesBasedOnURL(vfcGenericDetailUrl);

        String csvAlarmsDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/2c8f1219-8000-4001-aa13-496a0396d40f/resourceInstances/virc_fe_be/artifacts/5138e316-0237-49aa-817a-b3d8eaf77392";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcAlarmsList.csv"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(csvAlarmsDetailUrl);

        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcAlarmsList.csv"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(csvAlarmsDetailUrl);

        String csvAlarmsDetailUrl2 = refProp.getStringValue("sdc.catalog.url")
                + "resources/d7646638-2572-4a94-b497-c028ac15f9ca/artifacts/5138e316-0237-49aa-817a-b3d8eaf77392";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcAlarmsList.csv"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(csvAlarmsDetailUrl2);

        String allVfResourcesDetailUrl = refProp.getStringValue("sdc.catalog.url") + "resources?resourceType=VF";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcVFResources.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(allVfResourcesDetailUrl);

        String vfcResourcesDetailUrl = refProp.getStringValue("sdc.catalog.url")
                + "resources/a0475018-1e7e-4ddd-8bee-33cbf958c2e6/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcCVFCResourceExample.json"),
                "UTF-8")).when(spy).getCldsServicesOrResourcesBasedOnURL(vfcResourcesDetailUrl);

        String allVfcResourcesDetailUrl = refProp.getStringValue("sdc.catalog.url") + "resources?resourceType=VFC";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcVFCResources.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(allVfcResourcesDetailUrl);

        String allCvfcResourcesDetailUrl = refProp.getStringValue("sdc.catalog.url") + "resources?resourceType=CVFC";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcCVFCResources.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(allCvfcResourcesDetailUrl);

        String allVfAlarms = refProp.getStringValue("sdc.catalog.url")
                + "resources/84855843-5247-4e97-a2bd-5395a510253b/artifacts/d57ac7ec-f3c3-4793-983a-c75ac3a43153";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcMeasurementsList.csv"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(allVfAlarms);

        String vfcResourceExample = refProp.getStringValue("sdc.catalog.url")
                + "resources/d7646638-2572-4a94-b497-c028ac15f9ca/metadata";
        Mockito.doReturn(IOUtils.toString(
                SdcCatalogServicesItCase.class.getResourceAsStream("/example/sdc/sdcVFCResourceExample.json"), "UTF-8"))
                .when(spy).getCldsServicesOrResourcesBasedOnURL(vfcResourceExample);

        CldsServiceData cldsServiceData = spy
                .getCldsServiceDataWithAlarmConditions("a33ed748-3477-4434-b3f3-b5560f5e7d9c");
        assertTrue("a33ed748-3477-4434-b3f3-b5560f5e7d9c".equals(cldsServiceData.getServiceInvariantUUID()));
        assertTrue("29018914-966c-442d-9d08-251b9dc45b8f".equals(cldsServiceData.getServiceUUID()));
        assertTrue(cldsServiceData.getCldsVfs().size() == 1);

        List<CldsAlarmCondition> alarmsList = spy.getAllAlarmConditionsFromCldsServiceData(cldsServiceData,
                "alarmCondition");
        assertTrue(alarmsList.size() == 12);

    }

}
