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
import ViewToscaModal from './ViewToscaModal';
import { mount } from 'enzyme';


describe('Verify ViewToscaModal', () => {
	beforeEach(() => {
		fetch.resetMocks();
		fetch.mockImplementation(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {
					return Promise.resolve({
						"index": "1",
  					"toscaModelYaml":"MTCA",
						"toscaModelName":"DCAE_MTCAConfig",
						"version":"16",
						"userId":"aj928f",
						"policyType":"mtca",
						"lastUpdatedDate":"05-07-2019 19:09:42"
					});
				}
			});
		});
	});

	it('Test the tosca model view render method', () => {
		const component = shallow(<ViewToscaModal/>);
		component.setState({ toscaNames: {
			"index": "1",
			"toscaModelYaml": "MTCA",
			"toscaModelName": "DCAE_MTCAConfig",
			"version" : "16",
			"userId" : "aj928f",
			"policyType" : "mtca",
			"lastUpdatedDate" : "05-07-2019 19:09:42"
		}
	});
    expect(component).toMatchSnapshot();
  });

	it('Test Table icons', () => {
		const component = mount(<ViewToscaModal/>);
		expect(component.find('[className="MuiSelect-icon MuiTablePagination-selectIcon"]')).toBeTruthy();

  });

	it('Test handleYamlContent', () => {
		const yamlContent = 'MTCA Tosca model details';
		const component = shallow(<ViewToscaModal/>);
		component.find('[value="Please select Tosca model to view the details"]').prop('onChange')({ target: { value: yamlContent }});
    expect(component.state('content')).toEqual(yamlContent);
  });

	it('Test handleClose', () => {
		const historyMock = { push: jest.fn() };
    const handleClose = jest.spyOn(ViewToscaModal.prototype,'handleClose');
    const component = shallow(<ViewToscaModal history={historyMock} />)
    component.find('[variant="secondary"]').prop('onClick')();
    expect(handleClose).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
    handleClose.mockClear();
  });
});
