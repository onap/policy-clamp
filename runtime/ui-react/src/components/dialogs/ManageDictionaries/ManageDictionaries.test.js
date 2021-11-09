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

const TestDictionaryElements = {
  name: "test",
  secondLevelDictionary: 0,
  subDictionaryType: "",
  dictionaryElements: [
    {
      shortName: "alertType",
      name: "Alert Type",
      description: "Type of Alert",
      type: "string",
      subDictionary: "",
      createdDate: "2020-06-12T13:58:51.443931Z",
      updatedDate: "2020-06-13T16:27:57.084870Z",
      updatedBy: "admin",
      createdBy: "admin"
    }
  ]
};

const TestDictionaries =
  [
    {
      name: "test",
      secondLevelDictionary: 0,
      subDictionaryType: "string",
      dictionaryElements: [TestDictionaryElements],
      createdDate: "2020-06-14T21:00:33.231166Z",
      updatedDate: "2020-06-14T21:00:33.231166Z",
      updatedBy: "admin",
      createdBy: "admin"
    },
    {
      name: "testSub1",
      secondLevelDictionary: 1,
      subDictionaryType: "string",
      dictionaryElements: [
        {
          shortName: "subElem",
          name: "Sub Element",
          description: "Sub Element Description",
          type: "string",
          createdDate: "2020-06-14T21:04:44.402287Z",
          updatedDate: "2020-06-14T21:04:44.402287Z",
          updatedBy: "admin",
          createdBy: "admin"
        }
      ],
      createdDate: "2020-06-14T21:01:16.390250Z",
      updatedDate: "2020-06-14T21:01:16.390250Z",
      updatedBy: "admin",
      createdBy: "admin"
    }
  ];


const historyMock = { push: jest.fn() };

let errorMessage = '';

window.alert = jest.fn().mockImplementation((mesg) => {
  errorMessage = mesg;
  return
});

TemplateMenuService.getDictionary = jest.fn().mockImplementation(() => {
  return Promise.resolve(TestDictionaries);
});

TemplateMenuService.insDictionary = jest.fn().mockImplementation(() => {
  return Promise.resolve({ ok: true, status: 200 });
});

TemplateMenuService.deleteDictionary = jest.fn().mockImplementation(() => {
  return Promise.resolve("200");
});

TemplateMenuService.getDictionaryElements = jest.fn().mockImplementation(() => {
  return Promise.resolve(TestDictionaryElements);
});

TemplateMenuService.deleteDictionaryElements = jest.fn().mockImplementation(() => {
  return Promise.resolve("200");
});

