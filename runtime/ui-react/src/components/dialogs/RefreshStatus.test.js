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
import RefreshStatus from './RefreshStatus';
import LoopCache from '../../api/LoopCache';
import LoopActionService from '../../api/LoopActionService';

describe('Verify RefreshStatus', () => {

  const loopCache = new LoopCache({
    "name": "LOOP_Jbv1z_v1_0_ResourceInstanceName1_tca"
  });

  it('Test refresh status failed', async () => {
    const flushPromises = () => new Promise(setImmediate);
    const historyMock = { push: jest.fn() };
    const showSucAlert = jest.fn();
    const showFailAlert = jest.fn();

    const component = shallow(<RefreshStatus loopCache={ loopCache } history={ historyMock } showSucAlert={ showSucAlert } showFailAlert={ showFailAlert }/>)
    await flushPromises();
    component.update();

    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
  });

  it('Test refresh status successful', async () => {
    const flushPromises = () => new Promise(setImmediate);
    const historyMock = { push: jest.fn() };
    const updateLoopFunction = jest.fn();
    const showSucAlert = jest.fn();
    const showFailAlert = jest.fn();

    LoopActionService.refreshStatus = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
        }
      });
    });

    const component = shallow(<RefreshStatus loopCache={ loopCache }
                                             loopAction="submit" history={ historyMock } updateLoopFunction={ updateLoopFunction } showSucAlert={ showSucAlert } showFailAlert={ showFailAlert }/>)
    await flushPromises();
    component.update();

    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
  });

});
