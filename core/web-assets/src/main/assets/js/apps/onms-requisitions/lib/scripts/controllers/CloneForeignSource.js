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
* @copyright 2014-2022 The OpenNMS Group, Inc.
*/

(function() {

  'use strict';

  angular.module('onms-requisitions')

  .config(['$locationProvider', function($locationProvider) {
    $locationProvider.hashPrefix('');
  }])

  /**
  * @ngdoc controller
  * @name CloneForeignSourceController
  * @module onms-requisitions
  *
  * @requires $scope Angular local scope
  * @requires $uibModalInstance Angular UI modal instance
  * @requires foreignSource The requisition's name (a.k.a. foreign source)
  * @requires availableForeignSources The availeble requisitions (a.k.a. foreign sources)
  *
  * @description The controller for manage the modal dialog for clone the foreign source definition of a given requisition.
  */
  .controller('CloneForeignSourceController', ['$scope', '$uibModalInstance', 'foreignSource', 'availableForeignSources', function($scope, $uibModalInstance, foreignSource, availableForeignSources) {

    /**
    * @description The foreign source (a.k.a the name of the requisition).
    *
    * @ngdoc property
    * @name CloneForeignSourceController#foreignSource
    * @propertyOf CloneForeignSourceController
    * @returns {string} The foreign source
    */
    $scope.foreignSource = foreignSource;

    /**
    * @description The target foreign source (a.k.a the name of the requisition).
    *
    * @ngdoc property
    * @name CloneForeignSourceController#targetForeignSource
    * @propertyOf CloneForeignSourceController
    * @returns {string} The target foreign source
    */
    $scope.targetForeignSource = null;

    /**
    * @description The available foreign sources
    *
    * @ngdoc property
    * @name CloneForeignSourceController#$scope.availableForeignSources
    * @propertyOf CloneForeignSourceController
    * @returns {array} List of available foreign sources
    */
    $scope.availableForeignSources = availableForeignSources;

    /**
    * @description Saves the current asset
    *
    * @name AssetController:save
    * @ngdoc method
    * @methodOf AssetController
    */
    $scope.save = function() {
      $uibModalInstance.close($scope.targetForeignSource);
    };

    /**
    * @description Cancels current operation
    *
    * @name AssetController:cancel
    * @ngdoc method
    * @methodOf AssetController
    */
    $scope.cancel = function() {
      $uibModalInstance.dismiss('cancel');
    };

  }]);

}());
