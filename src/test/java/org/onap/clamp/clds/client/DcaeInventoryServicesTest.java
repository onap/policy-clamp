/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Huawei Technologies Co., Ltd.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 * ================================================================================
 *
 */

package org.onap.clamp.clds.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.onap.clamp.clds.client.DcaeInventoryServices.DCAE_INVENTORY_RETRY_INTERVAL;
import static org.onap.clamp.clds.client.DcaeInventoryServices.DCAE_INVENTORY_RETRY_LIMIT;
import static org.onap.clamp.clds.client.DcaeInventoryServices.DCAE_INVENTORY_URL;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.clamp.clds.config.ClampProperties;
import org.onap.clamp.clds.model.dcae.DcaeInventoryResponse;
import org.onap.clamp.clds.model.dcae.DcaeLinks;
import org.onap.clamp.clds.model.dcae.DcaeOperationStatusResponse;
import org.onap.clamp.util.HttpConnectionManager;


@RunWith(MockitoJUnitRunner.class)
public class DcaeInventoryServicesTest {

    @Mock
    private HttpConnectionManager httpConnectionManager;

    @Mock
    private ClampProperties properties;

    private static final String resourceUuid = "023a3f0d-1161-45ff-b4cf-8918a8ccf3ad";
    private static final String serviceUuid = "4cc5b45a-1f63-4194-8100-cd8e14248c92";
    private static final String artifactName = "tca_2.yaml";
    private static final String queryString = "?asdcResourceId=" + resourceUuid + "&asdcServiceId=" + serviceUuid
            + "&typeName=" + artifactName;
    private static final String url = "http://localhost:8085" + "/dcae-service-types" + queryString;

    @Test
    public void testDcaeInventoryResponse() throws ParseException, InterruptedException, IOException {
        when(properties.getStringValue(DCAE_INVENTORY_URL)).thenReturn("http://localhost:8085");
        when(properties.getStringValue(DCAE_INVENTORY_RETRY_LIMIT)).thenReturn("1");
        when(properties.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL)).thenReturn("100");
        String responseStr = "{\"totalCount\":1, "
                + "\"items\":[{\"typeId\":\"typeId-32147723-d323-48f9-a325-bcea8d728025\","
                + " \"typeName\":\"typeName-32147723-d323-48f9-a325-bcea8d728025\"}]}";
        when(httpConnectionManager.doHttpRequest(url, "GET", null, null,
                                                 "DCAE", null, null))
                .thenReturn(responseStr);

        DcaeInventoryServices services = new DcaeInventoryServices(properties,
                                                                   httpConnectionManager);
        DcaeInventoryResponse response = services.getDcaeInformation(artifactName, serviceUuid, resourceUuid);
        assertThat(response.getTypeId(),is("typeId-32147723-d323-48f9-a325-bcea8d728025"));
        assertThat(response.getTypeName(),is("typeName-32147723-d323-48f9-a325-bcea8d728025"));
    }

    @Test
    public void testDcaeInventoryResponseWithZeroCount() throws ParseException, InterruptedException, IOException {
        when(properties.getStringValue(DCAE_INVENTORY_URL)).thenReturn("http://localhost:8085");
        when(properties.getStringValue(DCAE_INVENTORY_RETRY_LIMIT)).thenReturn("1");
        when(properties.getStringValue(DCAE_INVENTORY_RETRY_INTERVAL)).thenReturn("100");
        when(httpConnectionManager.doHttpRequest(url, "GET", null, null,
                                                 "DCAE", null, null))
                .thenReturn("{\"totalCount\":0}\"}]}");
        DcaeInventoryServices services = new DcaeInventoryServices(properties,
                                                                   httpConnectionManager);
        DcaeInventoryResponse response = services.getDcaeInformation(artifactName, serviceUuid, resourceUuid);
        assertThat(response, nullValue());
    }

    @Test
    public void testDcaeInventoryResponsePojo() {
        DcaeInventoryResponse response = new DcaeInventoryResponse();
        response.setTypeId("typeId-32147723-d323-48f9-a325-bcea8d728025");
        response.setTypeName("typeName-32147723-d323-48f9-a325-bcea8d728025");
        assertThat(response.getTypeId(),is("typeId-32147723-d323-48f9-a325-bcea8d728025"));
        assertThat(response.getTypeName(),is("typeName-32147723-d323-48f9-a325-bcea8d728025"));
    }

    @Test
    public void testDcaeOperationStatusResponsePojo() {
        DcaeLinks links = new DcaeLinks();
        links.setSelf("selfUrl");
        links.setStatus("state");
        links.setUninstall("uninstallUrl");
        DcaeOperationStatusResponse response = new DcaeOperationStatusResponse();
        response.setRequestId("testId");
        response.setError("errorMessage");
        response.setLinks(links);
        response.setOperationType("install");
        response.setStatus("state");
        assertThat(response.getRequestId(),is("testId"));
        assertThat(response.getError(),is("errorMessage"));
        assertThat(response.getOperationType(),is("install"));
        assertThat(response.getStatus(),is("state"));
        assertThat(response.getLinks().getSelf(),is("selfUrl"));
        assertThat(response.getLinks().getStatus(),is("state"));
        assertThat(response.getLinks().getUninstall(),is("uninstallUrl"));
    }
}