TemplateMenuService.insDictionaryElements = jest.fn().mockImplementation(() => {
  return Promise.resolve("200");
});


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
            "secondLevelDictionary": 1,
            "subDictionaryType": "string",
            "updatedBy": "test",
            "updatedDate": "05-07-2019 19:09:42"
          });
        }
      });
    });
    const component = shallow(<ManageDictionaries/>);
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
            "secondLevelDictionary": 1,
            "subDictionaryType": "string",
            "updatedBy": "test",
            "updatedDate": "05-07-2019 19:09:42"
          });
        }
      });
    });
    const component = shallow(<ManageDictionaries/>);
  });

  it('Test Table icons', () => {

    const component = mount(<ManageDictionaries/>);
    expect(component.find('[className="MuiSelect-icon MuiTablePagination-selectIcon"]')).toBeTruthy();
  });

  test('Test add/replace and delete dictionary requests', async () => {

    const component = shallow(<ManageDictionaries history={ historyMock }/>)
    const instance = component.instance();

    const flushPromises = () => new Promise(setImmediate);

    instance.addReplaceDictionaryRequest({ name: "newdict", secondLevelDictionary: 0, subDictionaryType: "string" });
    instance.deleteDictionaryRequest("test");

    await flushPromises();

    expect(component.state('currentSelectedDictionary')).toEqual(null);
    expect(component.state('dictionaries')).toEqual(TestDictionaries);
  });

  test('Test update dictionary row', async () => {

    const component = shallow(<ManageDictionaries history={ historyMock }/>)
    const instance = component.instance();
    const rowData = { name: "newdict", secondLevelDictionary: 0, subDictionaryType: "string" };

    await expect(instance.updateDictionaryRow(rowData, rowData)).resolves.toEqual(undefined);

  }, 2000);

  test('Test add dictionary row', async () => {

    const addReplaceRequest = jest.spyOn(ManageDictionaries.prototype, 'addReplaceDictionaryRequest');
    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    const rowData = { name: "newdict", secondLevelDictionary: 0, subDictionaryType: "string" };

    await instance.addDictionaryRow(rowData);
    expect(addReplaceRequest).toHaveBeenCalledWith(rowData);

  }, 2000);

  test('Test add dictionary row with errors name already exists', async () => {

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    let rowData = { name: "test", secondLevelDictionary: 0, subDictionaryType: "" };

    await expect(instance.addDictionaryRow(rowData)).rejects.toEqual(undefined);

  }, 2000);

  test('Test add dictionary row with errors illegal chars in name', async () => {

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    let rowData = { name: "test@@", secondLevelDictionary: 0, subDictionaryType: "" };

    await expect(instance.addDictionaryRow(rowData)).rejects.toEqual(undefined);

  }, 2000);

  test('Test update dictionary row with errors illegal chars in name', async () => {

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    let rowData = { name: "test@@", secondLevelDictionary: 0, subDictionaryType: "" };

    await expect(instance.updateDictionaryRow(rowData)).rejects.toEqual(undefined);
  });


  test('Test add dictionary row with errors (illegal chars)', async () => {

    const addReplaceRequest = jest.spyOn(ManageDictionaries.prototype, 'addReplaceDictionaryRequest');
    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    let rowData = { name: "test@@", secondLevelDictionary: 0, subDictionaryType: "" };

    await expect(instance.addDictionaryRow(rowData)).rejects.toEqual(undefined);

  }, 2000);


  test('Test delete dictionary row', async () => {

    const deleteRequest = jest.spyOn(ManageDictionaries.prototype, 'deleteDictionaryRequest');
    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    const rowData = { name: "newdict", secondLevelDictionary: 0, subDictionaryType: "string" };

    await instance.deleteDictionaryRow(rowData);
    expect(deleteRequest).toHaveBeenCalledWith("newdict");

  }, 2000);

  test('Test handle select dictionary row click', async () => {

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    const rowData = { name: "newdict", secondLevelDictionary: 0, subDictionaryType: "string" };

    instance.handleDictionaryRowClick("event", rowData);
    expect(component.state('currentSelectedDictionary')).toEqual("newdict");
  }, 2000);

  test('Test dictionary element row add, update, delete', async () => {

    const rowData = {
      createdBy: "admin",
      createdDate: "2020-06-15T13:59:20.467381Z",
      description: "Description",
      name: "Some Elem",
      shortName: "someElem",
      type: "string",
      updatedBy: "admin",
      updatedDate: "2020-06-15T13:59:20.467381Z"
    };

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    const badRowData = {
      description: "Description",
      name: "Some Elem",
      shortName: "someElem",
      type: "string"
    };

    await instance.clickHandler();
    await instance.getDictionaryElements("test");

    await expect(instance.addDictionaryElementRow(rowData)).resolves.toEqual(undefined);
    await expect(instance.updateDictionaryElementRow(rowData, rowData)).resolves.toEqual(undefined);
    await expect(instance.deleteDictionaryElementRow(rowData)).resolves.toEqual(undefined);
  });

  test('Test dictionary element row add with errors', async () => {

    const badRowData = {
      description: "",
      name: "",
      shortName: "some#Elem",
      type: ""
    };

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    await expect(instance.addDictionaryElementRow(badRowData)).rejects.toEqual("");
  });

  test('Test dictionary element update with error illegal name', async () => {

    const badRowData = {
      description: "",
      name: "test@@",
      shortName: "some#Elem",
      type: ""
    };

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    await expect(instance.updateDictionaryElementRow(badRowData)).rejects.toEqual(undefined);
  });

  test('Test dictionary element addition with duplicate name error', async () => {

    const badRowData = {
      description: "description",
      name: "Alert Type",
      shortName: "alertType",
      type: "string"
    };

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    component.setState({ currentSelectedDictionary: 'test' });

    await instance.getDictionaryElements();
    await expect(instance.addDictionaryElementRow(badRowData)).rejects.toEqual("");
  });

  test('Test dictionary element addition with empty name error', async () => {

    const badRowData = {
      description: "description",
      name: "Alert Type",
      shortName: "",
      type: "string"
    };

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    component.setState({ currentSelectedDictionary: 'test' });

    await instance.getDictionaryElements();
    await expect(instance.addDictionaryElementRow(badRowData)).rejects.toEqual("");
  });


  it('Test Import CSV Sunny Day', async () => {

    TemplateMenuService.insDictionaryElements = jest.fn().mockImplementation(() => {
      return Promise.resolve({ ok: true, status: 200 });
    });

    let rawCsvData = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsvData += '"alertType","Alert Type","Alert Type Description","string","","admin","2020-06-11T13:56:14.927437Z"';

    let expectedResult = [
      {
        description: "Alert Type Description",
        name: "Alert Type",
        shortName: "alertType",
        subDictionary: "",
        type: "string"
      }
    ];

    const updateDictionaryElementsRequest = jest.spyOn(ManageDictionaries.prototype, 'updateDictionaryElementsRequest');

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    await expect(instance.importCsvData(rawCsvData)).toEqual('');
    expect(updateDictionaryElementsRequest).toHaveBeenCalledWith(expectedResult);
  });

  it('Test Import CSV Mandatory Field Check Errors', () => {

    let rawCsvData = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsvData += '"","","","","","",""';

    // The empty values for all the fields in row 1 of the rawCsvData will trigger a bunch of errors.
    // Getting Enzyme to properly match them with embedded newlines turned out to be impossible
    // and maybe not desirable anyway; so our test for "success" here is simply that the
    // routine returns a non-empty error string.

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();
    expect(instance.importCsvData(rawCsvData)).not.toEqual('');
  });

  it('Test Import CSV Errors in Row Data', async () => {

    TemplateMenuService.insDictionaryElements = jest.fn().mockImplementation(() => {
      return Promise.resolve({ ok: true, status: 200 });
    });

    let rawCsvData = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsvData += '"alert@Type","Alert Type","Alert Type Description","strin","subby","admin","2020-06-11T13:56:14.927437Z"';

    let expectedResult = [
      {
        description: "Alert Type Description",
        name: "Alert Type",
        shortName: "alertType",
        subDictionary: "",
        type: "string"
      }
    ];

    const updateDictionaryElementsRequest = jest.spyOn(ManageDictionaries.prototype, 'updateDictionaryElementsRequest');

    const component = shallow(<ManageDictionaries/>)
    const instance = component.instance();

    await expect(instance.importCsvData(rawCsvData)).not.toEqual('');
  });


  it('Test handleClose', () => {
    fetch.mockImplementationOnce(() => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () => {
          return Promise.resolve({
            "name": "vtest",
            "secondLevelDictionary": 1,
            "subDictionaryType": "string",
            "updatedBy": "test",
            "updatedDate": "05-07-2019 19:09:42"
          });
        }
      });
    });
    const handleClose = jest.spyOn(ManageDictionaries.prototype, 'handleClose');
    const component = shallow(<ManageDictionaries history={ historyMock }/>)
    component.find('[variant="secondary"]').prop('onClick')();
    expect(handleClose).toHaveBeenCalledTimes(1);
    expect(component.state('show')).toEqual(false);
    expect(historyMock.push.mock.calls[0]).toEqual(['/']);
    handleClose.mockClear();
  });
});
