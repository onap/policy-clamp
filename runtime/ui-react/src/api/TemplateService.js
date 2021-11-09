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

export default class TemplateService {

  static getLoopNames() {
    return fetch(window.location.pathname + 'restservices/clds/v2/loop/getAllNames', { method: 'GET', credentials: 'same-origin' })
      .then(function (response) {
        console.debug("getLoopNames response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getLoopNames query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getLoopNames error received", error);
        return {};
      });
  }

  static getAllLoopTemplates() {
    return fetch(window.location.pathname + 'restservices/clds/v2/templates', { method: 'GET', credentials: 'same-origin', })
      .then(function (response) {
        console.debug("getAllLoopTemplates response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getAllLoopTemplates query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getAllLoopTemplates error received", error);
        return {};
      });
  }

  static getDictionary() {
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/', { method: 'GET', credentials: 'same-origin', })
      .then(function (response) {
        console.debug("getDictionary response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getDictionary query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getDictionary error received", error);
        return {};
      });
  }

  static getDictionaryElements(dictionaryName) {
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/' + dictionaryName, {
      method: 'GET',
      headers: {
        "Content-Type": "application/json",
      },
      credentials: 'same-origin',
    })
      .then(function (response) {
        console.debug("getDictionaryElements response received: ", response.status);
        if (response.ok) {
          return response.json();
        } else {
          console.error("getDictionaryElements query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("getDictionaryElements error received", error);
        return {};
      });
  }

  static insDictionary(jsonData) {
    console.log("dictionaryName is", jsonData.name)
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/', {
      method: 'PUT',
      credentials: 'same-origin',
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(jsonData)
    })
      .then(function (response) {
        console.debug("insDictionary response received: ", response.status);
        if (response.ok) {
          return response.status;
        } else {
          var errorMessage = response.status;
          console.error("insDictionary query failed", response.status);
          return errorMessage;
        }
      })
      .catch(function (error) {
        console.error("insDictionary error received", error);
        return "";
      });
  }

  static insDictionaryElements(jsonData) {
    console.log("dictionaryName is", jsonData.name)
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/' + jsonData.name, {
      method: 'PUT',
      credentials: 'same-origin',
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(jsonData)
    })
      .then(function (response) {
        console.debug("insDictionary response received: ", response.status);
        if (response.ok) {
          return response.status;
        } else {
          var errorMessage = response.status;
          console.error("insDictionary query failed", response.status);
          return errorMessage;
        }
      })
      .catch(function (error) {
        console.error("insDictionary error received", error);
        return "";
      });
  }

  static deleteDictionary(dictionaryName) {
    console.log("inside templaemenu service", dictionaryName)
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/' + dictionaryName, {
      method: 'DELETE',
      headers: {
        "Content-Type": "application/json",
      },
      credentials: 'same-origin',
    })
      .then(function (response) {
        console.debug("deleteDictionary response received: ", response.status);
        if (response.ok) {
          return response.status;
        } else {
          console.error("deleteDictionary query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("deleteDictionary error received", error);
        return {};
      });
  }

  static deleteDictionaryElements(dictionaryData) {
    return fetch(window.location.pathname + 'restservices/clds/v2/dictionary/' + dictionaryData.name + '/elements/' + dictionaryData.shortName, {
      method: 'DELETE',
      headers: {
        "Content-Type": "application/json",
      },
      credentials: 'same-origin',
    })
      .then(function (response) {
        console.debug("deleteDictionary response received: ", response.status);
        if (response.ok) {
          return response.status;
        } else {
          console.error("deleteDictionary query failed");
          return {};
        }
      })
      .catch(function (error) {
        console.error("deleteDictionary error received", error);
        return {};
      });
  }
}
