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
import { shallow } from 'enzyme';
import CreateLoopModal from './CreateLoopModal';
import LoopService from '../../../api/LoopService';
import TemplateService from '../../../api/TemplateService';

let errorMessage = '';
window.alert = jest.fn().mockImplementation((mesg) => {
  errorMessage = mesg;
  return
});


describe('Verify CreateLoopModal', () => {

  it('Test the render method', async () => {
    const flushPromises = () => new Promise(setImmediate);
    TemplateService.getAllLoopTemplates = jest.fn().mockImplementation(() => {
      return Promise.resolve([{ "name": "template1" }, { "name": "template2" }]);
    });
    TemplateService.getLoopNames = jest.fn().mockImplementation(() => {
      return Promise.resolve([]);
    });

    const component = shallow(<CreateLoopModal/>);
    expect(component).toMatchSnapshot();
    await flushPromises();
    component.update();
    expect(component.state('templateNames')).toStrictEqual([{ "label": "template1", "value": "template1", "templateObject": { "name": "template1" } }, {
      "label": "template2",
      "value": "template2",
      "templateObject": { "name": "template2" }
    }]);
  });

  it('handleDropdownListChange event', async () => {
    const flushPromises = () => new Promise(setImmediate);

    const component = shallow(<CreateLoopModal/>);
    component.find('StateManager').simulate('change', { value: 'template1', templateObject: { "name": "template1" } });
    await flushPromises();
    component.update();
    expect(component.state('chosenTemplateName')).toEqual("template1");
    expect(component.state('fakeLoopCacheWithTemplate').getLoopTemplate()['name']).toEqual("template1");
    expect(component.state('fakeLoopCacheWithTemplate').getLoopName()).toEqual("fakeLoop");

    component.find('StateManager').simulate('change', { value: 'template2', templateObject: { "name": "template2" } });
    await flushPromises();
    component.update();
    expect(component.state('chosenTemplateName')).toEqual("template2");
    expect(component.state('fakeLoopCacheWithTemplate').getLoopTemplate()['name']).toEqual("template2");
    expect(component.state('fakeLoopCacheWithTemplate').getLoopName()).toEqual("fakeLoop");
  });

  it('handleModelName event', async () => {
    const flushPromises = () => new Promise(setImmediate);
    TemplateService.getAllLoopTemplates = jest.fn().mockImplementation(() => {
      return Promise.resolve([{ "name": "template1" }, { "name": "template2" }]);
    });
    TemplateService.getLoopNames = jest.fn().mockImplementation(() => {
      return Promise.resolve([]);
    });
    const event = { target: { value: "model1" } };
    const component = shallow(<CreateLoopModal/>);
    await flushPromises();
    component.find('input').simulate('change', event);
    component.update();
    expect(component.state('modelName')).toEqual("model1");
  });

  it('Test handleClose', () => {
    const historyMock = { push: jest.fn() };
    const handleClose = jest.spyOn(CreateLoopModal.prototype, 'handleClose');
    const component = shallow(<CreateLoopModal history={ historyMock }/>)

    component.find('[variant="secondary"]').prop('onClick')();

    expect(handleClose).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual(['/']);

    handleClose.mockClear();
  });

  it('Test handleCreate Fail', () => {
    const handleCreate = jest.spyOn(CreateLoopModal.prototype, 'handleCreate');
    const component = shallow(<CreateLoopModal/>)

    component.find('[variant="primary"]').prop('onClick')();

    expect(handleCreate).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(true);

    handleCreate.mockClear();
  });

  it('Test handleCreate Suc', async () => {
    const flushPromises = () => new Promise(setImmediate);
    const historyMock = { push: jest.fn() };
    const loadLoopFunction = jest.fn();

    LoopService.createLoop = jest.fn().mockImplementation(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
        }
      });
    });

    const handleCreate = jest.spyOn(CreateLoopModal.prototype, 'handleCreate');
    const component = shallow(<CreateLoopModal history={ historyMock } loadLoopFunction={ loadLoopFunction }/>)
    component.setState({
      modelName: "modelNameTest",
      chosenTemplateName: "template1"
    });

    component.find('[variant="primary"]').prop('onClick')();
    await flushPromises();
    component.update();

    expect(handleCreate).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual(['/']);

    handleCreate.mockClear();
  });

});
