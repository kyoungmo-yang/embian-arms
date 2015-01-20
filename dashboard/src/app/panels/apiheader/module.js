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

  var module = angular.module('kibana.panels.apiheader', []);
  app.useModule(module);

  module.controller('apiheader', function($scope, $routeParams, $location, dashboard) {
	var mainPage = dashboard.current.history_paths.main_page;
	var servicePage = dashboard.current.history_paths.service_page;
	var apiPage = dashboard.current.history_paths.api_page;
	var pre_histories = "";
	
	var hostparam = $routeParams.hostname ? '&hostname='+$routeParams.hostname : '';
  	$scope.panelMeta = {
  		interval: null,
  		loading: false,
		totloading: false
  	};
	
  	var push_history = function(history) {
  		$scope.histories.push(history);
  	};
 
  	var init_histories = function() {
  		$scope.histories = angular.fromJson($routeParams.history);
  		
  		pre_histories = angular.copy($scope.histories);
		pre_histories.pop();
  		pre_histories = angular.toJson(pre_histories);
  		
  		push_history({dn:dashboard.current.dashname});
  	};
  	
  	$scope.path = function(type) {
		if(type == "0") {
			return mainPage;
		}
		if(type == "1") {
			return servicePage+'?service='+$routeParams.service+'&history='+pre_histories;
		}
		
		if (type == "2") {
			return servicePage+'?service='+$routeParams.service+'&hostname='+$routeParams.hostname+'&history='+pre_histories;
		}
		
		return apiPage+'?history='+$routeParams.history+'&service='+$routeParams.service+hostparam+'&request='+$routeParams.request;
	};
	
	$scope.init = function() {
		init_histories();
		$scope.initBaseController(this, $scope);
	};
  });
});
