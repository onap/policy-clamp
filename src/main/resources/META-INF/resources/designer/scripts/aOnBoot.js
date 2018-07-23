/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
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


//When element is first created it should have a red box because it hasn't been edited
function newElementProcessor(id) {
  if ($('g[data-element-id="' + id + '"]').length > 0) {

    var _idNode = $('g[data-element-id="' + id + '"]')
    _idNode.children("rect").each(function() {
      if ($(this).attr('class') === 'djs-outline') {
        $(this).attr('class', "djs-outline-no-property-saved")
        $(this).attr('fill', 'red')
      }
    });

  }
}

function saveProperties(form) {
  elementMap[lastElementSelected] = form;
  totalJsonProperties = JSON.stringify(elementMap);

  //Take off the red border because the element has been edited
  if ($('g[data-element-id="' + lastElementSelected + '"]').length > 0) {
    var _idNode = $('g[data-element-id="' + lastElementSelected + '"]')
    _idNode.children("rect").each(function() {
      if ($(this).attr('class') === 'djs-outline-no-property-saved') {
        $(this).attr('class', "djs-outline")
        $(this).attr('fill', 'none')
      }
    });
  }
}

function saveGlobalProperties(form) {
  elementMap["global"] = form;
}
var isObject = function(a) {
  return (!!a) && (a.constructor === Object);
};

function loadPropertyWindow(type) {
  if (readMOnly) {
    if ($("#add_one_more").length == 1) {
      $("#add_one_more").off();
      $("#add_one_more").click(function(event) {
        event.preventDefault();
      })
    }
    $("input,#savePropsBtn").attr("disabled", "");
    $(".modal-body button").attr("disabled", "");
    ($("select:not([multiple])")).multiselect("disable");
  }

  var props = defaults_props[type];

  for (p in props) {
    if (isObject(props[p])) {
      var mySelect = $('#' + p);
      if (p == "operator") {
        $.each(props[p], function(val, text) {
          mySelect.append(
            $('<option></option>').val(val).html(val)
          );
        });
      } else {
        $.each(props[p], function(val, text) {
          mySelect.append(
            $('<option></option>').val(val).html(text)
          );
        });
      }
    } else {
      if (p == "pname") {
        var ms = new Date().getTime();
        props[p] = "Policy" + ms;
      }
      $("#" + p).val(props[p])
    }
  }
  setTimeout(function() {
    setMultiSelect(type);
  }, 100);



}

function setMultiSelect() {
  $("select").each(function(index, mySelect) {

    var mySelectObj = $(mySelect);
    if (! mySelectObj.parents(".multiselect-native-select")) {
      // keep native for this one
      return;
    }

    if (mySelectObj.parents(".multiselect-native-select").length > 0) {
      var selectDrop = mySelectObj.parent(".multiselect-native-select").find("select");
      mySelectObj.parent(".multiselect-native-select").parent().html(selectDrop);
    }

    var options = {
      numberDisplayed: 1,
      maxHeight: 200
    };

    if (mySelectObj.attr("multiple")
        && mySelectObj.attr("multiple") != 'false') {
      options.includeSelectAllOption = true;
    }
    
    if (mySelectObj.attr("enableFilter")
        && mySelectObj.attr("enableFilter") != 'false') {
      options.enableCaseInsensitiveFiltering = true;
      options.enableFiltering = true;
    }

    mySelectObj.multiselect(options);
  });
}

function loadSharedPropertyByService(onChangeUUID, refresh, callBack) {
  var uuid = onChangeUUID;
  if (uuid === undefined) {
    uuid = elementMap["global"] && elementMap["global"].length > 0 ?
      elementMap["global"][0].value : "";
  }
  var share = null,
    serviceUrl = '/restservices/clds/v1/clds/properties/' + uuid;
  if (refresh) {
    serviceUrl = '/restservices/clds/v1/clds/properties/' + uuid + '?refresh=true';
  }

  $.ajax({
    async: false,
    dataType: "json",
    url: serviceUrl,
    success: function(data) {
      vf_Services = JSON.parse(data);
      setASDCFields()
      if (refresh) {
        $("#paramsWarnrefresh").hide();
      }
      if ($("#paramsWarn")) {
        $("#paramsWarn").hide();
      }
      if (callBack && _.isFunction(callBack)) {
        callBack(true);
      }
    },
    error: function(s, a, err) {
      if (refresh) {
        $("#paramsWarnrefresh").show();
      }
      if ($("#paramsWarn")) {
        $("#paramsWarn").show();
      }

      $('#servName').text($("#service option:selected").text());
      if (callBack && _.isFunction(callBack)) {
        callBack(false);
      }
      console.log(err)
      console.log(s)
      console.log(a)
    },
    timeout: 100000

  });

  //vf_Services=share['shared']['byService'][uuid];
  //location_values = share['global']['location'];
}

