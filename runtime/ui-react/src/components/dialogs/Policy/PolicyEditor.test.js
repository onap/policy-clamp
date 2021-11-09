/*-
 * ============LICENSE_START=======================================================
 * ONAP POLICY-CLAMP
 * ================================================================================
 * Copyright (C) 2021 AT&T Intellectual Property. All rights
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
import React from 'react';
import PolicyEditor from './PolicyEditor';
import { shallow, mount } from 'enzyme';
import PolicyToscaService from '../../../api/PolicyToscaService';

describe('Verify PolicyEditor', () => {
  const fs = require('fs');

  let toscaJson = fs.readFileSync('src/components/dialogs/Policy/toscaData.test.json', { encoding: 'utf8', flag: 'r' })

  const policyProperties = {
    "tca.policy": {
      "domain": "measurementsForVfScaling",
      "metricsPerEventName": [
        {
          "policyScope": "DCAE",
          "thresholds": [
            {
              "version": "1.0.2",
              "severity": "MAJOR",
              "thresholdValue": 200,
              "closedLoopEventStatus": "ONSET",
              "closedLoopControlName": "LOOP_test",
              "direction": "LESS_OR_EQUAL",
              "fieldPath": "$.event.measurementsForVfScalingFields.vNicPerformanceArray[*].receivedTotalPacketsDelta"
            }
          ],
          "eventName": "vLoadBalancer",
          "policyVersion": "v0.0.1",
          "controlLoopSchemaType": "VM",
          "policyName": "DCAE.Config_tca-hi-lo"
        }
      ]
    }
  };


  it('Test the render method', async () => {
    PolicyToscaService.getToscaPolicyModel = jest.fn().mockImplementation(() => {
      return Promise.resolve(toscaJson);
    });

    const component = mount(<PolicyEditor policyModelType="onap.policies.monitoring.tcagen2" policyModelTypeVersion="1.0.0"
                                          policyName="org.onap.new" policyVersion="1.0.0" policyProperties={ policyProperties }
                                          policiesTableUpdateFunction={ () => {
                                          } }/>);
    await PolicyToscaService.getToscaPolicyModel();
    expect(component).toMatchSnapshot();
  });
});
