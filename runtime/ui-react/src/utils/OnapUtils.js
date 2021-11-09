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

export default class OnapUtils {

  constructor() {
    this.clickBlocked = false;
  }

  static jsonEditorErrorFormatter(errors) {

    let messages = [];
    let messagesOutputString = null;

    // errors is an array of JSON Editor "error" objects, where each
    // object looks like this:

    // {
    //   message: "Please populate the required property "Threshold""
    //   path: "root.signatures.0"
    //   property: "required"
    // }

    // In this function we concatenate all the messages, removing any duplicates,
    // and adding a newline between each message. The result returned is a single
    // string that can be displayed to the user in an alert message

    if (!Array.isArray(errors)) {
      console.error('jsoneEditorErrorFormatter was passed a non-array argument');
    } else {
      for (let ii = 0; ii < errors.length; ++ii) {
        if (!messages.includes(errors[ii].message)) {
          messages.push(errors[ii].message);
          if (messagesOutputString) {
            messagesOutputString += '\n' + errors[ii].message;
          } else {
            messagesOutputString = errors[ii].message;
          }
        }
      }
    }

    return messagesOutputString;
  }
}
