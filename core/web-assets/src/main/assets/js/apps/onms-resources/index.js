/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
/**
* @author Alejandro Galue <agalue@opennms.org>
* @copyright 2016-2022 The OpenNMS Group, Inc.
*/

'use strict';

const angular = require('vendor/angular-js');
const _ = require('underscore');
require('lib/onms-http');
require('apps/onms-default-apps');

angular.module('onms-resources', [
  'onms.http',
  'onms.default.apps',
  'ui.bootstrap',
  'angular-growl'
])

.config(['$locationProvider', function($locationProvider) {
  $locationProvider.hashPrefix('');
}])

.config(['growlProvider', function(growlProvider) {
  growlProvider.globalTimeToLive(3000);
  growlProvider.globalPosition('bottom-center');
}])

.filter('startFrom', function() {
  return function(input, _start) {
    const start = Number(_start);
    if (input) {
      return input.length < start ? input : input.slice(start);
    }
    return [];
  };
})

.controller('NodeListCtrl', ['$scope', '$filter', '$http', '$window', 'growl', function($scope, $filter, $http, $window, growl) {

  $scope.endUrl = 'graph/results.htm';
  $scope.resources = [];
  $scope.filteredResources = [];
  $scope.pageSize = 10;
  $scope.maxSize = 5;
  $scope.totalItems = 0;
  $scope.hasResources = false;
  $scope.loaded = false;

  $scope.goTo = function(id) {
    $window.location.href = getBaseHref() + 'graph/chooseresource.jsp?reports=all&parentResourceId=' + id + '&endUrl=' + $scope.endUrl;
  };

  $scope.update = function() {
    $scope.currentPage = 1;
    $scope.totalItems = $scope.filteredResources.length;
    $scope.numPages = Math.ceil($scope.totalItems / $scope.pageSize);
  };

  $http.get('rest/resources?depth=0').then(function succeeded(response) {
    const data = response.data;
    $scope.loaded = true;
    $scope.hasResources = data.resource.length > 0;
    $scope.resources = data.resource;
    $scope.filteredResources = $scope.resources;
    $scope.update();
  }, function errorCallback() {
     $scope.loaded = true;
     growl.error('There was a problem in retrieving resources through ReST', {ttl: 10000});
  });

  $scope.$watch('resourceFilter', function() {
    $scope.filteredResources = $filter('filter')($scope.resources, $scope.resourceFilter);
    $scope.update();
  });

}])

