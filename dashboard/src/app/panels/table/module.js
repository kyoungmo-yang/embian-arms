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
  'jquery',
  'kbn',
  'services/datasourceSrv',
  'underscore',
  'require'
],
function (angular, app, _, kbn, require) {
  'use strict';

  /*************************************************************************************************************
  *       Popup TABLE
  *************************************************************************************************************/

  var module = angular.module('kibana.panels.ptable', []);
  app.useModule(module);
  module.controller('ptable', function($scope, $rootScope, datasourceSrv, $routeParams, $location, dashboard) {
    var pcolumns = {};
	var expr = /([a-z]+)\({([a-z]\w+)},{([a-z]\w+)}\)/;
	var _p = '?';
	
  	$scope.panelMeta = {
  		interval: null,
  		loading: false
  	};

	var push_history = function(history) {
  		$scope.histories.push(history);
  	};
  	
  	var pop_history = function() {
  		return $scope.histories.pop();
  	}
  	
  	 var get_history_type = function() {
     	if($routeParams.service && $routeParams.service != 'undefined' && typeof $routeParams.service != 'undefined') {
     		if($routeParams.hostname && $routeParams.hostname != 'undefined' && typeof $routeParams.hostname != 'undefined') {
     			return "2";
     		}
     		
     		return "1";
     	}
     		
     	return "0";
     };
  	
  	var init_histories = function() {
  		$scope.histories = $routeParams.history ? angular.fromJson($routeParams.history) : [];
  		push_history({dn:dashboard.current.dashname, n:$scope.panel.title, t:'0'});
  		
		if($scope.histories.length > 0) {
			var h = $scope.histories[$scope.histories.length-1];
			h.n = $scope.panel.title;
			if($routeParams.hostname && $routeParams.hostname != 'undefined' && typeof $routeParams.hostname != 'undefined') {
				h.n = $routeParams.hostname+' '+$scope.panel.title;
			}
			h.t = get_history_type();
			_p = '?history='+ angular.toJson($scope.histories) + '&';
		}
  	};
  	
	$scope.init = function(panel, offset, limit) {
		init_histories();
		
		$scope.initBaseController(this, $scope);
		$scope.datasource = datasourceSrv.get('influxdb');
		$scope.panel = angular.copy(panel);
		$scope.panel.source.limit = limit;
		$scope.panel.source.offset = offset;
		$scope.current = 1;
		$scope.points = [];
		$scope.reset_rows();
		$scope.get_data($scope.panel.query);
		$scope.$on('__show_table_detail', function(e){ 
			$scope.reload();
		});
	};
	
	$scope.reset_rows = function() {
		$scope.rows = [];
	};
	
	$scope.text_align = function(column) {
		return (column.width == '100%' ? 'left;' : 'center;')
	};
	
	$scope.get_data = function() {
		$scope.panelMeta.loading = true;
		
		var options = {targets:[], useOwnHandler:true, range:{}};
		options.range = this.filter.timeRange(false);
		var target = angular.copy($scope.panel.source);
		if (target.limitbyquery) {
			target.query += ' limit ' + $scope.panel.detail.limit;
		}
		options.targets.push(target);
		$scope.datasource.query($scope.filter, options)
		                 .then($scope.dataHandler)
						 .then(null, function(err) {
							$scope.panelMeta.loading = false;
						 });
	};
	
	$scope.link_params = function(row, index) {
		var params = $scope.panel.columns[index].params;
		var p = _p;
		for(var i = 0; i < params.length; i++) {
			if (pcolumns[params[i]] >= 0) {
				p += params[i] + "=" + row[pcolumns[params[i]]] + '&';
			} else {
				p += params[i] + "=" + $routeParams[params[i]] + '&';
			}
		}
		
		return p;
	};

	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};

	$scope.reload = function() {
		$scope.panel.source.offset = 0;
		$scope.current = 1;
		$scope.reset_rows();
		$scope.get_data($scope.panel.query);
	};
	
	$scope.limit = function(array, offset, limit) {
		return kbn.limit(array, offset, limit);
	};
	
	$scope.total = function() {
		return Math.ceil($scope.points.length / ($scope.panel.source.limit * 1.0));
	};
	
	$scope.next = function() {
		if($scope.current >= $scope.total()) return;
		$scope.current++;
		$scope.panel.source.offset = $scope.panel.source.offset+$scope.panel.source.limit;
		$scope.pagenation($scope.panel.source.offset, $scope.panel.source.limit);
	};
	
	$scope.prev = function() {
		if($scope.current <= 1) return;
		$scope.current--;
		$scope.panel.source.offset = $scope.panel.source.offset-$scope.panel.source.limit;
		$scope.pagenation($scope.panel.source.offset, $scope.panel.source.limit);
	};
	
	$scope.pagenation = function(offset, limit) {
		var rows = [];
		var points = [];
		if (limit > 0) {
			points = $scope.limit($scope.points, offset, limit);
		} else {
			points = $scope.points;
		}
		
		for(var i = 0; i < points.length; i++) {
			var row = [];
			
			for(var j in $scope.panel.data_idx) {
				var type = $scope.panel.columns[j].type;
				var idx = $scope.panel.data_idx[j];
				var val = "";
				if(idx < 0) {
					if(type == "index") {
						val = i+1+offset;
					} else {
						val = "-";
					}
				} else {
					val = points[i][idx];
					if(type == "second") {
						val = kbn.sFormat(val, 2);
					} else if (type == "number") {
						val = val.toLocaleString();
					}
				}
				
				row.push(val);
			}
			
			if($scope.panel.source.colormap) {
				row.color = '';
				var now = Date.now() / 1000;
				var time = points[i][0];
				var diff = now - time;
				if(diff <= $scope.panel.source.colormap.red) {
					row.color = 'red';
				} else if (diff <= $scope.panel.source.colormap.yellow) {
					row.color = 'yellow';
				}
				
			}
			
			rows.push(row)
		}
		
		$scope.reset_rows();
		$scope.rows = rows;
		$scope.panelMeta.loading = false;
	}
	
	var apply_udf = function(columns, points, udf) {
		if(expr.test(udf)) {
			var m = udf.match(expr);
			var op = m[1];
			var or1 = {c:m[2], i:-1};
			var or2 = {c:m[3], i:-1};
			
			for(var i = 0; i < columns.length; i++) {
				if(or1.i < 0 && columns[i] == or1.c) {
					or1.i = i;
				} else if (or2.i < 0  && columns[i] == or2.c) {
					or2.i = i;
				}
				
				if(or1.i >= 0 && or2.i >= 0) {
					break;
				}
			}
			
			if(or1.i >= 0 && or2.i >= 0) {
				columns.push(op);
				
				for(var i = 0; i < points.length; i++) {
					if(op == 'division') {
						points[i].push((points[i][or1.i] / points[i][or2.i]))
					}
					// TODO: more udf imple
				}
			}
		}
		return {columns:columns, points:points};
	}
	
    $scope.dataHandler = function(results) {
		if(!results || !results.data) {
			return ;
		}
		
		results = results.data;
		if(!results || !results[0] || typeof results[0].columns === 'undefined') {
			$scope.reset_rows();
			$scope.panelMeta.loading = false;
			return ;
		}
		
		var columns = [];
		$scope.points = [];
		
		if($scope.panel.source.udf) {
			var r = apply_udf(results[0].columns, results[0].points, $scope.panel.source.udf);
			columns = r.columns;
			$scope.points = r.points;
		} else {
			columns = results[0].columns;
			$scope.points = results[0].points;
		}
		
		var rows = [];
		
		$scope.panel.sort_prop = $scope.panel.source.sortby;
		
		if(!$scope.panel.data_idx || !$scope.panel.data_idx.length != $scope.panel.columns.length) {
			$scope.panel.data_idx = [];
		
			for(var j in $scope.panel.columns) {
				for(var i in columns) {
					if(columns[i] == $scope.panel.sort_prop) {
						$scope.panel.sort_prop = i;
					}
					
					if(columns[i] == $scope.panel.columns[j].field) {
						$scope.panel.data_idx.push(i);
						pcolumns[$scope.panel.columns[j].field] = j;
						break;
					}
				}
				
				if($scope.panel.data_idx.length <= j) {
					$scope.panel.data_idx.push(-1);
				}
			}
		}
		
		if($scope.panel.sort_prop >= 0 && $scope.panel.source.sortby != null) {
			$scope.sort_by($scope.points, $scope.panel.sort_prop, 'asc');
		}
		
		$scope.pagenation($scope.panel.source.offset, $scope.panel.source.limit);
    };
  });

  /*************************************************************************************************************
  *       TABLE
  *************************************************************************************************************/

  var module = angular.module('kibana.panels.table', []);
  app.useModule(module);

  module.controller('table', function($scope, $rootScope, datasourceSrv, $routeParams, $location, dashboard, filterSrv) {
	var pcolumns = {};
	var expr = /([a-z]+)\({([a-z]\w+)},{([a-z]\w+)}\)/;
	var _p = '?';
	
	$scope.panelMeta = {
		interval: null,
		loading: false
	}
	
  	var push_history = function(history) {
  		$scope.histories.push(history);
  	};
  	
  	var pop_history = function() {
  		return $scope.histories.pop();
  	}
  	
  	var get_history_type = function() {
    	if($routeParams.service && $routeParams.service != 'undefined' && typeof $routeParams.service != 'undefined') {
    		if($routeParams.hostname && $routeParams.hostname != 'undefined' && typeof $routeParams.hostname != 'undefined') {
    			return "2";
    		}
    		
    		return "1";
    	}
    		
    	return "0";
    };
  	
  	var init_histories = function() {
  		$scope.histories = $routeParams.history ? angular.fromJson($routeParams.history) : [];
  		push_history({dn:dashboard.current.dashname, n:$routeParams.service, t:'0'});
  		
		if($scope.histories.length > 0) {
			var h = $scope.histories[$scope.histories.length-1];
			h.n = $scope.panel.title;
			if($routeParams.hostname && $routeParams.hostname != 'undefined' && typeof $routeParams.hostname != 'undefined') {
				h.n = $routeParams.hostname+' '+$scope.panel.title;
			}
			h.t = get_history_type();
			_p = '?history='+ angular.toJson($scope.histories) + '&';
		}
  	};
  	
	$scope.init = function() {
		init_histories();
		
		$scope.initBaseController(this, $scope);
		$scope.datasource = datasourceSrv.get('influxdb');
		$scope.reset_rows();
		$scope.get_data();
		
        $scope.$on('refresh',function() {
          if ($scope.panelMeta.loading) { return; }
          $scope.get_data();
        });
    };
    
	$scope.link_params = function(row, index) {
		var params = $scope.panel.columns[index].params;
		var p=_p;
		
		for(var i = 0; i < params.length; i++) {
			if (pcolumns[params[i]] >= 0) {
				p += params[i] + "=" + row[pcolumns[params[i]]] + '&';
			} else {
				p += params[i] + "=" + $routeParams[params[i]] + '&';
			}
		}
		
		return p;
	};
	
	$scope.showdetail = function() {
		$scope.$broadcast('__show_table_detail');
	};
		
	$scope.reset_rows = function() {
		$scope.rows = [];
	};
	
	$scope.text_align = function(column) {
		return (column.width == '100%' ? 'left;' : 'center;')
	};
	
	$scope.row_color = function(index) {
		if($scope.points && $scope.points.length > index) {
			$scope.points[index]
		}
		return '';
	};
	
	$scope.get_data = function() {
		$scope.panelMeta.loading = true;
		
		var options = {targets:[], useOwnHandler:true, range:{}};
		options.range = this.filter.timeRange(false);
		var target = angular.copy($scope.panel.source);
		if (target.limitbyquery) {
			target.query += ' limit ' + $scope.panel.source.limit;
		}
		options.targets.push(target);
		$scope.datasource.query($scope.filter, options)
		                 .then($scope.dataHandler)
						 .then(null, function(err) {
							$scope.panelMeta.loading = false;
						 });
	};
	
	$scope.sort_by = function(array, idx, direction) {
		array.sort(kbn.sort_by(idx, direction));
	};
	
	$scope.limit = function(array, offset, limit) {
		return kbn.limit(array, offset, limit);
	}
	
	var apply_udf = function(columns, points, udf) {
		if(expr.test(udf)) {
			var m = udf.match(expr);
			var op = m[1];
			var or1 = {c:m[2], i:-1};
			var or2 = {c:m[3], i:-1};
			
			for(var i = 0; i < columns.length; i++) {
				if(or1.i < 0 && columns[i] == or1.c) {
					or1.i = i;
				} else if (or2.i < 0  && columns[i] == or2.c) {
					or2.i = i;
				}
				
				if(or1.i >= 0 && or2.i >= 0) {
					break;
				}
			}
			
			if(or1.i >= 0 && or2.i >= 0) {
				columns.push(op);
				
				for(var i = 0; i < points.length; i++) {
					if(op == 'division') {
						points[i].push((points[i][or1.i] / points[i][or2.i]))
					}
					// TODO: more udf imple
				}
			}
		}
		return {columns:columns, points:points};
	}
	
    $scope.dataHandler = function(results) {
		if(!results || !results.data) {
			return ;
		}
		
		results = results.data;
		if(!results || !results[0] || typeof results[0].columns === 'undefined') {
			$scope.reset_rows();
			$scope.panelMeta.loading = false;
			return ;
		}
		
		var columns = [];
		$scope.points = [];
		
		if($scope.panel.source.udf) {
			var r = apply_udf(results[0].columns, results[0].points, $scope.panel.source.udf);
			columns = r.columns;
			$scope.points = r.points;
		} else {
			columns = results[0].columns;
			$scope.points = results[0].points;
		}
		
		var rows = [];
		
		$scope.panel.sort_prop = $scope.panel.source.sortby;
		
		if(!$scope.panel.data_idx || !$scope.panel.data_idx.length != $scope.panel.columns.length) {
			$scope.panel.data_idx = [];
		
			for(var j in $scope.panel.columns) {
				for(var i in columns) {
					if(columns[i] == $scope.panel.sort_prop) {
						$scope.panel.sort_prop = i;
					}
					
					if(columns[i] == $scope.panel.columns[j].field) {
						$scope.panel.data_idx.push(i);
						pcolumns[$scope.panel.columns[j].field] = j;
						break;
					}
				}
				
				if($scope.panel.data_idx.length <= j) {
					$scope.panel.data_idx.push(-1);
				}
			}
		}
		
		if($scope.panel.sort_prop >= 0) {
			$scope.sort_by(results[0].points, $scope.panel.sort_prop, 'asc');
		}
		
		if ($scope.panel.source.limit > 0) {
			results[0].points = $scope.limit(results[0].points, 0, $scope.panel.source.limit);
		}
		
		for(var i = 0; i < results[0].points.length; i++) {
			var row = [];
			
			for(var j in $scope.panel.data_idx) {
				var type = $scope.panel.columns[j].type;
				var idx = $scope.panel.data_idx[j];
				var val = "";
				if(idx < 0) {
					if(type == "index") {
						val = i+1;
					} else {
						val = "-";
					}
				} else {
					val = results[0].points[i][idx];
					if(type == "second") {
						val = kbn.sFormat(val, 2);
					} else if (type == "number") {
						val = val.toLocaleString();
					}
				}
				
				row.push(val);
			}
			
			if($scope.panel.source.colormap) {
				row.color = '';
				var now = Date.now() / 1000;
				var time = results[0].points[i][0];
				var diff = now - time;
				if(diff <= $scope.panel.source.colormap.red) {
					row.color = 'red';
				} else if (diff <= $scope.panel.source.colormap.yellow) {
					row.color = 'yellow';
				}
				
			}

			rows.push(row)
		}
		
		$scope.reset_rows();
		$scope.rows = rows;
		$scope.panelMeta.loading = false;
    };
	
	$scope.add_row = function(row) {
		if (row && row.length == $scope.panel.columns.length) {
			$scope.rows.push(row);
		}
	};
  });
});