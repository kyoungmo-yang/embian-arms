define([
	'angular',
	'app',
	'underscore',
	'kbn',
	'moment',
	'require',
	'jquery',
	'services/datasourceSrv'
],
function (angular, app, _, kbn, moment, require, $) {
	var module = angular.module('kibana.controllers');
	module.controller('searchlogs', function($scope, $timeout, datasourceSrv, dashboard) {
		var DATA_INDEX = 0;
		
		$scope.search_table = [{'height':'200px', 'panels':[{'title':'검색 결과', 'span':12, 'type':'stable'}]}];

		$scope.init = function() {
			$scope.datasource = datasourceSrv.get('influxdb');
			
			var time = this.filter.timeRange(true);
	        if(time) {
	        	$scope.between = {from:time.from.getTime()/1000, to:time.to.getTime()/1000};
	        	$scope.time = getScopeTimeObj(time.from,time.to);
	        }
	        $scope.customTime();
			
			
			$scope.tag_display = [];
			$scope.keywords = [];
			$scope.__keyword = '';

			$scope.logtypes = ['access log', 'error log'];
			$scope.log_type_select = $scope.logtypes[0];
			$scope.set_logtype();
			
			$scope.show_keyword_list = false;
			$scope.setNowDisabled = true;
			$scope.error = false;
			
			$scope.columns = [];
			$scope.points = [];
			
			$scope.loading = false;
			dashboard.current.services.filter.list[0].to = $scope.between.to;
			dashboard.current.services.filter.list[0].from = $scope.between.from;
			$timeout(function() {
				$scope.$broadcast('__search');
		      },600);
		};

		$scope.add_keyword = function(field, keyword) {
			var pattern = /[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|\&\:]/g;
			$scope.show_keyword_list = false;
			$scope.tag_display.push(field + ':' + keyword);

			var k = field+':';
			
			if($scope.ctypes[field] && $scope.ctypes[field] == 'number') {
				k += keyword;
			} else {
				k += "*" + keyword.replace(pattern, "\\$&") + "*";
			}
			
			if ($scope.keywords.indexOf(k) === -1) {
				$scope.keywords.push(k);
	        }
			$scope.__keyword = '';
			
			$("#__search_input").focus();
		};

		$scope.handle_keyword = function(event) {  
			$scope.show_keyword_list = ($scope.__keyword.length != 0);
			$scope.error = false;
			
	       	 switch (event.which) {
	            	// SPACE BAR
	                case 32:
	                	if($scope.__keyword.length != 0) {
	                		$scope.error = false;
	                		$scope.add_keyword("message", $scope.__keyword);
	                	} else {
	                		$scope.error = "검색어를 입력해주세요.";
	                	}
	                break;
	                
	                // ENTER
	                case 13:
	                	$scope.get_data($scope.keywords);
	                	$scope.__keyword = '';
	                	$scope.show_keyword_list = false;
	                break;
	       	 }
       	};
		
		$scope.set_logtype = function () {
			$scope.keywords = [];
			$scope.__keyword = '';
			$scope.error = false;
			
			
			if($scope.log_type_select === 'error log') {
				$scope.logtype = 'error.log';
			} else {
				$scope.logtype = 'service.log';
			}
			$scope.points = [];
			$scope.makes_columns();
		};
		
		$scope.makes_colIndexes = function(columns) {
			var ci = [];

			for(var idx = 0; idx<$scope.indexes.length; idx++) {
				for(var i = 0; i<columns.length; i++) {
					if($scope.columns[$scope.indexes[idx]].name == columns[i]) {
						ci[idx] = i;
						
						break;
					}
				}
			}
			
			$scope.colIndexes = ci;
		};
		
		$scope.makes_columns = function () {
			$scope.columns = [];
			$scope.indexes = [];
			$scope.ctypes = [];
			var count=4;
			
			$scope.loading = true;
			
			$scope.datasource.rawQuery('select * from ' + $scope.logtype + ' where time < '+ $scope.between.to +'s and time > '+ $scope.between.from +'s limit 1').then(function(data){
				var columns = data[DATA_INDEX].columns.slice(2, data[DATA_INDEX].columns.length);
				var point = data[DATA_INDEX].points[DATA_INDEX].slice(2, data[DATA_INDEX].columns.length);
				
				for(var i=0; i<columns.length; i++) {
					if(_.isNumber(point[i])) {
						$scope.ctypes[columns[i]] = 'number';
					} else {
						$scope.ctypes[columns[i]] = 'string';
					}
				}
				
				columns = columns.sort();
				for(var i=0; i<columns.length; i++) {
					if(columns[i] == "time_local") {
						$scope.indexes[0] = i;
					}
					else if(columns[i] == "hostname") {
						$scope.indexes[1] = i;
					}
					else if(columns[i] == "request") {
						$scope.indexes[2] = i;
					}
					else if(columns[i] == "message") {
						$scope.indexes[3] = i;
					}
					else if(columns[i] == "request_params") {
						$scope.indexes[3] = i;
						
					}
					else {
						$scope.indexes[count++] = i;
					}
					
					var oper = '=~';
					
					if(_.isNumber(point[i])) {
						oper = '=';
						$scope.ctypes[columns[i]] = 'number';
					} else {
						$scope.ctypes[columns[i]] = 'string';
					}
					
					$scope.columns.push({name:columns[i], oper:oper});
				}
				$scope.loading = false;
			});
		};
		
		$scope.get_data = function(keywords) {
			if(keywords.length == 0 && $scope.__keyword =='') {
				$scope.error = "검색어를 입력해주세요.";
				return;
			}
			
			if (!$scope.tmp && $scope.__keyword !='') {
				$scope.error = false;
				$scope.add_keyword("message", $scope.__keyword);
				$scope.get_data(keywords);
				return;
			} else {
				$scope.points = [];
				$scope.h_key = [];
				$scope.loading = true;
				$scope.error = false;

				dashboard.current.services.query.list[0].query = keywords.join(' AND ');
				dashboard.current.services.filter.list[0].to = $scope.between.to;
				dashboard.current.services.filter.list[0].from = $scope.between.from;
				
				$scope.$broadcast('__search');
				
				for(var k in $scope.tag_display) {
					$scope.h_key.push($scope.tag_display[k].split(':')[1]);
				}
				$("#keywords-tmp").val($scope.h_key.join(' '));
				
				$scope.loading = false;
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

	      // Set the filter
	    	$scope.panel.filter_id = $scope.filter.setTime(_filter);
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
			$scope.error = false;
			var _from = datepickerToLocal(time.from.date),
			    _to = datepickerToLocal(time.to.date),
			    _t = time;
			
			
			    _from.setHours(_t.from.hour,_t.from.minute,_t.from.second, _t.from.millisecond);
			    _to.setHours(_t.to.hour,_t.to.minute,_t.to.second, _t.to.millisecond);

			// Check that the objects are valid and to is after from
			if(isNaN(_from.getTime()) || isNaN(_to.getTime()) || _from.getTime() >= _to.getTime()) {
				$scope.error = "invalid time range";
				return false;
			}
			$scope.between.from = _from.getTime()/1000;
			$scope.between.to = _to.getTime()/1000;
	      return $scope.between;
	    };
		
	    // Do not use the results of this function unless you plan to use setHour/Minutes/etc on the result
	    var datepickerToLocal = function(date) {
	    	date = moment(date).clone().toDate();
//	    	return moment(new Date(date.getTime() + date.getTimezoneOffset() * 60000)).toDate();
	    	return date;
	    };
	});
});