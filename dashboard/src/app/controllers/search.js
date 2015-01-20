//define([
//	'angular',
//	'app',
//	'underscore',
//	'kbn',
//	'moment',
//	'require',
//	'jquery',
//	'services/datasourceSrv'
//],
//function (angular, app, _, kbn, moment, require, $) {
//	var module = angular.module('kibana.controllers');
//	module.controller('search', function($scope, datasourceSrv, dashboard) {
//		var DATA_INDEX = 0;
//		
//		$scope.search_table = [{'height':'200px', 'panels':[{'title':'Search Result', 'span':12, 'type':'stable'}]}];
//		
//		////////////////////////////////////////////////////////////////////////////////////////
//		//  Search
//		////////////////////////////////////////////////////////////////////////////////////////
//		$scope.init = function() {
//			$scope.datasource = datasourceSrv.get('influxdb');
//			$scope.keywords = [];
//			$scope.tag_display = [];
//			
//			var time = this.filter.timeRange(true);
//			
//	        if(time) {
//	        	$scope.between = {from:time.from.getTime()/1000, to:time.to.getTime()/1000};
//	        	$scope.time = getScopeTimeObj(time.from,time.to);
//	        }
//	        
//			$scope.customTime();
//			
//			$scope.__keyword = '';
//			$scope.logtypes = ['access log', 'error log'];
//			$scope.log_type_select = $scope.logtypes[0];
//			$scope.set_logtype();
//			
//			$scope.show_keyword_list = false;
//			$scope.setNowDisabled = true;
//			$scope.error = false;
//			$scope.columnShow = false;
//			
//			$scope.columns = [];
//			$scope.points = [];
//			
//			$scope.pageSize = 30;
//			$scope.currentPage = 0;
//		};
//		
//		$scope.reload = function () {
//			$scope.customTime();
//			
//			$scope.logtypes = ['access log', 'error log'];
//			$scope.log_type_select = $scope.logtypes[0];
//			$scope.set_logtype();
//			
//			$scope.keywords = [];
//			$scope.tag_display = [];
//			$scope.__keyword = '';
//        	$scope.show_keyword_list = false;
//			$scope.setNowDisabled=true;
//			$scope.error = false;
//			$scope.columnShow = false;
//			
//			$scope.columns = [];
//			
//			$scope.pageSize = 30;
//			$scope.currentPage = 0;
//			
//			var time = this.filter.timeRange(true);
//			
//	        if(time) {
//	        	$scope.between = {from:time.from.getTime()/1000, to:time.to.getTime()/1000};
//	        	$scope.time = getScopeTimeObj(time.from,time.to);
//	          
//	        }
//		};
//		
//		$scope.add_keyword = function(field, keyword) {
//			var pattern = /[\-\[\]\/\{\}\(\)\*\+\?\.\\\^\$\|\&\:]/g;
//			$scope.show_keyword_list = false;
//			$scope.tag_display.push(field + ':' + keyword);
//			
//			var k = field+':' + "*" + keyword.replace(pattern, "\\$&") + "*";
//			if ($scope.keywords.indexOf(k) === -1) {
//				$scope.keywords.push(k);
//	        }
//			$scope.__keyword = '';
//			
//			$("#__search_input").focus();
//		};
//		
//		$scope.handle_keyword = function(event) {
//			$scope.show_keyword_list = ($scope.__keyword.length != 0);
//			$scope.columnShow = false;
//			
//	       	 switch (event.which) {
//	            	// SPACE BAR
//	                case 32:
//	                	if($scope.__keyword.length != 0) {
//	                		$scope.error = false;
//	                		$scope.add_keyword("message", $scope.__keyword);
//	                	} else {
//	                		$scope.error = 'please set keyword';
//	                	}
//	                break;
//	                
//	                // ENTER
//	                case 13:
//	                	$scope.get_data($scope.keywords);
//	                	$scope.__keyword = '';
//	                	$scope.show_keyword_list = false;
//	                break;
//	       	 }
//       	};
//		
//		$scope.set_logtype = function () {
//			$scope.keywords = [];
//			$scope.__keyword = '';
//			$scope.error = false;
//			$scope.columnShow = false;
//			$scope.currentPage = 0;
//			
//			
//			if($scope.log_type_select === 'error log') {
//				$scope.logtype = 'error.log';
//			} else {
//				$scope.logtype = 'service.log';
//			}
//			$scope.points = [];
//			$scope.makes_columns();
//		};
//		
//		$scope.makes_colIndexes = function(columns) {
//			var ci = [];
//
//			for(var idx = 0; idx<$scope.indexes.length; idx++) {
//				for(var i = 0; i<columns.length; i++) {
//					if($scope.columns[$scope.indexes[idx]].name == columns[i]) {
//						ci[idx] = i;
//						
//						break;
//					}
//				}
//			}
//			
//			$scope.colIndexes = ci;
//		};
//		
//		$scope.makes_columns = function () {
//			$scope.columns = [];
//			$scope.indexes = [];
//			var count=4;
//			
//			$scope.loading = true;
//			
//			$scope.datasource.rawQuery('select * from ' + $scope.logtype + ' where time < '+ $scope.between.to +'s and time > '+ $scope.between.from +'s limit 1').then(function(data){
//				var columns = data[DATA_INDEX].columns.slice(2, data[DATA_INDEX].columns.length).sort();
//				var point = data[DATA_INDEX].points[DATA_INDEX].slice(2, data[DATA_INDEX].columns.length);
//				for(var i=0; i<columns.length; i++) {
//					if(columns[i] == "time_local") {
//						$scope.indexes[0] = i;
//					}
//					else if(columns[i] == "hostname") {
//						$scope.indexes[1] = i;
//					}
//					else if(columns[i] == "request") {
//						$scope.indexes[2] = i;
//					}
//					else if(columns[i] == "message") {
//						$scope.indexes[3] = i;
//					}
//					else if(columns[i] == "request_params") {
//						$scope.indexes[3] = i;
//						
//					}
//					else {
//						$scope.indexes[count++] = i;
//					}
//					
//					var oper = '=~';
//					
//					if(_.isNumber(point[i])) {
//						oper = '=';
//					}
//					
//					$scope.columns.push({name:columns[i], oper:oper});
//				}
//				$scope.loading = false;
//			});
//		};
//		
////		var regex_escape = function(keywords) {
////			var keyword_tmp = [];
////			var pattern = /[\-\[\]\/\{\}\(\)\+\?\.\\\^\$\|\&]/g;
////			
////			for(var idx in keywords) {
////				if(pattern.test(keywords[idx])) {
////					keyword_tmp.push(keywords[idx].replace(pattern, "\\$&"));
////				} else {
////				    keyword_tmp.push(keywords[idx]);
////				}
////			}
////			return keyword_tmp;
////		};
//		
////		$scope.apply_results = function(results) {
////			if(!_.isUndefined(results)) {
////				$scope.nodata = false;
////				$scope.makes_colIndexes(results.columns);
////				$scope.set_points(results);
////			} else {
////				$scope.nodata = true;
////
////				$scope.points = [];
////				$scope.index = 0;
////			}
////		};
//		
//		$scope.get_data = function(keywords) {	
//			var start_time = Date.now();
//			
//			if (!keywords || keywords.length <= 0) {
//				$scope.add_keyword("message", $scope.__keyword);
//				$scope.error = false;
//				$scope.get_data(keywords);
//				return;
//			} else {
//				$scope.points = [];
//				$scope.columnShow = true;
//				$scope.loading = true;
//				$scope.error = false;
//				
////				dashboard.current.services.query.list[0].query = regex_escape(keywords).join(' AND ');
//				dashboard.current.services.query.list[0].query = keywords.join(' AND ');
//				dashboard.current.services.filter.list[0].to = $scope.between.to;
//				dashboard.current.services.filter.list[0].from = $scope.between.from;
//
//				
//				console.log(dashboard.current.services.query.list[0].query);
//				$scope.$broadcast('__search');
//				$scope.loading = false;
//				
//				for(var k in $scope.tag_display) {
//					$scope.h_key.push($scope.tag_display[k].split(':')[1]);
//				}
//				$("#keywords-tmp").val($scope.h_key.join(' '));
//				
////				for(var i=0; i<keywords.length ; i++) {
//////					console.log();
////					$(".odd").highlight(keywords[i].split(":")[1]);
////				}
//				
////				$scope.duration = (Date.now()-start_time)/1000;
////				var query = build_query(build_typed_keywords(keywords));
////				$scope.currentPage = 1;
////				
////				var fQuery = query+" order desc limit " + ($scope.currentPage*$scope.pageSize);
////				console.log(fQuery);
////				$scope.datasource.rawQuery(fQuery).then(function(data){
////					$scope.data = data[DATA_INDEX];
////					$scope.apply_results($scope.data);
////				$scope.loading = false;
////				$scope.duration = (Date.now()-start_time)/1000;
////				});
//			}
//		};
//		
////		$scope.set_points = function (data) {
////		$scope.points = data.points;
////	};
////
////	var is_number = function(str) {
////		return /^[-+]?\d*(\d+\.|\.\d+)?$/.test(str);
////	};
//		
////		var build_typed_keywords = function(keywords) {
////		var FIELD_IDX=0;
////		var VALUE_IDX=1;
////		var k = [];
////		for(var i = 0; i < keywords.length; i++) {
////			var keyword = keywords[i].split(/[:]/);
////
////			for(var j = 0; j < $scope.columns.length; j++) {
////				if(!($scope.columns[j].oper == '=' && !is_number(keywords[i]))) {
////					k.push({field:keyword[FIELD_IDX], oper:$scope.columns[j].oper, value:keyword.slice(VALUE_IDX, keyword.length).join(':')});
////					break;
////				}
////			}
////		}
////		return k;
////	};
////	
////	var build_cond = function(keyword) {
////		return keyword.field+keyword.oper + (keyword.oper == '=' ? keyword.value : '/'+regex_escape(keyword.value)+'/');
////	};
////	
////	var build_query = function(keywords) {
////		var query = "select * from " + $scope.logtype + " where time < "+ $scope.between.to +"s and time > "+ $scope.between.from +"s and ";
////		var columns = $scope.columns;
////		var conds = [];
////		
////		for(var i = 0; i<keywords.length; i++) {
////			var cond = build_cond(keywords[i]);
////
////			if(cond) {
////				conds.push(cond);
////			}
////		}
////		query += conds.join(' and ');
////		
////		return query;
////	};
//		
////		$scope.prevPage = function(keywords) {
////			$scope.loading = true;
////			
////			var query = build_query(build_typed_keywords(keywords));
////			if($scope.currentPage != 1) {
////				$scope.currentPage--;
////				var pQuery = query+" order desc limit " + ($scope.currentPage*$scope.pageSize);
////				$scope.datasource.rawQuery(pQuery).then(function(data){
////					$scope.data = data[DATA_INDEX];
////					$scope.apply_results($scope.data);
////					$scope.loading = false;
////				});
////			}
////			$scope.loading = false;
////		};
////		
////		$scope.nextPage = function(keywords) {
////			$scope.loading = true;
////			
////			var query = build_query(build_typed_keywords(keywords));
////			
////			var nQuery = query+" order desc limit " + (($scope.currentPage+1)*$scope.pageSize);
////			$scope.datasource.rawQuery(nQuery).then(function(data){
////				var pSize = data[DATA_INDEX].points.length;
////				
////				if(pSize >= (($scope.currentPage*$scope.pageSize)+1)) {
////					$scope.currentPage++;
////					$scope.data = data[DATA_INDEX];
////					$scope.apply_results($scope.data);
////					$scope.loading = false;
////				}
////				$scope.loading = false;
////			});
////		};
//		
//		////////////////////////////////////////////////////////////////////////////////////////
//		//  Time Picker
//		////////////////////////////////////////////////////////////////////////////////////////
//	    $scope.setAbsoluteTimeFilter = function (time) {
//	      // Create filter object
//	    	var _filter = _.clone(time);
//	    	if($scope.panel.now) {
//	    		_filter.to = "now";
//	    	}
//
//	      // Set the filter
//	    	$scope.panel.filter_id = $scope.filter.setTime(_filter);
//	    };
//		
//	    // ng-pattern regexs
//	    $scope.patterns = {
//	    	date: /^[0-9]{2}\/[0-9]{2}\/[0-9]{4}$/,
//	    	hour: /^([01]?[0-9]|2[0-3])$/,
//	    	minute: /^[0-5][0-9]$/,
//	    	second: /^[0-5][0-9]$/,
//	    	millisecond: /^[0-9]*$/
//	    };
//		
//	    $scope.customTime = function() {
//	    	$scope.temptime = cloneTime($scope.time);
//
//	    	// Date picker needs the date to be at the start of the day
//	    	$scope.temptime.from.date.setHours(1,0,0,0);
//	    	$scope.temptime.to.date.setHours(1,0,0,0);
//	    };
//		
//	    $scope.setNow = function() {
//	    	$scope.time.to = getTimeObj(new Date());
//	    };
//		
//	    var getTimeObj = function(date) {
//	    	return {
//	    		date: new Date(date),
//	    		hour: pad(date.getHours(),2),
//	    		minute: pad(date.getMinutes(),2),
//	    		second: pad(date.getSeconds(),2),
//	    		millisecond: pad(date.getMilliseconds(),3)
//	    	};
//	    };
//		
//	    var pad = function(n, width, z) {
//	    	z = z || '0';
//	    	n = n.toString();
//	    	return n.length >= width ? n : new Array(width - n.length + 1).join(z) + n;
//	    };
//		
//	    var cloneTime = function(time) {
//	    	var _n = {
//	    		from: _.clone(time.from),
//	    		to: _.clone(time.to)
//	    	};
//	    	// Create new dates as _.clone is shallow.
//	    	_n.from.date = new Date(_n.from.date);
//	    	_n.to.date = new Date(_n.to.date);
//	    	return _n;
//	    };
//
//	    var getScopeTimeObj = function(from,to) {
//	    	return {
//	    		from: getTimeObj(from),
//	    		to: getTimeObj(to)
//	    	};
//	    };
//		
//	    // Constantly validate the input of the fields. This function does not change any date variables
//	    // outside of its own scope
//	    $scope.validate = function(time) {
//			$scope.error = false;
//			var _from = datepickerToLocal(time.from.date),
//			    _to = datepickerToLocal(time.to.date),
//			    _t = time;
//			
//			    _from.setHours(_t.from.hour,_t.from.minute,_t.from.second, _t.from.millisecond);
//			    _to.setHours(_t.to.hour,_t.to.minute,_t.to.second, _t.to.millisecond);
//
//			// Check that the objects are valid and to is after from
//			if(isNaN(_from.getTime()) || isNaN(_to.getTime()) || _from.getTime() >= _to.getTime()) {
//				$scope.error = "invalid time range";
//				return false;
//			}
//			$scope.between.from = _from.getTime()/1000;
//			$scope.between.to = _to.getTime()/1000;
//	      return $scope.between;
//	    };
//		
//	    // Do not use the results of this function unless you plan to use setHour/Minutes/etc on the result
//	    var datepickerToLocal = function(date) {
//	    	date = moment(date).clone().toDate();
//	    	return moment(new Date(date.getTime() + date.getTimezoneOffset() * 60000)).toDate();
//	    };
//	});
//});