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

  var module = angular.module('kibana.panels.sbhosts', []);
  app.useModule(module);

  module.controller('sbhosts', function($scope, $rootScope, datasourceSrv, $routeParams, dashboard) {
	var pcolumns = ['hostname'];
	var dataidx = null;
	var color_map = {l1:'../img/l1_status.png', l2:'../img/l2_status.png', l3:'../img/l3_status.png', l4:'../img/l4_status.png'};
	
	$scope.has_listener = true;
	
  	$scope.panelMeta = {
  		interval: null,
  		loading: false,
		stsloading: false
  	};
	
	$scope.host_status = [];
	
	$scope.status_color = function(hostname) {
		var color = $scope.host_status[hostname];
		if(color) {
			return color;
		}
		
		return color_map.l1;
	}
	
	$scope.init = function() {
		$scope.initBaseController(this, $scope);
		$scope.hosts = [];
		$scope.datasource = datasourceSrv.get('influxdb');
		$scope.get_data();
		$rootScope.$on('__service_allview', function(e){
			if(!$scope.has_listener) return ;
			$scope.select();
		});
		
	  	$scope.get_data();
        $scope.$on('refresh',function() {
          if ($scope.panelMeta.stsloading || $scope.panelMeta.loading) { return; }
          $scope.get_data();
        });
	};
	
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
	
	$scope.new_host = function(hostname) {
		return {hostname:hostname, selected:false};
	};
	
	$scope.select = function(host) {
		for(var i = 0; i < $scope.hosts.length; i++) {
			$scope.hosts[i].selected = false;
		}
		if (host) {
			host.selected = true;
			var rows = angular.copy($scope.panel.rows);
			for(var i = 0; i < rows.length; i++) {
				for(var j = 0; j < rows[i].panels.length; j++) {
					rows[i].panels[j].source.query = rows[i].panels[j].source.query.replace('%HOSTNAME%', host.hostname);
				}
			}
			$rootScope.$broadcast('__service_hostview', rows, host);
			$routeParams['hostname'] = host.hostname;
		} else {
			delete $routeParams['hostname'];
		}
	};
	
	$scope.new_hosts = function(array, idx) {
		var hosts = [];
		for(var i = 0; i < array.length; i++) {
			hosts.push($scope.new_host(array[i][idx]));
		}
		
		return hosts;
	};
	
	$scope.get_status = function() {
  		$scope.panelMeta.stsloading = true;
		
  		$scope.datasource.rawQuery($scope.panel.source.query2)
  		                 .then($scope.statusHandler)
  						 .then(null, function(err) {
							 // do nothing
  						 });
	};
	
	var init_status = function() {
		for(var hostname in $scope.host_status) {
			$scope.host_status[hostname] = color_map.l1;
		}
	};
	
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
	
	$scope.statusHandler = function(results) {
		init_status();
		
		if(!results || !results[0] || typeof results[0].columns === 'undefined') {
			$scope.panelMeta.stsloading = false;
			return ;
		}
		
		var columns = results[0].columns;
		var dataidx1 = null;
		var pcolumns1 = ['level', 'hostname']
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
		
		var _alerts = [];
		
		if(results[0].points) {
			for(var i = 0; i < results[0].points.length; i++) {
				var color = color_map.l1;
				var level = results[0].points[i][dataidx1[0]];
				var hostname = results[0].points[i][dataidx1[1]];
				
				_alerts.push([level, hostname, color]);
				$scope.sort_by(_alerts, 0, 'asc');
			}
			
			var _pre_host = '';
			for(var i = 0; i < _alerts.length; i++) {
				var color = _alerts[0][2];
				var level = _alerts[0][0];
				var hostname = _alerts[0][1];
				
				if(hostname != _pre_host) {
					if(level === 'l1') {
						color = color_map.l2;
					} else if(level === 'l2') {
						color = color_map.l3;
					} else if(level === 'l3') {
						color = color_map.l4;
					}
				
					$scope.host_status[hostname] = color;
				}
			};
		}
		$scope.panelMeta.stsloading = false;
	};
	
  	$scope.get_data = function() {
  		$scope.panelMeta.loading = true;
		
		var options = {targets:[], useOwnHandler:true, range:{}};
		$scope.time_range = this.filter.timeRange(false);
		options.range = $scope.time_range;
		var source = angular.copy($scope.panel.source);
		source.query = $scope.panel.source.query1;
		options.targets.push(source);
		
		$scope.datasource.query($scope.filter, options)
  		// $scope.datasource.rawQuery($scope.panel.source.query1)
  		                 .then($scope.dataHandler)
  						 .then(null, function(err) {
  						 	// $scope.panel.error = err.message || "Timeseries data request error";
  							$scope.panelMeta.loading = false;
  							// $scope.inspector.error = err;
  							// $scope.render([]);
  						 });
  	};
	
	$scope.dataHandler = function(results) {
		if(!results || !results.data) {
			return ;
		}
		
		results = results.data;		
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
		
		if(results[0].points.length > 0) {
			$scope.sort_by(results[0].points, dataidx[0], 'desc');
			for(var i = 0; i < results[0].points.length; i++) {
				if(!has_host(results[0].points[i][dataidx[0]])) {
					var host = $scope.new_host(results[0].points[i][dataidx[0]])
					$scope.hosts.push(host);
					if($routeParams.hostname && $routeParams.hostname === host.hostname) {
						$scope.select(host);
					}
				}
			}
		}
		
		$scope.panelMeta.loading = false;
		$scope.get_status();
	};
	
	var has_host = function(hostname) {
		for(var i = 0; i < $scope.hosts.length; i++) {
			if ($scope.hosts[i].hostname === hostname) {
				return true;
			}
		}
		
		return false;
	};
  });
});
