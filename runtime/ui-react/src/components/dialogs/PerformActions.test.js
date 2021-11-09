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
import PerformActions from './PerformActions';
import LoopCache from '../../api/LoopCache';
import LoopActionService from '../../api/LoopActionService';

describe('Verify PerformActions', () => {

  const loopCache = new LoopCache({
    "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca"
  });

  it('Test the render method action failed', async () => {
    const flushPromises = () => new Promise(setImmediate);
    const historyMock = { push: jest.fn() };
    const updateLoopFunction = jest.fn();
    const showSucAlert = jest.fn();
    const showFailAlert = jest.fn();
    const setBusyLoading = jest.fn();
    const clearBusyLoading = jest.fn();

    LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
        }
      });
    });
    const component = shallow(<PerformActions loopCache={ loopCache }
                                              loopAction="submit" history={ historyMock } updateLoopFunction={ updateLoopFunction } showSucAlert={ showSucAlert } showFailAlert={ showFailAlert }
                                              setBusyLoading={ setBusyLoading } clearBusyLoading={ clearBusyLoading }/>)
    await flushPromises();
    component.update();

    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
  });

  it('Test the render method action successful', async () => {
    const flushPromises = () => new Promise(setImmediate);
    const historyMock = { push: jest.fn() };
    const updateLoopFunction = jest.fn();
    const showSucAlert = jest.fn();
    const showFailAlert = jest.fn();
    const setBusyLoading = jest.fn();
    const clearBusyLoading = jest.fn();

    LoopActionService.performAction = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
        }
      });
    });
    LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
        }
      });
    });
    const component = shallow(<PerformActions loopCache={ loopCache }
                                              loopAction="submit" history={ historyMock } updateLoopFunction={ updateLoopFunction } showSucAlert={ showSucAlert } showFailAlert={ showFailAlert }
                                              setBusyLoading={ setBusyLoading } clearBusyLoading={ clearBusyLoading }/>)
    await flushPromises();
    component.update();

    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
  });

});
