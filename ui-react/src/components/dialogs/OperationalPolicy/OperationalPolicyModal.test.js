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
import OperationalPolicyModal from './OperationalPolicyModal';
import LoopCache from '../../../api/LoopCache';

describe('Verify OperationalPolicyModal', () => {
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
            "operationalPolicies": [{
                "name": "OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca",
                "configurationsJson": {
                    "operational_policy": {
                        "controlLoop": {},
                        "policies": []
                    }
                },
                "jsonRepresentation" : {"schema": {}}
             }]
    });
    const historyMock = { push: jest.fn() };
    const flushPromises = () => new Promise(setImmediate);

    it('Test handleClose', () => {
      const handleClose = jest.spyOn(OperationalPolicyModal.prototype,'handleClose');
      const component = mount(<OperationalPolicyModal history={historyMock} loopCache={loopCache}/>)

      component.find('[variant="secondary"]').get(0).props.onClick();

      expect(handleClose).toHaveBeenCalledTimes(1);
      expect(component.state('show')).toEqual(false);
      expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test handleSave', async () => {
        const loadLoopFunction = jest.fn();
        const handleSave = jest.spyOn(OperationalPolicyModal.prototype,'handleSave');
        const component = mount(<OperationalPolicyModal history={historyMock} 
                          loopCache={loopCache} loadLoopFunction={loadLoopFunction} />)

        component.find('[variant="primary"]').prop('onClick')();
        await flushPromises();
        component.update();

        expect(handleSave).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(false);
        expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test handleRefresh', async () => {
        const updateLoopFunction = jest.fn();
        const handleRefresh = jest.spyOn(OperationalPolicyModal.prototype,'handleRefresh');
        const component = mount(<OperationalPolicyModal loopCache={loopCache} updateLoopFunction={updateLoopFunction} />)

        component.find('[variant="secondary"]').get(1).props.onClick();
        await flushPromises();
        component.update();

        expect(handleRefresh).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(true);
    });
});