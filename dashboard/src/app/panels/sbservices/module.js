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
  'services/datasourceSrv',
],

function (angular, app, _, kbn, require) {
  'use strict';

  /*******************************************************************************************************
  *             SBSERVICES
  *******************************************************************************************************/

  var module = angular.module('kibana.panels.sbservices', []);
  app.useModule(module);

  module.controller('sbservices', function($scope, $rootScope, $location, datasourceSrv, dashboard) {
	  var pcolumns = ['service'];
	  var dataidx = null;
	  var service_template = {
		  title:"",
		  service:"",
		  span:1,
		  icon:"../img/service.png",
		  type:"sbservice",
		  href:"",
		  param: "",
		  source: {
			  query:"",
			  interval:0
		  }
	  };
	  
    	$scope.panelMeta = {
    		interval: null,
    		loading: false
    	}
		
  	$scope.init = function() {
  		$scope.initBaseController(this, $scope);
		$scope.services = [];
		$scope.datasource = datasourceSrv.get('influxdb');
		service_template.href = $scope.panel.href;
		$scope.histories = [{dn:dashboard.current.dashname, t:"0"}];
		service_template.param = $scope.panel.param;
	  	$scope.get_data();
        $scope.$on('refresh',function() {
          if ($scope.panelMeta.loading) { return; }
          $scope.get_data();
        });
  	};
	
	$scope.new_service = function(name, source) {
		var h = $scope.histories[$scope.histories.length-1];
		var nservice = angular.copy(service_template);
		nservice.title = name;
		nservice.service = name;
		nservice.href = nservice.href + "/sub?" + nservice.param + "=" + name + '&history=[{"dn":"'+h.dn+'","n":"'+$scope.panel.title+'","t":"'+h.t+'"}]';
		nservice.source.query1 = source.query1.replace("%SERVICE%", name);
		nservice.source.query2 = source.query2.replace("%SERVICE%", name);
		nservice.source.interval = source.interval;
		nservice.source.column = source.column;
		nservice.source.series = source.series;
		nservice.source.rawQuery = source.rawQuery;
		
		return nservice;
	};
	
	$scope.new_services = function(array, idx) {
		var services = [];
		for(var i = 0; i < array.length; i++) {
			services.push($scope.new_service(array[i][idx], $scope.panel.source));
		}
		
		return services;
	};
	
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
	
  	$scope.get_data = function() {
  		$scope.panelMeta.loading = true;
		
		var options = {targets:[], useOwnHandler:true, range:{}};
		options.range = this.filter.timeRange(false);
		options.targets.push($scope.panel.source);
		$scope.datasource.query($scope.filter, options)
  		// $scope.datasource.rawQuery($scope.panel.source.query)
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
				if (!has_service(results[0].points[i][dataidx[0]])) {
					$scope.services.push($scope.new_service(results[0].points[i][dataidx[0]], $scope.panel.source));
				}
			}
			
			
		}
		
		$scope.panelMeta.loading = false;
	};
	
	var has_service = function(service) {
		for(var i = 0; i < $scope.services.length; i++) {
			if ($scope.services[i].service === service) {
				return true;
			}
		}
		
		return false;
	};
  });

  /*******************************************************************************************************
  *             SBSERVICE
  *******************************************************************************************************/

  var module = angular.module('kibana.panels.sbservice', []);
  app.useModule(module);

  module.controller('sbservice', function($scope, $rootScope, timer, datasourceSrv, alertSrv, dashboard) {
	var pcolumns = ['level', 'hostname', 'message', 'title'];
	var dataidx = null;
	var color_map = {l1:'../img/l1_status', l2:'../img/l2_status', l3:'../img/l3_status', l4:'../img/l4_status'};
	
	$scope.status = {tot:0, l1:0, l2:0, l3:0, l4:0, color:color_map.l1};
	
  	$scope.panelMeta = {
  		interval: null,
  		loading: false,
		totloading: false
  	};
	
	$scope.init = function() {
		$scope.alertList=[];
		
		$scope.initBaseController(this, $scope);
		$scope.datasource = datasourceSrv.get('influxdb');
	  	$scope.get_tot();
        $scope.$on('refresh',function() {
          if ($scope.panelMeta.totloading || $scope.panelMeta.loading) { return; }
          $scope.get_tot();
        });
	};
	
	$scope._icon = function(_status, _level) {
		if(_level != 'l4' && _status.l4 > 0) {
			return color_map[_level]+'0.png';
		}
		
		if(_level != 'l3' && _status.l3 > 0) {
			return color_map[_level]+'0.png';
		}
		
		if(_level != 'l2' && _status.l2 > 0) {
			return color_map[_level]+'0.png';
		}
		
		return _status[_level] > 0 ? color_map[_level]+'.png' : color_map[_level]+'0.png'
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
		var pcolumns1 = ['total'];
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
		$scope.status.color = color_map.l1;
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
