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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onap.clamp.clds.AbstractIT;
import org.onap.clamp.clds.client.req.SdcReq;
import org.onap.clamp.clds.model.CldsAsdcServiceDetail;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

/**
 * Test ASDC API - stand alone (except for some config).
 * Replicates getAsdcServices and getAsdcServicesByUUID in the CldsService
 * Adds test of putting putting an artifact to VF.
 * TODO Also needs update and perhaps delete tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PropJsonBuilderIT extends AbstractIT {

    private String globalPropsPartial;
    private ObjectMapper mapper;

    @Before
    public void setUp() throws IOException {
        String url = refProp.getStringValue("asdc.serviceUrl");
        String catalogUrl = refProp.getStringValue("asdc.catalog.url");
        String basicAuth = SdcReq.getAsdcBasicAuth(refProp);
        System.out.println("value of string and basicAuth:" + url + basicAuth);
        CldsAsdcServiceDetail cldsservicedetail = new CldsAsdcServiceDetail();
        //	cldsservicedetail.set
        String globalProps = refProp.getStringValue("globalPropsTest");
        globalPropsPartial = refProp.getStringValue("globalPropsPartialTest");
        mapper = new ObjectMapper();
    }

    /**
     * List services from ASDC.
     * List meta data for a particular service from ASDC.
     * Test uploading artifact to a VF in ASDC.
     */
    @Test
    public void testAsdc() throws Exception {
//		String createEmptySharedObject = createEmptySharedObject();
//		System.out.println("value of emptySharedObject:" + createEmptySharedObject);
        sampleJsonObject();
        System.out.println(createTestEmptySharedObject());
    }

    private void sampleJsonObject() throws JsonProcessingException {
        ArrayNode arrayNode = mapper.createArrayNode();

        /**
         * Create three JSON Objects objectNode1, objectNode2, objectNode3
         * Add all these three objects in the array
         */

        ObjectNode objectNode1 = mapper.createObjectNode();
        objectNode1.put("bookName", "Java");
        objectNode1.put("price", "100");

        ObjectNode objectNode2 = mapper.createObjectNode();
        objectNode2.put("bookName", "Spring");
        objectNode2.put("price", "200");

        ObjectNode objectNode3 = mapper.createObjectNode();
        objectNode3.put("bookName", "Liferay");
        objectNode3.put("price", "500");

        /**
         * Array contains JSON Objects
         */
        arrayNode.add(objectNode1);
        arrayNode.add(objectNode2);
        arrayNode.add(objectNode3);

        /**
         * We can directly write the JSON in the console.
         * But it wont be pretty JSON String
         */
        System.out.println(arrayNode.toString());

        /**
         * To make the JSON String pretty use the below code
         */
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode));
    }

    private String createEmptySharedObject() throws JsonProcessingException {

        /**
         * "": {
         "vf": {
         "": ""
         },
         "location": {
         "": ""
         },
         "alarmCondition": {
         "": ""
         }
         }
         */
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        emptyObjectNode.put("", "");
        ObjectNode vfObjectNode = mapper.createObjectNode();
        vfObjectNode.putPOJO("vf", emptyObjectNode);
        ObjectNode locationObjectNode = mapper.createObjectNode();
        locationObjectNode.putPOJO("location", emptyObjectNode);
        ObjectNode alarmConditionObjectNode = mapper.createObjectNode();
        alarmConditionObjectNode.putPOJO("alarmCondition", emptyObjectNode);
        ObjectNode emptyServiceObjectNode = mapper.createObjectNode();
        ArrayNode samArrayNode = mapper.createArrayNode();
        samArrayNode.add(vfObjectNode);
        samArrayNode.add(locationObjectNode);
        samArrayNode.add(alarmConditionObjectNode);
        emptyServiceObjectNode.putPOJO("", samArrayNode);

        /**
         * "vf": {
         *			" ": " ",
         *			"DCAE_CLAMP_DEMO3 1": "DCAE_CLAMP_DEMO3"
         *        }
         *
         */
        ObjectNode vfObjectNode2 = mapper.createObjectNode();
        ObjectNode dcaeClampDemo3Node = mapper.createObjectNode();
        dcaeClampDemo3Node.put("DCAE_CLAMP_DEMO3", "DCAE_CLAMP_DEMO3");
        ArrayNode vfArrayNode = mapper.createArrayNode();
        vfArrayNode.add(emptyObjectNode);
        vfArrayNode.add(dcaeClampDemo3Node);
        vfObjectNode2.putPOJO("vf", vfArrayNode);

        /**
         * "location": {
         "SNDGCA64": "San Diego SAN3",
         "ALPRGAED": "Alpharetta PDK1",
         "LSLEILAA": "Lisle DPA3"
         },
         */
        ObjectNode locationObjectNode2 = mapper.createObjectNode();
        ObjectNode sandiegoLocationNode = mapper.createObjectNode();
        sandiegoLocationNode.put("SNDGCA64", "San Diego SAN3");
        ObjectNode alpharettaNode = mapper.createObjectNode();
        alpharettaNode.put("ALPRGAED", "Alpharetta PDK1");
        ArrayNode locationArrayNode = mapper.createArrayNode();
        locationArrayNode.add(emptyObjectNode);
        locationArrayNode.add(sandiegoLocationNode);
        locationArrayNode.add(alpharettaNode);
        locationObjectNode2.putPOJO("location", locationArrayNode);

        /**
         * "alarmCondition": {
         "A+Fallback+Operation+will+soon+be+started": "A Fallback Operation will soon be started",
         "BRM%2C+Auto+Export+Backup+Failed": "BRM, Auto Export Backup Failed",
         */
        ObjectNode alarmConditionObjectNode2 = mapper.createObjectNode();
        ObjectNode alamrCondition1 = mapper.createObjectNode();
        alamrCondition1.put("A+Fallback+Operation+will+soon+be+started", "A Fallback Operation will soon be started");
        ObjectNode alarmConditon2 = mapper.createObjectNode();
        alarmConditon2.put("BRM%2C+Scheduled+Backup+Failed", "BRM, Scheduled Backup Failed");
        ArrayNode alarmArrayNode = mapper.createArrayNode();
        alarmArrayNode.add(emptyObjectNode);
        alarmArrayNode.add(alamrCondition1);
        alarmArrayNode.add(alarmConditon2);
        alarmConditionObjectNode2.putPOJO("alarmCondition", alarmArrayNode);

        ArrayNode byServiceIdArrayNode = mapper.createArrayNode();
        byServiceIdArrayNode.add(vfObjectNode2);
        byServiceIdArrayNode.add(locationObjectNode2);
        byServiceIdArrayNode.add(alarmConditionObjectNode2);

        ObjectNode byServiceIdNode = mapper.createObjectNode();
        byServiceIdNode.putPOJO("c989a551-69f7-4b30-b10a-2e85bb227c30", byServiceIdArrayNode);

        ArrayNode byServiceBasicArrayNode = mapper.createArrayNode();
        byServiceBasicArrayNode.add(emptyServiceObjectNode);
        byServiceBasicArrayNode.add(byServiceIdNode);

        ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();

        byServiceBasicObjetNode.putPOJO("byService", byServiceBasicArrayNode);

        /**
         * "byVf": {
         "": {
         "vfc": {
         "": ""
         },
         "03596c12-c7e3-44b7-8994-5cdfeda8afdd": {
         "vfc": {
         " ": " "
         }
         }
         }
         }
         */

        ObjectNode byVfCBasicNode = mapper.createObjectNode();
        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode vfcIdObjectNode = mapper.createObjectNode();
        vfcIdObjectNode.putPOJO("03596c12-c7e3-44b7-8994-5cdfeda8afdd", vfCObjectNode);
        ArrayNode emptyvfcArrayNode = mapper.createArrayNode();
        emptyvfcArrayNode.add(vfCObjectNode);
        emptyvfcArrayNode.add(vfcIdObjectNode);
        emptyvfcobjectNode.putPOJO("", emptyvfcArrayNode);

        byVfCBasicNode.putPOJO("byVf", emptyvfcobjectNode);

        ArrayNode finalSharedArrayObject = mapper.createArrayNode();

        finalSharedArrayObject.add(byServiceBasicObjetNode);
        finalSharedArrayObject.add(byVfCBasicNode);

        ObjectNode finalSharedObjectNode = mapper.createObjectNode();
        finalSharedObjectNode.putPOJO("shared", finalSharedArrayObject);

        System.out.println("value :" + finalSharedObjectNode.toString());
        String testFinal = finalSharedObjectNode.toString();
        testFinal = testFinal.replaceFirst("\\{", ",");
        return globalPropsPartial + testFinal;
    }

    private String createTestEmptySharedObject() throws IOException {
        String locationStringValue = refProp.getStringValue("ui.location.default");
        String alarmStringValue = refProp.getStringValue("ui.alarm.default");

        ObjectNode locationJsonNode = (ObjectNode) mapper.readValue(locationStringValue, JsonNode.class);
        ObjectNode alarmStringJsonNode = (ObjectNode) mapper.readValue(alarmStringValue, JsonNode.class);
        /**
         * "": {
         "vf": {
         "": ""
         },
         "location": {
         "": ""
         },
         "alarmCondition": {
         "": ""
         }
         }
         */
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        emptyObjectNode.put("", "");
        ObjectNode vfObjectNode = mapper.createObjectNode();
        vfObjectNode.putPOJO("vf", emptyObjectNode);
        vfObjectNode.putPOJO("location", emptyObjectNode);
        vfObjectNode.putPOJO("alarmCondition", emptyObjectNode);
        ObjectNode emptyServiceObjectNode = mapper.createObjectNode();
        emptyServiceObjectNode.putPOJO("", vfObjectNode);

        /**
         * "vf": {
         *			" ": " ",
         *			"DCAE_CLAMP_DEMO3 1": "DCAE_CLAMP_DEMO3"
         *        }
         *
         */
        ObjectNode vfObjectNode2 = mapper.createObjectNode();
        ObjectNode dcaeClampDemo3Node = mapper.createObjectNode();
        dcaeClampDemo3Node.put("", "");
        dcaeClampDemo3Node.put("DCAE_CLAMP_DEMO3", "DCAE_CLAMP_DEMO3");
        vfObjectNode2.putPOJO("vf", dcaeClampDemo3Node);

        /**
         * "location": {
         "SNDGCA64": "San Diego SAN3",
         "ALPRGAED": "Alpharetta PDK1",
         "LSLEILAA": "Lisle DPA3"
         },
         */
//		ObjectNode sandiegoLocationNode = mapper.createObjectNode();
//		sandiegoLocationNode.put("SNDGCA64","San Diego SAN3");
//		sandiegoLocationNode.put("ALPRGAED","Alpharetta PDK1");	
        vfObjectNode2.putPOJO("location", locationJsonNode);

        /**
         * "alarmCondition": {
         "A+Fallback+Operation+will+soon+be+started": "A Fallback Operation will soon be started",
         "BRM%2C+Auto+Export+Backup+Failed": "BRM, Auto Export Backup Failed",
         */
//		ObjectNode alamrCondition1 = mapper.createObjectNode();
//		alamrCondition1.put("A+Fallback+Operation+will+soon+be+started","A Fallback Operation will soon be started");
//		alamrCondition1.put("BRM%2C+Scheduled+Backup+Failed","BRM, Scheduled Backup Failed");
        vfObjectNode2.putPOJO("alarmCondition", alarmStringJsonNode);
        emptyServiceObjectNode.putPOJO("c989a551-69f7-4b30-b10a-2e85bb227c30", vfObjectNode2);
        ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();
        byServiceBasicObjetNode.putPOJO("byService", emptyServiceObjectNode);

        /**
         * "byVf": {
         "": {
         "vfc": {
         "": ""
         },
         "03596c12-c7e3-44b7-8994-5cdfeda8afdd": {
         "vfc": {
         " ": " "
         }
         }
         }
         }
         */

        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode subVfCObjectNode = mapper.createObjectNode();
        subVfCObjectNode.putPOJO("vfc", emptyObjectNode);
        vfCObjectNode.putPOJO("03596c12-c7e3-44b7-8994-5cdfeda8afdd", subVfCObjectNode);
        emptyvfcobjectNode.putPOJO("", vfCObjectNode);
        byServiceBasicObjetNode.putPOJO("byVf", emptyvfcobjectNode);

        ObjectNode readTree = (ObjectNode) mapper.readValue(globalPropsPartial, JsonNode.class);

        readTree.putPOJO("shared", byServiceBasicObjetNode);
        System.out.println("valuie of objNode:" + readTree);
        return readTree.toString();
    }

    private String createCldsSharedObject(CldsAsdcServiceDetail cldsAsdcServiceDetail) throws IOException {
        /**
         * "": {
         "vf": {
         "": ""
         },
         "location": {
         "": ""
         },
         "alarmCondition": {
         "": ""
         }
         }
         */
        ObjectNode emptyObjectNode = mapper.createObjectNode();
        emptyObjectNode.put("", "");
        ObjectNode vfObjectNode = mapper.createObjectNode();
        vfObjectNode.putPOJO("vf", emptyObjectNode);
        vfObjectNode.putPOJO("location", emptyObjectNode);
        vfObjectNode.putPOJO("alarmCondition", emptyObjectNode);
        ObjectNode emptyServiceObjectNode = mapper.createObjectNode();
        emptyServiceObjectNode.putPOJO("", vfObjectNode);

        /**
         * "vf": {
         *			" ": " ",
         *			"DCAE_CLAMP_DEMO3 1": "DCAE_CLAMP_DEMO3"
         *        }
         *
         */
        ObjectNode vfObjectNode2 = mapper.createObjectNode();
        ObjectNode dcaeClampDemo3Node = mapper.createObjectNode();
        dcaeClampDemo3Node.put("", "");
        dcaeClampDemo3Node.put("DCAE_CLAMP_DEMO3", "DCAE_CLAMP_DEMO3");
        vfObjectNode2.putPOJO("vf", dcaeClampDemo3Node);

        /**
         * "location": {
         "SNDGCA64": "San Diego SAN3",
         "ALPRGAED": "Alpharetta PDK1",
         "LSLEILAA": "Lisle DPA3"
         },
         */
        ObjectNode sandiegoLocationNode = mapper.createObjectNode();
        sandiegoLocationNode.put("SNDGCA64", "San Diego SAN3");
        sandiegoLocationNode.put("ALPRGAED", "Alpharetta PDK1");
        vfObjectNode2.putPOJO("location", sandiegoLocationNode);

        /**
         * "alarmCondition": {
         "A+Fallback+Operation+will+soon+be+started": "A Fallback Operation will soon be started",
         "BRM%2C+Auto+Export+Backup+Failed": "BRM, Auto Export Backup Failed",
         */
        ObjectNode alamrCondition1 = mapper.createObjectNode();
        alamrCondition1.put("A+Fallback+Operation+will+soon+be+started", "A Fallback Operation will soon be started");
        alamrCondition1.put("BRM%2C+Scheduled+Backup+Failed", "BRM, Scheduled Backup Failed");
        vfObjectNode2.putPOJO("alarmCondition", alamrCondition1);
        emptyServiceObjectNode.putPOJO("c989a551-69f7-4b30-b10a-2e85bb227c30", vfObjectNode2);
        ObjectNode byServiceBasicObjetNode = mapper.createObjectNode();
        byServiceBasicObjetNode.putPOJO("byService", emptyServiceObjectNode);

        /**
         * "byVf": {
         "": {
         "vfc": {
         "": ""
         },
         "03596c12-c7e3-44b7-8994-5cdfeda8afdd": {
         "vfc": {
         " ": " "
         }
         }
         }
         }
         */

        ObjectNode emptyvfcobjectNode = mapper.createObjectNode();
        ObjectNode vfCObjectNode = mapper.createObjectNode();
        vfCObjectNode.putPOJO("vfC", emptyObjectNode);
        ObjectNode subVfCObjectNode = mapper.createObjectNode();
        subVfCObjectNode.putPOJO("vfc", emptyObjectNode);
        vfCObjectNode.putPOJO("03596c12-c7e3-44b7-8994-5cdfeda8afdd", subVfCObjectNode);
        emptyvfcobjectNode.putPOJO("", vfCObjectNode);
        byServiceBasicObjetNode.putPOJO("byVf", emptyvfcobjectNode);

        ObjectNode readTree = (ObjectNode) mapper.readValue(globalPropsPartial, JsonNode.class);

        readTree.putPOJO("shared", byServiceBasicObjetNode);
        System.out.println("valuie of objNode:" + readTree);
        return readTree.toString();
    }
}
