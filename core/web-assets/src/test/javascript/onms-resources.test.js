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
* @copyright 2016 The OpenNMS Group, Inc.
*/

'use strict';

const angular = require('angular-js');
require('angular-mocks');
require('onms-resources');

var createController, scope, httpBackend;

beforeEach(angular.mock.module('onms-resources', function($provide) {
  $provide.value('$log', console);
}));

beforeEach(angular.mock.inject(function($rootScope, $httpBackend, $controller) {
  scope = $rootScope.$new();
  httpBackend = $httpBackend;
  createController = function() {
    return $controller('NodeResourcesCtrl', {
      '$scope': scope
    });
  };
}));

afterEach(function() {
  httpBackend.verifyNoOutstandingExpectation();
  httpBackend.verifyNoOutstandingRequest();
});

test('Controller: NodeResourcesCtrl: Validate loading process', function() {
  console.log('Testing NodeResourcesCtrl: load');
  createController();
  httpBackend.expect('GET', 'rest/resources/fornode/10').respond({
    label: 'Test Node',
    name: 'Test:Node',
    link: 'element/node.jsp?node=Test:Node',
    children: {
      resource: [{
        id: 'node[Test%3ANode].nodeSnmp[]',
        label: 'Node-level Performance Data',
        typeLabel: 'SNMP Node Data',
        externalValueAttributes: {
          hasFlows: false
        }
      },{
        id: 'node[Test%3ANode].interfaceSnmp[eth0]',
        label: 'eth0',
        typeLabel: 'SNMP Interface Data',
        externalValueAttributes: {
          hasFlows: true
        }
      },{
        id: 'node[Test%3ANode].interfaceSnmp[eth1]',
        label: 'eth1',
        typeLabel: 'SNMP Interface Data',
        externalValueAttributes: {}
      }]
    }
  });
  scope.init(10,null);
  httpBackend.flush();
  console.log('Original Resources: ' + angular.toJson(scope.resources));
  console.log('Filtered Resources: ' + angular.toJson(scope.filteredResources));
  expect(scope.nodeLink).toEqual('element/node.jsp?node=Test:Node');
  expect(scope.nodeLabel).toEqual('Test Node');
  expect(scope.hasResources).toEqual(true);
  expect(scope.resources['SNMP Node Data'].length).toEqual(1);
  expect(scope.resources['SNMP Interface Data'].length).toEqual(2);
  expect(scope.filteredResources['SNMP Node Data'].length).toEqual(1);
  expect(scope.filteredResources['SNMP Interface Data'].length).toEqual(2);
  expect(scope.filteredResources['SNMP Node Data'][0].hasFlows).toEqual(false);
  expect(scope.filteredResources['SNMP Interface Data'][0].hasFlows).toEqual(true);
  expect(scope.filteredResources['SNMP Interface Data'][1].hasFlows).toEqual(false);
});
