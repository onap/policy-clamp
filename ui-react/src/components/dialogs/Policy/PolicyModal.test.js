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
import PolicyModal from './PolicyModal';
import LoopCache from '../../../api/LoopCache';
import LoopService from '../../../api/LoopService';
import OnapConstant from '../../../utils/OnapConstants';

describe('Verify PolicyModal', () => {
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
    const loopCacheStr = {
            "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
            "operationalPolicies": [{
                "name": "OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca",
                "configurationsJson": {
                    "operational_policy": {
                        "controlLoop": {},
                        "policies": []
                    }
                },
                "policyModel": {"policyPdpGroup": {"supportedPdpGroups":[{"monitoring": ["xacml"]}]}},
                "jsonRepresentation" : {"schema": {}}
             }]
    };
    const loopCache = new LoopCache(loopCacheStr);
    const historyMock = { push: jest.fn() };
    const flushPromises = () => new Promise(setImmediate);
    const match = {params: {policyName:"OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca", policyInstanceType: OnapConstant.operationalPolicyType}}
        
    it('Test handleClose', () => {
      const handleClose = jest.spyOn(PolicyModal.prototype,'handleClose');
      const component = mount(<PolicyModal history={historyMock} match={match} loopCache={loopCache}/>)

      component.find('[variant="secondary"]').prop('onClick')();

      expect(handleClose).toHaveBeenCalledTimes(1);
      expect(component.state('show')).toEqual(false);
      expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test handleSave', async () => {
        const loadLoopFunction = jest.fn();
        const handleSave = jest.spyOn(PolicyModal.prototype,'handleSave');
        const component = mount(<PolicyModal history={historyMock} 
                          loopCache={loopCache} match={match} loadLoopFunction={loadLoopFunction} />)

        component.find('[variant="primary"]').get(0).props.onClick();
        await flushPromises();
        component.update();

        expect(handleSave).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(false);
        expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    });

    it('Test handleRefresh', async () => {
        LoopService.refreshOperationalPolicyJson = jest.fn().mockImplementation(() => {
            return Promise.resolve(loopCacheStr);
        });
        const updateLoopFunction = jest.fn();
        const handleRefresh = jest.spyOn(PolicyModal.prototype,'handleRefresh');
        const component = mount(<PolicyModal loopCache={loopCache} match={match} updateLoopFunction={updateLoopFunction} />)

        component.find('[variant="primary"]').get(1).props.onClick();
        await flushPromises();
        component.update();

        expect(handleRefresh).toHaveBeenCalledTimes(1);
        expect(component.state('show')).toEqual(true);
        expect(component.state('showSucAlert')).toEqual(true);
        expect(component.state('showMessage')).toEqual("Successfully refreshed");
    });

    it('Test handlePdpGroupChange', () => {
        const component = mount(<PolicyModal loopCache={loopCache} match={match} />)
        component.setState({ 
                "pdpGroup": [{"option1":["subPdp1","subPdp2"]}],
                "chosenPdpGroup": "option2"
        });
        expect(component.state('chosenPdpGroup')).toEqual("option2");

        const instance = component.instance();
        const event = {label:"option1", value:"option1"}
        instance.handlePdpGroupChange(event);
        expect(component.state('chosenPdpGroup')).toEqual("option1");
        expect(component.state('chosenPdpSubgroup')).toEqual("");
        expect(component.state('pdpSubgroupList')).toEqual([{label:"subPdp1", value:"subPdp1"}, {label:"subPdp2", value:"subPdp2"}]);
    });

    it('Test handlePdpSubgroupChange', () => {
        const component = mount(<PolicyModal loopCache={loopCache} match={match} />)

        const instance = component.instance();
        const event = {label:"option1", value:"option1"}
        instance.handlePdpSubgroupChange(event);
        expect(component.state('chosenPdpSubgroup')).toEqual("option1");
    });
});