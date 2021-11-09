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

import CsvToJson from './CsvToJson'

describe('Verify CsvToJson', () => {

  const hdrNames = [
    "Element Short Name",
    "Element Name",
    "Element Description",
    "Element Type",
    "Sub-Dictionary"
  ];

  const jsonKeyNames = [
    "shortName",
    "name",
    "description",
    "type",
    "subDictionary"
  ];

  const mandatory = [true, true, true, true, false];

  it('Test CsvToJson No Error Case, Quoted Columns', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let expectedResult = {
      errorMessages: '',
      jsonObjArray: [
        {
          description: "Type of Alert",
          name: "Alert Type",
          shortName: "alertType",
          subDictionary: "",
          type: "string"
        }
      ]
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson No Error Case, Unquoted Columns', () => {

    let rawCsv = 'Element Short Name,Element Name,Element Description,Element Type,Sub-Dictionary\n';
    rawCsv += 'alertType,Alert Type,Type of Alert,string,,admin,2020-06-11T13:56:14.927437Z';

    let expectedResult = {
      errorMessages: '',
      jsonObjArray: [
        {
          description: "Type of Alert",
          name: "Alert Type",
          shortName: "alertType",
          subDictionary: "",
          type: "string"
        }
      ]
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Properly Escaped Double Quote and Delimiter', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert ""Type""","Type of Alert, Varies","string","","admin","2020-06-11T13:56:14.927437Z"';

    let errorMessage = '';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: [
        {
          description: "Type of Alert, Varies",
          name: 'Alert "Type"',
          shortName: 'alertType',
          subDictionary: "",
          type: "string",
        }

      ]
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });


  it('Test CsvToJson Error Header Mismatch Error Case', () => {

    let rawCsv = '"Element Short Names","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let errorMessage = 'Row 1 header key at column #1 is a mismatch. Expected row header must contain at least:\n';
    errorMessage += 'Element Short Name,Element Name,Element Description,Element Type,Sub-Dictionary';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Mismatched Double Quotes in Column', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alert"Type","Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let errorMessage = '\nRow #2 is badly formatted at column #1. Perhaps an unescaped double quote.'

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Illegal Whitespace', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += 'alertType ,  "Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let errorMessage = '\nMismatched double quotes or illegal whitespace around delimiter at row #2 near column #2';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Too Few Data Columns', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Type of Alert"';

    let errorMessage = '\nNot enough columns (5) at row #2';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Wrong Header Column Order', () => {

    let rawCsv = '"Element Name","Element Short Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let errorMessage = 'Row 1 header key at column #1 is a mismatch. Expected row header must contain at least:\n';
    errorMessage += 'Element Short Name,Element Name,Element Description,Element Type,Sub-Dictionary';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Not Enough Rows', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';

    let errorMessage = '\nNot enough row data found in import file. Need at least a header row and one row of data';

    let expectedResult = {
      errorMessages: errorMessage,
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Mandatory Field Is Empty', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"","Alert Type","Type of Alert","string","","admin","2020-06-11T13:56:14.927437Z"';

    let expectedResult = {
      errorMessages: '\nElement Short Name at row #2 is empty but requires a value.',
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '|', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Mismatched Double Quotes At End', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Alert Type Description","string","admin","2020-06-11T13:56:14.927437Z';

    let expectedResult = {
      errorMessages: '\nMismatched double quotes at row #2',
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '||', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Mismatched Mandatory Array Parameters', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Alert Type Description","string","admin","2020-06-11T13:56:14.927437Z';

    let expectedResult = {
      errorMessages: 'interanl error: csvHeaderNames, jsonKeyNames, and mandatory arrays parameters are not the same length',
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '||', hdrNames, jsonKeyNames, [true])).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Empty Mandatory Array Parameters', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Alert Type Description","string","admin","2020-06-11T13:56:14.927437Z';

    let expectedResult = {
      errorMessages: 'interanl error: csvHeaderNames, jsonKeyNames, and mandatory arrays have no entries',
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '||', [], [], [])).toEqual(expectedResult);
  });

  it('Test CsvToJson Error Illegal Data Contains Internal Delimiter', () => {

    let rawCsv = '"Element Short Name","Element Name","Element Description","Element Type","Sub-Dictionary"\n';
    rawCsv += '"alertType","Alert Type","Alert Type||Description","string","admin","2020-06-11T13:56:14.927437Z';

    let expectedResult = {
      errorMessages: '\nRow #1 contains illegal sequence of characters (||)',
      jsonObjArray: []
    };

    expect(CsvToJson(rawCsv, ',', '||', hdrNames, jsonKeyNames, mandatory)).toEqual(expectedResult);
  });
})
