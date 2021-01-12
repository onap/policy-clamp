/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

export default function CsvToJson(rawCsvData, delimiter, internalDelimiter, csvHeaderNames, jsonKeyNames, mandatory) {

	let printDictKeys = '';
	let result = { jsonObjArray: [], errorMessages: '' };

	// Validate that all parallel arrays passed in have same number of elements;
	// this would be a developer error.

	let checkLength = csvHeaderNames.length;

	if (checkLength !== jsonKeyNames.length || checkLength !== mandatory.length) {
		result.errorMessages = 'interanl error: csvHeaderNames, jsonKeyNames, and mandatory arrays parameters are not the same length';
		return result;
	}

	if (checkLength < 1) {
		result.errorMessages = 'interanl error: csvHeaderNames, jsonKeyNames, and mandatory arrays have no entries';
		return result;
	}

	// Make a nice string to print in the error case to tell user what is the
	//  required heaer row format

	for (let i=0; i < csvHeaderNames.length; ++i) {
		if (i === 0) {
			printDictKeys = csvHeaderNames[i];
		} else {
			printDictKeys += ',' +  csvHeaderNames[i];
		}
	}

	let dictElems = rawCsvData.split('\n');
	let numColumns = 0;
	let filteredDictElems = [];

	// The task of the following loop is to convert raw CSV rows into easily parseable
	// and streamlined versions of the rows with an internalDelimiter replacing the standard
	// comma; it is presumed (and checked) that the internalDelimiter cannot exist as a valid
	// sequence of characters in the user's data.

	// This conversion process also strips leading and trailing whitespace from each row,
	// discards empty rows, correctly interprets and removes all double quotes that programs like
	// Excel use to support user columns that contain special characters, most notably, the comma
	// delimiter. A double-quote that is contained within a double-quoted column value 
	// must appear in this raw data as a sequence of two double quotes. Furthermore, any column
	// value in the raw CSV data that does not contain a delimiter may or may not be enclosed in
	// double quotes. It is the Excel convention to not use double qoutes unless necessary, and
	// there is no reasonable way to tell Excel to surround every column value with double quotes. 
	// Any files that were directly "exported" by CLAMP itself from the Managing Dictionaries
	// capability, surround all columns with double quotes.

	for (let i = 0; i < dictElems.length; i++) {

		let oneRow = dictElems[i].trim();
		let j = 0;
		let inQuote = false
		let nextChar = undefined;
		let prevChar = null;

		
		if (oneRow === '') {
			continue; // Skip blank rows
		} else if (oneRow.indexOf(internalDelimiter) !== -1) {
			result.errorMessages += '\nRow #' + i + ' contains illegal sequence of characters (' + internalDelimiter + ')';
			break;
		} else {
			nextChar = oneRow[1];
		}

		let newStr = '';
		numColumns = 1;

		// This "while loop" performs the very meticulous task of removing double quotes that
		// are used by Excel to encase special characters as user string value data,
		// and manages to correctly identify columns that are defined with or without
		// double quotes and to process the comma delimiter correctly when encountered
		// as a user value within a column. Such a column would have to be encased in
		// double quotes; a comma found outside double quotes IS a delimiter.

		while (j < oneRow.length) {
		 	if (oneRow[j] === '"') {
		 		if (inQuote === false) {
					if (prevChar !== delimiter && prevChar !== null) {
						result.errorMessages += '\nMismatched double quotes or illegal whitespace around delimiter at row #' + (i + 1) + ' near column #' + numColumns;
						break;
					} else {
						inQuote = true;
					}
				} else {
					if (nextChar === '"') {
						newStr += '"';
						++j;
					} else if ((nextChar !== delimiter) && (nextChar !== undefined)) {
						result.errorMessages += '\nRow #' + (i + 1) + ' is badly formatted at column #' + numColumns + '. Perhaps an unescaped double quote.';
						break;
					} else if (nextChar === delimiter) {
						++numColumns;
						inQuote = false;
						newStr += internalDelimiter;
						prevChar = delimiter;
						j += 2;
						nextChar = oneRow[j+1];
						continue;
					} else {
						++numColumns;
						inQuote = false;
						break;
					}
				}
			} else {
				if (oneRow[j] === delimiter && inQuote === false) {
					newStr += internalDelimiter;
					++numColumns;
				} else {
					newStr += oneRow[j];
				}
			}
			prevChar = oneRow[j];
			++j;
			nextChar = oneRow[j+1]; // can result in undefined at the end
		}

		if (result.errorMessages === '' && inQuote !== false) {
			result.errorMessages += '\nMismatched double quotes at row #' + (i + 1);
			break;
		} else if (result.errorMessages === '' && numColumns < jsonKeyNames.length) {
			result.errorMessages += '\nNot enough columns (' + jsonKeyNames.length + ') at row #' + (i + 1);
			break;
		}

		filteredDictElems.push(newStr);
	}

	if (result.errorMessages !== '') {
		return result;
	}

	// Perform further checks on data that is now in JSON form
	if (filteredDictElems.length < 2) {
		result.errorMessages += '\nNot enough row data found in import file. Need at least a header row and one row of data';
		return result;
	}

	// Now that we have something reliably parsed into sanitized columns lets run some checks
	// and convert it all into an array of JSON objects to push to the back end if all the
	// checks pass.

	let headers = filteredDictElems[0].split(internalDelimiter);

	// check that headers are included in proper order
	for (let i=0; i < jsonKeyNames.length; ++i) {
		if (csvHeaderNames[i] !== headers[i]) {
			result.errorMessages += 'Row 1 header key at column #' + (i + 1) + ' is a mismatch. Expected row header must contain at least:\n' + printDictKeys; 
			return result;
		}
	}

	// Convert the ASCII rows of data into an array of JSON obects that omit the header
	// row which is not sent to the back end.

	for (let i = 1; i < filteredDictElems.length; i++) {
		let data = filteredDictElems[i].split(internalDelimiter);
		let obj = {};
		for (let j = 0; j < data.length && j < jsonKeyNames.length; j++) {
			let value = data[j].trim();
			if (mandatory[j] === true && value === '') {
				result.errorMessages += '\n' + csvHeaderNames[j] + ' at row #' + (i+1) + ' is empty but requires a value.';
			}
			obj[jsonKeyNames[j]] = value;
		}
		result.jsonObjArray.push(obj);
	}

	if (result.errorMessages !== '') {
		// If we have errors, return empty parse result even though some things
		// may have parsed properly. We do not want to encourage the caller
		// to think the data is good for use.
		result.jsonObjArray = [];
	}

	return result;
}
