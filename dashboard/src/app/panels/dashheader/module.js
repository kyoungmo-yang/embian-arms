/** @scratch /panels/5
 * include::panels/text.asciidoc[]
 */

/** @scratch /panels/text/0
 * == text
 * Status: *Stable*
 *
 * The text panel is used for displaying static text formated as markdown, sanitized html or as plain
 * text.
 *
 */
define([
  'angular',
  'app',
  'underscore',
  'kbn',
  'require',
  'jquery',
  'services/datasourceSrv'
],

function (angular, app, _, kbn, require) {
  'use strict';

  var module = angular.module('kibana.panels.dashheader', []);
  app.useModule(module);

  module.controller('dashheader', function($scope, $routeParams, $location, dashboard) {

  	$scope.panelMeta = {
  		interval: null,
  		loading: false,
		totloading: false
  	};

	$scope.init = function() {
		$scope.histories = [{dn:dashboard.current.dashname, t:"0"}];
		$scope.initBaseController(this, $scope);
	};
  });
});