.controller('NodeResourcesCtrl', ['$scope', '$filter', '$http', '$window', 'growl', function($scope, $filter, $http, $window, growl) {

  $scope.searchQuery = undefined;
  $scope.resources = {};
  $scope.hasResources = false;
  $scope.filteredResources = {};
  $scope.isCollapsed = {};
  $scope.nodeLink = undefined;
  $scope.nodeLabel = undefined;
  $scope.nodeCriteria = undefined;
  $scope.url = 'graph/results.htm';
  $scope.reports = 'all';
  $scope.loaded = false;
  $scope.generatedId = '';

  $scope.init = function(nodeCriteria, reports, endUrl) {
    if (!nodeCriteria) {
      return;
    }
    // Update node criteria in scope.
    $scope.nodeCriteria = nodeCriteria;
    
    if (reports) {
      $scope.reports = reports;
    }
    if (endUrl) {
      $scope.url = endUrl;
    }

    $http.get('rest/resources/fornode/'+nodeCriteria).then(function succeeded(response) {
      const data = response.data;
      $scope.nodeLink = data.link;
      $scope.nodeLabel = data.label;
      $scope.loaded = true;
      $scope.hasResources = data.children.resource.length > 0;
      var reduced = _.map(data.children.resource, function (obj) {
        var resource = {
          id: obj.id,
          label: obj.label,
          typeLabel: obj.typeLabel,
          checked: false,
          ifIndex: parseInt(obj.externalValueAttributes.ifIndex, 10), // will return NaN if not set
          hasFlows: typeof obj.externalValueAttributes.hasFlows === 'undefined' ? false : JSON.parse(obj.externalValueAttributes.hasFlows),
          hasIngressFlows: typeof obj.externalValueAttributes.hasIngressFlows === 'undefined' ? false : JSON.parse(obj.externalValueAttributes.hasIngressFlows),
          hasEgressFlows: typeof obj.externalValueAttributes.hasEgressFlows === 'undefined' ? false : JSON.parse(obj.externalValueAttributes.hasEgressFlows)
        };
        $scope.updateFlowUrlForResource(nodeCriteria, resource);
        return resource;
      });
      $scope.resources = _.groupBy(_.sortBy(reduced, function(r) {
        var type = r['typeLabel'];
        return (type === 'SNMP Node Data' || type === 'SNMP Interface Data') ? Infinity : type;
      }), 'typeLabel');
      // Perform a shallow copy of the resource map - the resources may be updated asynchronously
      // with additional attributes
      $scope.filteredResources = {};
      for (var k in $scope.resources) {
        if (Object.prototype.hasOwnProperty.call($scope.resources, k)) {
          $scope.filteredResources[k] = $scope.resources[k];
        }
      }
    }, function errorCallback() {
       $scope.loaded = true;
       growl.error('There was a problem in retrieving resources through ReST', {ttl: 10000});
    });
  };

  $scope.updateFlowUrlForResource = function(nodeCriteria, resource) {
    if ((!resource.hasIngressFlows && !resource.hasEgressFlows) || isNaN(resource.ifIndex)) {
      // No flows, or not an interface, nothing to do
      return;
    }

    $http({
      url: 'rest/flows/flowGraphUrl',
      method: 'GET',
      params: {
        exporterNode: nodeCriteria,
        ifIndex: resource.ifIndex
      }
      }).then(function succeeded(response) {
        // Update the flowGraphUrl on the associated resource
        resource.flowGraphUrl = response.data.flowGraphUrl;
      }, function errorCallback() {
        // pass
      });
  };

  $scope.checkAll = function(check) {
    for (var key in $scope.resources) {
      if ($scope.resources.hasOwnProperty(key)) {
        _.each($scope.filteredResources[key], function(r) {
          r.selected = check;
        });
      }
    }
  };

  $scope.graphSelected = function() {
    var selected = [];
    for (var key in $scope.resources) {
      if ($scope.resources.hasOwnProperty(key)) {
        _.each($scope.filteredResources[key], function(r) {
          if (r.selected) {
            selected.push(r.id);
          }
        });
      }
    }
    $scope.doGraph(selected);
  };

  $scope.graphAll = function() {
    // Graph All will render all graphs for specific node. Controller will fetch specific resources.
    if ($scope.nodeCriteria) {
      $window.location.href = getBaseHref() + $scope.url + '?nodeCriteria=' + $scope.nodeCriteria + ($scope.reports ? '&reports=' + $scope.reports : '');
    } else {
      growl.error('Invalid node.');
    }
  };

  $scope.doGraph = function (selected) {
    // Custom report graphs doesn't support generatedId
    if(($scope.url === "graph/adhoc2.jsp")) {
       $scope.setResourceIds(selected);
       return;
    }
    // Save resources with an ID and form url with generatedId.
    if (selected.length > 0) {
      $http.post('rest/resources/generateId', selected)
        .then(function doGraphSuccess(response) {
          $scope.generatedId = response.data;
          if ($scope.generatedId) {
            $window.location.href = getBaseHref() + $scope.url + '?generatedId=' + $scope.generatedId + ($scope.reports ? '&reports=' + $scope.reports : '');
          } else {
            $scope.setResourceIds(selected);
          }
        }, function doGraphError() {
          $scope.setResourceIds(selected);
        });
    } else {
      growl.error('Please select at least one resource.');
    }
  };

  $scope.setResourceIds = function (selected) {
    for (var i = 0; i < selected.length; i++) {
      selected[i] = 'resourceId=' + selected[i];
    }
    $window.location.href = getBaseHref() + $scope.url + '?' + selected.join('&') + ($scope.reports ? '&reports=' + $scope.reports : '');
  };

  $scope.$watch('searchQuery', function() {
    $scope.filteredResources = {};
    for (var key in $scope.resources) {
      if ($scope.resources.hasOwnProperty(key)) {
        $scope.filteredResources[key] = $filter('filter')($scope.resources[key], $scope.searchQuery);
      }
    }
  });

}]);
