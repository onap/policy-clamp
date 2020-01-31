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
import UploadToscaPolicyModal from './UploadToscaPolicyModal';


describe('Test Upload Tosca Policy Model', () => {

      it('Test handleMicroServiceName', () => {

      const component = shallow(<UploadToscaPolicyModal />);

        const inputValue = 'TCA'

        const button = component.find('input').at(1);

        button.simulate('change', { target: { value: inputValue }});

        expect(component.state('policyModelType')).toEqual(inputValue);

        expect(component).toMatchSnapshot();

    });

    it('Test handleUploadToscaPolicyModel for Tosca Model', () => {

      const component = shallow(<UploadToscaPolicyModal />);

      const fakeEvent = { preventDefault: () => console.log('preventDefault') };

      component.setState({
    	policyModelType: "TCA",
        upldBtnClicked: false,
        policyModelTosca: "TCAToscaModelYaml",
        selectedFile: { name: "tca.yaml"}
      });

      const Button = component.find('Button').at(1);

      Button.simulate('click', fakeEvent);

      expect(component.state('policyModelTosca')).toEqual('TCAToscaModelYaml');

    });

    it('Test handleClose', () => {

      const historyMock = { push: jest.fn() };

      const handleClose = jest.spyOn(UploadToscaPolicyModal.prototype,'handleClose');

      const component = shallow(<UploadToscaPolicyModal history={historyMock} />)

      component.find('[variant="secondary"]').at(1).prop('onClick')();

      expect(handleClose).toHaveBeenCalledTimes(1);

      expect(component.state('show')).toEqual(false);

      expect(historyMock.push.mock.calls[0]).toEqual([ '/']);

      handleClose.mockClear();

    });

});
