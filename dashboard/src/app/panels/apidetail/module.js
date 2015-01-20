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
  'moment',
  'kbn',
  'services/datasourceSrv',
  'controllers/metricKeys'
],

function (angular, app, _, moment, kbn) {
  'use strict';

  var module = angular.module('kibana.panels.apidetail', []);
  app.useModule(module);

  module.controller('apidetail', function($scope, $rootScope, datasourceSrv, dashboard, $routeParams) {
	var pcolumns = ['hostname'];
	var dataidx = null;
	
  	$scope.panelMeta = {
  		interval: null,
  		loading: false
  	}
	
	$scope.filters = {f1:'service', f2:'', f3:'1m', f4:'Last 1h'};
	
	$scope.init = function() {
		$scope.initBaseController(this, $scope);
        var time = this.filter.timeRange(true);
        if(time) {
          $scope.panel.now = this.filter.timeRange(false).to === "now" ? true : false;
          $scope.time = getScopeTimeObj(time.from,time.to);
        }
		
		if(this.filter.time.from) {
			var from = this.filter.time.from.replace(/now-/g, '');
			for(var i = 0; i < $scope.panel.filters.f4.length; i++) {
				if($scope.panel.filters.f4[i].replace(/Last /g, '') == from) {
					$scope.filters.f4 = $scope.panel.filters.f4[i];
					break;
				}
			}
		}
		
		$scope.customTime();
		
		$scope.datasource = datasourceSrv.get('influxdb');
		
		$scope.panel.filters.f1.push($routeParams.service);
		$scope.filters.f1 = $routeParams.service;
		$scope.filters.f2 = $routeParams.hostname ? $routeParams.hostname : 'All';
		
		$scope.get_data();
	};
		
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
	
  	$scope.get_data = function() {
  		$scope.panelMeta.loading = true;
		var options = {targets:[], useOwnHandler:true, range:{}};
		options.range = this.filter.timeRange(false);
		var target = angular.copy($scope.panel.source);
		options.targets.push(target);
		$scope.datasource.query($scope.filter, options)
		                 .then($scope.dataHandler)
						 .then(null, function(err) {
							$scope.panelMeta.loading = false;
						 });
  	};
	
	$scope.new_filters = function(array, index) {
		var filters = ['All'];
		for(var i = 0; i < array.length; i++) {
			filters.push(array[i][index]);
		}
		
		return filters;
	}

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
			$scope.panel.filters.f2 = $scope.new_filters(results[0].points, dataidx[0]);
		}
		
		$scope.panelMeta.loading = false;
	};
	
	$scope.filter_changed = function(field, value) {
		var val = angular.copy(value);
		if (field === 'hostname' || field === 'service') {
			if(value === 'All') {
				val = "''";
			} else {
				val = "'" + val + "'";
			}
		}
		
		apply_condition(field, val);
		$scope.$broadcast('refresh');
	};
	
	var apply_condition = function(field, value) {
		if(field === 'interval') {
			for(var i = 0; i < $scope.panel.graphs.length; i++) {
				var graph = $scope.panel.graphs[i];
				graph.targets[0].interval = value;
			}
		} else if (field === 'time') {
			if((/^Last /).test(value)) {
				var val = value.replace('Last ', '');
				var t = {from: 'now-'+val, to: 'now'};
				$scope.setAbsoluteTimeFilter(t);
			} else {
				$scope.setAbsoluteTimeFilter(value);
			}
		} else {
			var groupby_field = null;
			
			if (field === 'hostname') {
				if(value === "''") {
					groupby_field = 'hostname';
				} else if($scope.editor.index != 2) {
					groupby_field = 'request';
				}
			}
			
			for(var i = 0; i < $scope.panel.graphs.length; i++) {
				var graph = $scope.panel.graphs[i];
				graph.targets[0].condition_key = field;
				graph.targets[0].condition_value = value;
				
				if(groupby_field != null) {
					graph.targets[0].groupby_field = groupby_field;
				}
			}
		}
	};
	
	////////////////////////////////////////////////////////////////////////////////////////
	//  Time Picker
	////////////////////////////////////////////////////////////////////////////////////////
    $scope.setAbsoluteTimeFilter = function (time) {
      // Create filter object
      var _filter = _.clone(time);
      if($scope.panel.now) {
        _filter.to = "now";
      }
	  
	  var _refresh = (_filter.from === 'now-5m' || _filter.from === 'now-5m'|| _filter.from === 'now-15m'|| _filter.from === 'now-1h');
	  
      // Set the filter
      $scope.panel.filter_id = $scope.filter.setTime(_filter, _refresh, false);
      //
      // // Update our representation
      // $scope.time = getScopeTimeObj(time.from,time.to);
    };
	
    // ng-pattern regexs
    $scope.patterns = {
      date: /^[0-9]{2}\/[0-9]{2}\/[0-9]{4}$/,
      hour: /^([01]?[0-9]|2[0-3])$/,
      minute: /^[0-5][0-9]$/,
      second: /^[0-5][0-9]$/,
      millisecond: /^[0-9]*$/
    };
	
    $scope.customTime = function() {
      $scope.temptime = cloneTime($scope.time);

      // Date picker needs the date to be at the start of the day
      $scope.temptime.from.date.setHours(1,0,0,0);
      $scope.temptime.to.date.setHours(1,0,0,0);
    };
	
    $scope.setNow = function() {
      $scope.time.to = getTimeObj(new Date());
    };
	
    var getTimeObj = function(date) {
      return {
        date: new Date(date),
        hour: pad(date.getHours(),2),
        minute: pad(date.getMinutes(),2),
        second: pad(date.getSeconds(),2),
        millisecond: pad(date.getMilliseconds(),3)
      };
    };
	
    var pad = function(n, width, z) {
      z = z || '0';
      n = n.toString();
      return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
    };
	
    var cloneTime = function(time) {
      var _n = {
        from: _.clone(time.from),
        to: _.clone(time.to)
      };
      // Create new dates as _.clone is shallow.
      _n.from.date = new Date(_n.from.date);
      _n.to.date = new Date(_n.to.date);
      return _n;
    };

    var getScopeTimeObj = function(from,to) {
      return {
        from: getTimeObj(from),
        to: getTimeObj(to)
      };
    };
	
    // Constantly validate the input of the fields. This function does not change any date variables
    // outside of its own scope
    $scope.validate = function(time) {
		$scope.panel.error = false;
      var _from = datepickerToLocal(time.from.date),
        _to = datepickerToLocal(time.to.date),
        _t = time;

		_from.setHours(_t.from.hour,_t.from.minute,_t.from.second,_t.from.millisecond);
		_to.setHours(_t.to.hour,_t.to.minute,_t.to.second,_t.to.millisecond);

		// Check that the objects are valid and to is after from
		if(isNaN(_from.getTime()) || isNaN(_to.getTime()) || _from.getTime() >= _to.getTime()) {
		  $scope.panel.error = "invalid time range";
		  return false;
		}
		var t = {from:_from,to:_to};
		$scope.filter_changed('time', t);
      return t;
    };
	
    // Do not use the results of this function unless you plan to use setHour/Minutes/etc on the result
    var datepickerToLocal = function(date) {
      date = moment(date).clone().toDate();
      return moment(new Date(date.getTime() + date.getTimezoneOffset() * 60000)).toDate();
    };
  });
});
