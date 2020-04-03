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
import { mount } from 'enzyme';
import { render } from 'enzyme';
import ManageDictionaries from './ManageDictionaries';
import TemplateMenuService from '../../../api/TemplateService'

describe('Verify ManageDictionaries', () => {
	beforeEach(() => {
		fetch.resetMocks();
	});

	it('Test API Successful', () => {
		fetch.mockImplementationOnce(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {
					return Promise.resolve({
						"name": "vtest",
						"secondLevelDictionary": "1",
						"subDictionaryType": "string",
						"updatedBy": "test",
						"updatedDate": "05-07-2019 19:09:42"
					});
				}
			});
		});
		const component = shallow(<ManageDictionaries />);
		expect(component).toMatchSnapshot();
	});

	it('Test API Exception', () => {
		fetch.mockImplementationOnce(() => {
			return Promise.resolve({
				ok: false,
				status: 500,
				json: () => {
					return Promise.resolve({
						"name": "vtest",
            "secondLevelDictionary": "1",
						"subDictionaryType": "string",
        		"updatedBy": "test",
            "updatedDate": "05-07-2019 19:09:42"
					});
				}
			});
		});
		const component = shallow(<ManageDictionaries />);
	});

	it('Test API Rejection', () => {
		const myMockFunc  = fetch.mockImplementationOnce(() => Promise.reject('error'));
		setTimeout( () => myMockFunc().catch(e => {
			console.info(e);
		}),
		100
		);
		const component = shallow(<ManageDictionaries />);
		expect(myMockFunc.mock.calls.length).toBe(1);
	});

	it('Test Table icons', () => {
		fetch.mockImplementationOnce(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {
					return Promise.resolve({
						"name": "vtest",
	          "secondLevelDictionary": "1",
						"subDictionaryType": "string",
	       		"updatedBy": "test",
	          "updatedDate": "05-07-2019 19:09:42"
					});
				}
			});
		});
		const component = mount(<ManageDictionaries />);
		expect(component.find('[className="MuiSelect-icon MuiTablePagination-selectIcon"]')).toBeTruthy();
	});

	test('Test get dictionaryNames/dictionaryElements, add/delete dictionary functions', async () => {
		const historyMock = { push: jest.fn() };
		TemplateMenuService.getDictionary = jest.fn().mockImplementation(() => {
			return Promise.resolve("test");
		});
		TemplateMenuService.getDictionaryElements = jest.fn().mockImplementation(() => {
			return Promise.resolve({dictionaryElements:"testitem"});
		});
		TemplateMenuService.insDictionary = jest.fn().mockImplementation(() => {
			return Promise.resolve(200);
		});
		TemplateMenuService.deleteDictionary = jest.fn().mockImplementation(() => {
			return Promise.resolve(200);
		});
		const flushPromises = () => new Promise(setImmediate);
		const component = shallow(<ManageDictionaries history={historyMock} />)
		component.setState({ newDict: {
			"name": "test",
			"secondLevelDictionary": "0",
			"subDictionaryType": "string"
			}
		});
		component.setState({ delData: {
			"name": "test",
			"secondLevelDictionary": "0",
			"subDictionaryType": "string"
			}
		});
		const instance = component.instance();
		instance.getDictionaryElements("test");
		instance.clickHandler();
		instance.addDictionary();
		instance.deleteDictionary();
		await flushPromises();
		expect(component.state('dictionaryNames')).toEqual("test");
		expect(component.state('dictionaryElements')).toEqual("testitem");
		expect(component.state('dictNameFlag')).toEqual(false);
	});

	test('Test adding and deleting dictionaryelements', async () => {
		const historyMock = { push: jest.fn() };
		TemplateMenuService.getDictionary = jest.fn().mockImplementation(() => {
			return Promise.resolve("test");
		});
		TemplateMenuService.insDictionaryElements = jest.fn().mockImplementation(() => {
			return Promise.resolve(200);
		});
		TemplateMenuService.deleteDictionaryElements = jest.fn().mockImplementation(() => {
			return Promise.resolve(200);
		});
		const flushPromises = () => new Promise(setImmediate);
		const component = shallow(<ManageDictionaries history={historyMock}/>)
		component.setState({ newDictItem: {
			"name": "test",
			"dictionaryElements" : {
				"shortName": "shorttest",
				}
		}});
		component.setState({ delDictItem: {
			"name": "test",
			"dictionaryElements" : {
				"shortName": "shortTest",
				}
		}});
		const instance = component.instance();
		instance.addDictionary();
		instance.deleteDictionary();
		await flushPromises();
		expect(component.state('dictionaryNames')).toEqual("test");
	});

	it('Test handleClose', () => {
		fetch.mockImplementationOnce(() => {
			return Promise.resolve({
				ok: true,
				status: 200,
				json: () => {
					return Promise.resolve({
						"name": "vtest",
		      	"secondLevelDictionary": "1",
						"subDictionaryType": "string",
		      	"updatedBy": "test",
		      	"updatedDate": "05-07-2019 19:09:42"
					});
				}
			});
		});
		const historyMock = { push: jest.fn() };
		const handleClose = jest.spyOn(ManageDictionaries.prototype,'handleClose');
		const component = shallow(<ManageDictionaries history={historyMock} />)
		component.find('[variant="secondary"]').prop('onClick')();
		expect(handleClose).toHaveBeenCalledTimes(1);
		expect(component.state('show')).toEqual(false);
		expect(historyMock.push.mock.calls[0]).toEqual([ '/']);
		handleClose.mockClear();
	});
});
