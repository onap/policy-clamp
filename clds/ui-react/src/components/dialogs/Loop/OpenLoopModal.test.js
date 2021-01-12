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
import OpenLoopModal from './OpenLoopModal';
import LoopService from '../../../api/LoopService';

describe('Verify OpenLoopModal', () => {

  beforeEach(() => {
      fetch.resetMocks();
      fetch.mockResponse(JSON.stringify([
        "LOOP_gmtAS_v1_0_ResourceInstanceName1_tca",
        "LOOP_gmtAS_v1_0_ResourceInstanceName1_tca_3",
        "LOOP_gmtAS_v1_0_ResourceInstanceName2_tca_2"
      ]));
  });

    it('Test the render method', () => {

    const component = shallow(<OpenLoopModal/>);
    expect(component).toMatchSnapshot();
  });

  it('Onchange event', async () => {
    const flushPromises = () => new Promise(setImmediate);
    LoopService.getLoop = jest.fn().mockImplementation(() => {
  		return Promise.resolve({
  			ok: true,
  			status: 200,
  			json: () => {}
  		});
  	});
    const event = {value: 'LOOP_gmtAS_v1_0_ResourceInstanceName1_tca_3'};
    const component = shallow(<OpenLoopModal/>);
    component.find('StateManager').simulate('change', event);
    await flushPromises();
    component.update();
    expect(component.state('chosenLoopName')).toEqual("LOOP_gmtAS_v1_0_ResourceInstanceName1_tca_3");
  });


  it('Test handleClose', () => {
    const historyMock = { push: jest.fn() }; 
    const handleClose = jest.spyOn(OpenLoopModal.prototype,'handleClose');
    const component = shallow(<OpenLoopModal history={historyMock} />)

    component.find('[variant="secondary"]').prop('onClick')();

    expect(handleClose).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    
    handleClose.mockClear();
  });

    it('Test handleSubmit', () => {
    const historyMock = { push: jest.fn() };
    const loadLoopFunction = jest.fn();  
    const handleOpen = jest.spyOn(OpenLoopModal.prototype,'handleOpen');
    const component = shallow(<OpenLoopModal history={historyMock} loadLoopFunction={loadLoopFunction}/>)

    component.find('[variant="primary"]').prop('onClick')();

    expect(handleOpen).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual([ '/']);

    handleOpen.mockClear();
  });

});
