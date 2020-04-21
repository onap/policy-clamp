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
import { shallow } from 'enzyme';
import LoopSvg from './LoopSvg';
import LoopCache from '../../../api/LoopCache';
import LoopService from '../../../api/LoopService';

describe('Verify LoopSvg', () => {
    const loopCache = new LoopCache({
        "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
        "microServicePolicies": [{
            "name": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca",
            "modelType": "onap.policies.monitoring.cdap.tca.hi.lo.app",
            "properties": {"domain": "measurementsForVfScaling"},
            "shared": false,
            "jsonRepresentation": {"schema": {}}
        }],
        "operationalPolicies": [{
            "name": "OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca",
            "configurationsJson": {
                "operational_policy": {
                    "controlLoop": {},
                    "policies": []
                }
            }
        }]
    });

    it('Test the render method no loopName', () => {
        const localLoopCache = new LoopCache({
        "microServicePolicies": [{
            "name": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca",
            "modelType": "onap.policies.monitoring.cdap.tca.hi.lo.app",
            "properties": {"domain": "measurementsForVfScaling"},
            "shared": false,
            "jsonRepresentation": {"schema": {}}
          }]
        });
        const component = shallow(
            <LoopSvg.WrappedComponent loopCache={localLoopCache}/>
        );

        expect(component).toMatchSnapshot();
    });

    it('Test the render method', () => {
        const component = shallow(
            <LoopSvg.WrappedComponent  loopCache={loopCache}/>
        );

        expect(component).toMatchSnapshot();
    });

    it('Test the render method svg not empty', async () => {
        const flushPromises = () => new Promise(setImmediate);
        LoopService.getSvg = jest.fn().mockImplementation(() => {
            return Promise.resolve("<svg><text test</text></svg>");
        });
        const component = shallow(
            <LoopSvg.WrappedComponent  loopCache={loopCache}/>
        );
        await flushPromises();
        expect(component).toMatchSnapshot();
    });

    it('Test handleSvgClick', () => {
        const historyMock = { push: jest.fn() };

        const component = shallow(
            <LoopSvg.WrappedComponent loopCache={loopCache} history={historyMock}/>
        );
        let dummyElement = document.createElement('div');
        dummyElement.setAttribute("data-element-id","TCA_h2NMX_v1_0_ResourceInstanceName1_tca");

        const event = { target: { parentNode: { parentNode:{ parentNode: dummyElement }}}};

        component.simulate('click', event);
        component.update();

        expect(historyMock.push.mock.calls[0]).toEqual([ '/policyModal/MICRO-SERVICE-POLICY/TCA_h2NMX_v1_0_ResourceInstanceName1_tca']);

        //click operational policy
        dummyElement.setAttribute("data-element-id","OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca");
        const event2 = { target: { parentNode: { parentNode:{ parentNode: dummyElement }}}};

        component.simulate('click', event2);
        component.update();

        expect(historyMock.push.mock.calls[1]).toEqual([ '/policyModal/OPERATIONAL-POLICY/OPERATIONAL_h2NMX_v1_0_ResourceInstanceName1_tca']);
    });

    it('Test componentWillReceiveProps method', () => {
        const localLoopCache = new LoopCache({
        "microServicePolicies": [{
            "name": "TCA_h2NMX_v1_0_ResourceInstanceName1_tca",
            "modelType": "onap.policies.monitoring.cdap.tca.hi.lo.app",
            "properties": {"domain": "measurementsForVfScaling"},
            "shared": false,
            "jsonRepresentation": {"schema": {}}
          }]
        });
        const component = shallow(
            <LoopSvg.WrappedComponent loopCache={localLoopCache}/>
        );

        expect(component.state('componentModalMapping').size).toEqual(1);

        component.setProps({loopCache: loopCache});
        expect(component.state('componentModalMapping').size).toEqual(2);
    });
});
