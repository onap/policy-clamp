/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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
import { mount } from 'enzyme';
import ModifyLoopModal from './ModifyLoopModal';
import LoopCache from '../../../api/LoopCache';
import LoopService from '../../../api/LoopService';
import PolicyToscaService from '../../../api/PolicyToscaService';

describe('Verify ModifyLoopModal', () => {
    beforeEach(() => {
        PolicyToscaService.getToscaPolicyModels = jest.fn().mockImplementation(() => {
            return Promise.resolve([{
                "policyModelType":"test",
                "policyAcronym":"test",
                "version":"1.0.0",
                "updatedBy":"",
                "updatedDate":""
            }]);
        });
        PolicyToscaService.getToscaPolicyModelYaml = jest.fn().mockImplementation(() => {
            return Promise.resolve("OK");
        });
    })

    const loopCache = new LoopCache({
            "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
            "microServicePolicies": [{
                "name": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca",
                "modelType": "onap.policies.monitoring.cdap.tca.hi.lo.app",
                "properties": {"domain": "measurementsForVfScaling"},
                "shared": false,
                "jsonRepresentation": {"schema": {}}
            }],
            "globalPropertiesJson": {
                "dcaeDeployParameters": {
                    "testMs": {
                    "location_id": "",
                    "policy_id": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca"
                }
            }
        }
    });
    const historyMock = { push: jest.fn() };
    const flushPromises = () => new Promise(setImmediate);

    it('Test handleClose', () => {
        const handleClose = jest.spyOn(ModifyLoopModal.prototype,'handleClose');
        const component = mount(<ModifyLoopModal history={historyMock} loopCache={loopCache}/>)

        component.find('[variant="secondary"]').get(0).props.onClick();

        expect(handleClose).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(false);
        expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test getToscaPolicyModelYaml',  async () => {
        const flushPromises = () => new Promise(setImmediate);
        const component = mount(<ModifyLoopModal history={historyMock} loopCache={loopCache}/>)
        component.setState({ 
                "selectedRowData": {"tableData":{"id":0}}
        });
        const instance = component.instance();

        instance.getToscaPolicyModelYaml("","1.0.0");
        expect(component.state('content')).toEqual("Please select Tosca model to view the details");

        instance.getToscaPolicyModelYaml("test","1.0.0");
        await flushPromises();
        expect(component.state('content')).toEqual("OK");

        PolicyToscaService.getToscaPolicyModelYaml = jest.fn().mockImplementation(() => {
            return Promise.resolve("");
        });
        instance.getToscaPolicyModelYaml("test","1.0.0");
        await flushPromises();
        expect(component.state('content')).toEqual("No Tosca model Yaml available");
    });

    it('Test handleYamlContent',  async () => {
        const component = mount(<ModifyLoopModal loopCache={loopCache}/>)
        const instance = component.instance();

        const event = {"target":{"value":"testValue"}}
        instance.handleYamlContent(event);
        expect(component.state('content')).toEqual("testValue");
    });
});