function loadSharedPropertyByServiceProperties(callBack) {
  $.ajax({
    async: false,
    dataType: "json",
    url: '/restservices/clds/v1/clds/properties/',
    success: function(data) {
      vf_Services = JSON.parse(data);
      setASDCFields();
      if (callBack && _.isFunction(callBack)) {
        callBack(true);
      }
    },
    error: function(s, a, err) {
      $('#servName').text($("#service option:selected").text());
      if (callBack && _.isFunction(callBack)) {
        callBack(false);
      }
    },
    timeout: 100000

  });
}

function setASDCFields() {
  if (vf_Services === null || vf_Services === undefined) {
    loadSharedPropertyByService()
  } else {
    try {
      $("#vf").empty().multiselect("refresh");
      $("#location").empty().multiselect("refresh");
      $("#actionSet").empty().multiselect("refresh");
      $("#vfc").empty().multiselect("refresh");
      $("#paramsWarn").hide();
      var uuid = Object.keys(vf_Services['shared']['byService'])[0];

      var vf_values = vf_Services['shared']['byService'][uuid] &&
        vf_Services['shared']['byService'][uuid]['vf'] &&
        _.keys(vf_Services['shared']['byService'][uuid]['vf']).length > 0 ?
        vf_Services['shared']['byService'][uuid]['vf'] : null;

      var selectedVF = {}
      for (let e in elementMap["global"]) {
        if (elementMap['global'][e].name === "vf") {
          selectedVF = elementMap['global'][e].value[0]
        }
      }

      var vfc_values2 = selectedVF &&
        vf_Services['shared']['byVf'][selectedVF] &&
        vf_Services['shared']['byVf'][selectedVF]['vfc'] &&
        _.keys(vf_Services['shared']['byVf'][selectedVF]['vfc']).length > 0 ?
        vf_Services['shared']['byVf'][selectedVF]['vfc'] : null;

      if (vf_values) {
        for (key in vf_values) {
          if ($("#vf").length > 0) {
            $("#vf").append("<option value=\"" + key + "\">" + vf_values[key] + "</opton>")
          }
        }
        $("#vf").multiselect("rebuild");
      }

      var location_values = vf_Services['global']['location'];
      if (location_values) {
        for (key in location_values) {
          if ($("#location").length > 0) {
            $("#location").append("<option value=\"" + key + "\">" + location_values[key] + "</opton>")
          }
        }
        $("#location").multiselect("rebuild");
      }

      var actionSet_values = vf_Services['global']['actionSet'];
      if (actionSet_values) {
        for (key in actionSet_values) {
          if ($("#actionSet").length > 0) {
            $("#actionSet").append("<option value=\"" + key + "\">" + actionSet_values[key] + "</opton>")
          }
        }
        $("#actionSet").multiselect("rebuild");
      }

      if (vfc_values2) {
        $("#vfc").append("<option value=\"\"></opton>");
        for (key in vfc_values2) {
          if ($("#vfc").length > 0) {
            $("#vfc").append("<option value=\"" + key.split("\"").join("") + "\">" + vfc_values2[key] + "</opton>")
          }
        }
        $("#vfc").multiselect("rebuild");
      }
      if ($("#vfc").length > 0 && !vfc_values2) {
        showWarn();
      }
      if ($("#vf").length > 0 && !vf_values) {
        showWarn();
      }
      if ($("#location").length > 0 && !location_values) {
        showWarn();
      }

      function showWarn() {
        $("#paramsWarn").show();
        $('#servName').text($("#service option:selected").text());
      }
    } catch (e) {
      console.log(e)
    }
  }
}

//Typically used when opening a new model/template
function reloadDefaultVariables(isTemp) {
  isTemplate = isTemp;
  vf_Services = null;
}

$(window).on('load',function() {
  $.ajax({
    dataType: "json",
    url: '/restservices/clds/v1/clds/properties',
    success: function(data) {
      defaults_props = JSON.parse(data);
    },
    error: function(s, a, err) {
      console.log(err)
      console.log(s)
      console.log(a)
    },
    timeout: 100000
  });
})
