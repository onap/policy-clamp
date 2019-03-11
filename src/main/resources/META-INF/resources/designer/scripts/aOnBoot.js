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



function setASDCFields() {
    try {
      var location_values = defaults_props['global']['location'];
      if (location_values) {
        for (key in location_values) {
          if ($("#location").length > 0) {
            $("#location").append("<option value=\"" + key + "\">" + location_values[key] + "</opton>")
          }
        }
        $("#location").multiselect("rebuild");
      }

      var actionSet_values = defaults_props['global']['actionSet'];
      if (actionSet_values) {
        for (key in actionSet_values) {
          if ($("#actionSet").length > 0) {
            $("#actionSet").append("<option value=\"" + key + "\">" + actionSet_values[key] + "</opton>")
          }
        }
        $("#actionSet").multiselect("rebuild");
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


function setPolicyOptions() {
console.log("reset policy default options");
    try {
      var actor_values = defaults_props['policy']['actor'];
      if (actor_values) {
        for (key in actor_values) {
          if ($("#actor").length > 0) {
            $("#actor").append("<option value=\"" + key + "\">" + actor_values[key] + "</opton>")
          }
        }
        $("#actor").multiselect("rebuild");
      }

      var recipe_values = defaults_props['policy']['vnfRecipe'];
      if (recipe_values) {
        for (key in recipe_values) {
          if ($("#recipe").length > 0) {
            $("#recipe").append("<option value=\"" + key + "\">" + recipe_values[key] + "</opton>")
          }
        }
        $("#recipe").multiselect("rebuild");
      }
      var parentPolicyConditions_values = defaults_props['policy']['parentPolicyConditions'];
      if (parentPolicyConditions_values) {
        for (key in parentPolicyConditions_values) {
          if ($("#parentPolicyConditions").length > 0) {
            $("#parentPolicyConditions").append("<option value=\"" + key + "\">" + parentPolicyConditions_values[key] + "</opton>")
          }
        }
        $("#parentPolicyConditions").multiselect("rebuild");
      }
      var timeUnitsGuard_values = defaults_props['policy']['timeUnitsGuard'];
      if (timeUnitsGuard_values) {
        for (key in timeUnitsGuard_values) {
          if ($("#timeUnitsGuard").length > 0) {
            $("#timeUnitsGuard").append("<option value=\"" + key + "\">" + timeUnitsGuard_values[key] + "</opton>")
          }
        }
        $("#timeUnitsGuard").multiselect("rebuild");
      }
      function showWarn() {
        $("#paramsWarn").show();
        $('#servName').text($("#service option:selected").text());
      }
    } catch (e) {
      console.log(e)
    }
 
}


//Typically used when opening a new model/template
function reloadDefaultVariables(isTemp) {
  isTemplate = isTemp;

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

