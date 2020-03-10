/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights
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
import ConfigurationPolicyModal from './ConfigurationPolicyModal';
import LoopCache from '../../../api/LoopCache';

describe('Verify ConfigurationPolicyModal', () => {
    beforeEach(() => {
        fetch.resetMocks();
        fetch.mockImplementation(() => {
            return Promise.resolve({
                ok: true,
                status: 200,
                text: () => "OK"
            });
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
        }]
    });
    const historyMock = { push: jest.fn() };
    const matchMock = { params:{ policyName: "TCA_h2NMX_v1_0_ResourceInstanceName1_tca" } } 
    const flushPromises = () => new Promise(setImmediate);
	
    it('Test handleClose', () => {
      const handleClose = jest.spyOn(ConfigurationPolicyModal.prototype,'handleClose');
      const component = mount(<ConfigurationPolicyModal history={historyMock} loopCache={loopCache} match={matchMock}/>)

      component.find('[variant="secondary"]').prop('onClick')();

      expect(handleClose).toHaveBeenCalledTimes(1);
      expect(component.state('show')).toEqual(false);
      expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test handleSave', async () => {
        const loadLoopFunction = jest.fn();
        const handleSave = jest.spyOn(ConfigurationPolicyModal.prototype,'handleSave');
        const component = mount(<ConfigurationPolicyModal history={historyMock} match={matchMock} loopCache={loopCache} loadLoopFunction={loadLoopFunction}/>)

        component.find('[variant="primary"]').prop('onClick')();
        await flushPromises();
        component.update();

        expect(handleSave).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(false);
        expect(component.state('policyName')).toEqual("TCA_h2NMX_v1_0_ResourceInstanceName1_tca");
        expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });
});