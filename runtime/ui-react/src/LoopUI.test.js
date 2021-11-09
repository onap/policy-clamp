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
import LoopUI from './LoopUI';
import OnapConstants from './utils/OnapConstants';

import LoopCache from './api/LoopCache';
import LoopActionService from './api/LoopActionService';
import LoopService from './api/LoopService';

describe('Verify LoopUI', () => {
  beforeEach(() => {
    fetch.resetMocks();
    fetch.mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => "testUser"

      });
    });
  })

  const loopCache = new LoopCache({
    "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
    "components": {
      "POLICY": {
        "componentState": {
          "stateName": "UNKNOWN",
          "description": "The policies defined have NOT yet been created on the policy engine"
        }
      },
      "DCAE": {
        "componentState": {
          "stateName": "BLUEPRINT_DEPLOYED",
          "description": "The DCAE blueprint has been found in the DCAE inventory but not yet instancianted for this loop"
        }
      }
    }
  });

  it('Test the render method', async () => {
    const flushPromises = () => new Promise(setImmediate);

    const component = shallow(<LoopUI/>)
    component.setState({
      loopName: "testLoopName",
      showSucAlert: false,
      showFailAlert: false
    });
    await flushPromises();
    expect(component).toMatchSnapshot();
  });

  test('Test closeLoop method', () => {
    const historyMock = { push: jest.fn() };
    const component = shallow(<LoopUI history={ historyMock }/>)
    const instance = component.instance();
    instance.closeLoop();

    expect(component.state('loopName')).toEqual(OnapConstants.defaultLoopName);
    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
  })

  test('Test loadLoop method refresh suc', async () => {
    const historyMock = { push: jest.fn() };
    LoopService.getLoop = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        text: () => {
        }
      });
    });

    LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
      return Promise.resolve({ name: "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca" });
    });

    const flushPromises = () => new Promise(setImmediate);
    const component = shallow(<LoopUI history={ historyMock }/>)
    const instance = component.instance();
    instance.loadLoop("LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca");

    await flushPromises();

    const resLoopCache = instance.getLoopCache();

    expect(resLoopCache.getComponentStates()).toBeUndefined();
    expect(component.state('loopName')).toEqual("LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca");
  })

  test('Test loadLoop method refresh fail', async () => {
    const historyMock = { push: jest.fn() };
    LoopService.getLoop = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        name: "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca",
        "components": {
          "POLICY": {
            "componentState": {
              "stateName": "UNKNOWN",
              "description": "The policies defined have NOT yet been created on the policy engine"
            }
          },
          "DCAE": {
            "componentState": {
              "stateName": "BLUEPRINT_DEPLOYED",
              "description": "The DCAE blueprint has been found in the DCAE inventory but not yet instancianted for this loop"
            }
          }
        }
      });
    });

    LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
      return Promise.reject({ error: "whatever" });
    });

    const flushPromises = () => new Promise(setImmediate);
    const component = shallow(<LoopUI history={ historyMock }/>)
    const instance = component.instance();
    instance.loadLoop("LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca");

    await flushPromises();

    const resLoopCache = instance.getLoopCache();

    expect(resLoopCache).toEqual(loopCache);
    expect(component.state('loopName')).toEqual("LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca");
  })

  test('Test alert methods', () => {
    const component = shallow(<LoopUI/>)
    expect(component.state('showSucAlert')).toEqual(false);

    const instance = component.instance();
    instance.showSucAlert("testAlert");
    expect(component.state('showSucAlert')).toEqual(true);
    expect(component.state('showFailAlert')).toEqual(false);
    expect(component.state('showMessage')).toEqual("testAlert");

    instance.disableAlert();

    expect(component.state('showSucAlert')).toEqual(false);
    expect(component.state('showFailAlert')).toEqual(false);

    instance.showFailAlert("testAlert2");
    expect(component.state('showSucAlert')).toEqual(false);
    expect(component.state('showFailAlert')).toEqual(true);
    expect(component.state('showMessage')).toEqual("testAlert2");
  })
});
