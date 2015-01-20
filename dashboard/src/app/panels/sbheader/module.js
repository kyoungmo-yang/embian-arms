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

  var module = angular.module('kibana.panels.sbheader', []);
  app.useModule(module);

  module.controller('sbheader', function($scope, $rootScope, datasourceSrv, dashboard, $routeParams, $location, alertSrv) {
	var mainPage = dashboard.current.history_paths.main_page;
	var servicePage = dashboard.current.history_paths.service_page;
	  
	var main_page = dashboard.current.main_page;
	
	var pre_histories = "";
	
	var pcolumns = ['level', 'hostname', 'message', 'title'];
	var dataidx = null;
	
	var color_map = {l1:'../img/l1_status.png', l2:'../img/l2_status.png', l3:'../img/l3_status.png', l4:'../img/l4_status.png'};
	
	$scope.status = {tot:0, l1:0, l2:0, l3:0, l4:0, color:color_map.l1};
	$scope.has_listener = true;
  	$scope.panelMeta = {
  		interval: null,
  		loading: false,
		totloading: false
  	};
  	
  	var push_history = function(history) {
 		$scope.histories.push(history);
  	};
  	
  	var pop_history = function() {
  		return $scope.histories.pop();
  	}
  	
  	var init_histories = function() {
  		$scope.histories = angular.fromJson($routeParams.history);
  		pre_histories = angular.copy($scope.histories);
  		pre_histories = angular.toJson(pre_histories);
  		
  		push_history({dn:dashboard.current.dashname, n:$routeParams.service, t:'1'});
  		$scope.view_status = 'allview';
  	};
  	
	$scope.init = function() {
		init_histories();
		$scope.alertList=[];
		$scope.initBaseController(this, $scope);
		$scope.datasource = datasourceSrv.get('influxdb');
		$scope.allview_rows = dashboard.current.content.rows;
		
		if(!$routeParams.hostname) {
			dashboard.current.rows = $scope.allview_rows;
		}
		$rootScope.$on('__service_hostview', function(e, rows, host){
			if(!$scope.has_listener) return ;
			
			pop_history();
			push_history({dn:dashboard.current.dashname, n:host.hostname, t:'2'});
			
			dashboard.current.rows = rows;
			$scope.view_status = 'hostview';
			
		});
		
	  	$scope.get_tot();
        $scope.$on('refresh',function() {
          if ($scope.panelMeta.totloading || $scope.panelMeta.loading) { return; }
          $scope.get_tot();
        });
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
	};
	
	$scope.show_allview = function() {
		$rootScope.$broadcast('__service_allview');
		dashboard.current.rows = [];
		dashboard.current.rows = $scope.allview_rows;
		
		if($scope.view_status === 'hostview') {
			pop_history();
			push_history({dn:dashboard.current.dashname, n:$routeParams.service, t:'1'});
		}
		
		$scope.view_status = 'allview';
	};
	
	$scope.get_tot = function() {
  		$scope.panelMeta.totloading = true;
		
		var options = {targets:[], useOwnHandler:true, range:{}};
		$scope.time_range = this.filter.timeRange(false);
		options.range = $scope.time_range;
		var source = angular.copy($scope.panel.source);
		source.query = $scope.panel.source.query1;
		options.targets.push(source);
		
		$scope.datasource.query($scope.filter, options)
  		// $scope.datasource.rawQuery($scope.panel.source.query1)
  		                 .then($scope.totHandler)
  						 .then(null, function(err) {
							 // do nothing
  						 });
	};
	
	$scope.totHandler = function(results) {
		if(!results || !results.data) {
			return ;
		}
		
		results = results.data;		
		if(!results || !results[0] || typeof results[0].columns === 'undefined') {
			$scope.panelMeta.totloading = false;
			return ;
		}
		
		var columns = results[0].columns;
		var dataidx1 = null;
		var pcolumns1 = ['total']
		if(!dataidx1 || !dataidx1.length != pcolumns1.length) {
			dataidx1 = [];

			for(var j in pcolumns1) {
				for(var i in columns) {
					if(columns[i] == pcolumns1[j]) {
						dataidx1.push(i);
						break;
					}
				}
		
				if(dataidx1.length <= j) {
					dataidx1.push(-1);
				}
			}
		}
				
		if(results[0].points.length > 0) {
			var total = results[0].points[0][dataidx1[0]];
			$scope.status.tot = total;
		}
		$scope.panelMeta.totloading = false;
		$scope.get_data();
	};
		
  	$scope.get_data = function() {
  		$scope.panelMeta.loading = true;
  		$scope.datasource.rawQuery($scope.panel.source.query2)
  		                 .then($scope.dataHandler)
  						 .then(null, function(err) {
  						 	// $scope.panel.error = err.message || "Timeseries data request error";
  							$scope.panelMeta.loading = false;
  							// $scope.inspector.error = err;
  							// $scope.render([]);
  						 });
  	};
	
  	$scope.clearAlert = function() {
  		for(var i=0; i<$scope.alertList.length; i++) {
			alertSrv.clear($scope.alertList[i]);	
		}
		
		$scope.alertList = [];
  	}
	
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
  	
	$scope.dataHandler = function(results) {
		$scope.status.color = color_map.l1
		$scope.status.l1 = $scope.status.tot;
		$scope.status.l2 = 0;
		$scope.status.l3 = 0;
		$scope.status.l4 = 0;
		
		$scope.clearAlert();
		
		if(!results || !results[0] || typeof results[0].columns === 'undefined') {
			$scope.panelMeta.loading = false;
			return ;
		}
		
		var columns = results[0].columns;
		if(!dataidx || !dataidx.length != pcolumns.length) {
			dataidx = [];

			for(var j in pcolumns) {
				for(var i in columns) {
					if(columns[i] == pcolumns[j]) {
						dataidx.push(i);
						break;
					}
				}
		
				if(dataidx.length <= j) {
					dataidx.push(-1);
				}
			}
		}
		
		var _alerts = [];
		
		if(results[0].points) {
			for(var i = 0; i < results[0].points.length; i++) {
				var level = results[0].points[i][dataidx[0]];
				var hostname = results[0].points[i][dataidx[1]];
				var message = results[0].points[i][dataidx[2]];
				var title = results[0].points[i][dataidx[3]];
				
				_alerts.push([level, hostname, message, title]);
			}
			
			$scope.sort_by(_alerts, 0, 'asc');
			var _pre_host = '';
			for(var i = 0; i < _alerts.length; i++) {
				var level = _alerts[i][0];
				var hostname = _alerts[i][1];
				var message = _alerts[i][2];
				var title = _alerts[i][3];
				
				if(!message || _.isUndefined(message)) {
					$scope.alertList.push(alertSrv.set(hostname+":",title , level));
				} else {
					$scope.alertList.push(alertSrv.set(hostname+":", title+'-'+message, level));
				}
				
				if(hostname != _pre_host) {
					if(level === 'l1') {
						$scope.status.l2 += 1;
					} else if(level === 'l2') {
						$scope.status.l3 += 1;
					} else if(level === 'l3') {
						$scope.status.l4 += 1;
					}

					_pre_host = hostname;
				}
			}

			$scope.status.l1 = $scope.status.tot - $scope.status.l2 - $scope.status.l3 - $scope.status.l4;
		}
		
		if($scope.status.l2 > 0) {
			$scope.status.color = color_map.l2
		}
		
		if($scope.status.l3 > 0) {
			$scope.status.color = color_map.l3
		}
		
		if($scope.status.l4 > 0) {
			$scope.status.color = color_map.l4
		}
		
		$scope.panelMeta.loading = false;
	};
  });
});
