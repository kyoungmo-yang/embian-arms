define([
  'angular',
  'underscore',
  'config',
  'jquery',
  'require'
],
function (angular, _, config, $) {
	var module = angular.module('kibana.controllers');
	
//	/*************************************************************************************************************
//	*       Angularjs websocket example
//	*************************************************************************************************************/
//
//	module.factory('settings', ['$q', '$rootScope', function($q, $rootScope) {
//		var callbacks = {};
//	    var currentCallbackId = 0;
//		var ws = new WebSocket('ws://'+config.cep_engine_url+'/login');
//
//		ws.onopen = function(){  
//		    console.log("Socket has been opened!");  
//		};
//		    
//		ws.onmessage = function(message) {
//		    listener(JSON.parse(message.data));
//		};
//	    
//	    function sendRequest(request) {
//	        var defer = $q.defer();
//	        var callbackId = getCallbackId();
//	        callbacks[callbackId] = {
//	          time: new Date(),
//	          cb:defer
//	        };
//	        request.callback_id = callbackId;
//	        console.log('Sending request', request);
//	        ws.send(JSON.stringify(request));
//	        return defer.promise;
//	    }
//	    
//	    function listener(data) {
//	        var messageObj = data;
//	        console.log("Received data from websocket: ", messageObj);
//	        // If an object exists with callback_id in our callbacks object, resolve it
//	        if(callbacks.hasOwnProperty(messageObj.callback_id)) {
//	          console.log(callbacks[messageObj.callback_id]);
//	          $rootScope.$apply(callbacks[messageObj.callback_id].cb.resolve(messageObj.data));
//	          delete callbacks[messageObj.callbackID];
//	        }
//	    }
//	    
//	    function getCallbackId() {
//	        currentCallbackId += 1;
//	        if(currentCallbackId > 10000) {
//	          currentCallbackId = 0;
//	        }
//	        return currentCallbackId;
//	    }
//	}])
	
	module.controller('settingsCtrl', function($scope, $http) {
		
		/*************************************************************************************************************
		*       Angularjs websocket example
		*************************************************************************************************************/
		var callbacks = {};
	    var currentCallbackId = 0;
		var ws = new WebSocket('ws://'+config.cep_engine_url+'/login');

		ws.onopen = function(){  
		    console.log("Socket has been opened!");  
		};
		    
		ws.onmessage = function(message) {
		    listener(JSON.parse(message.data));
		};
	    
	    function sendRequest(request) {
	        var defer = $q.defer();
	        var callbackId = getCallbackId();
	        callbacks[callbackId] = {
	          time: new Date(),
	          cb:defer
	        };
	        request.callback_id = callbackId;
	        console.log('Sending request', request);
	        ws.send(JSON.stringify(request));
	        return defer.promise;
	    }
	    
	    function listener(data) {
	        var messageObj = data;
	        console.log("Received data from websocket: ", messageObj);
	        // If an object exists with callback_id in our callbacks object, resolve it
	        if(callbacks.hasOwnProperty(messageObj.callback_id)) {
	          console.log(callbacks[messageObj.callback_id]);
	          $rootScope.$apply(callbacks[messageObj.callback_id].cb.resolve(messageObj.data));
	          delete callbacks[messageObj.callbackID];
	        }
	    }
	    
	    function getCallbackId() {
	        currentCallbackId += 1;
	        if(currentCallbackId > 10000) {
	          currentCallbackId = 0;
	        }
	        return currentCallbackId;
	    }
		
		/*************************************************************************************************************
		*       Login
		*************************************************************************************************************/
		$scope.loginChecked = false;

		$scope.login = function (username, password) {
			$http({
				method: 'POST',
				url: config.cep_engine_url+'/login',
				withCredentials: true,
				headers: {'Content-Type': 'application/x-www-form-urlencoded'},
				data: $.param({username: username, password: password}),
					}).success(function(response, status, headers) {
						if(response.success) {
							$scope.loginChecked = true;
							$scope.reload();
						} else {
							$scope.loginChecked = false;
							$scope.error = response.message;
						}
					}).error(function (response) {
						$scope.error = response.message;
					});
		};
		
		
		/*************************************************************************************************************
		*       Settings
		*************************************************************************************************************/
		$scope.loading = false;
		$scope.error = false;

		$scope.init = function() {
			$scope.panels = [{title:'주의', level:1, rows:[]},
			                 {title:'경고', level:2, rows:[]},
			                 {title:'위험', level:3, rows:[]}];
			
			$scope.$on('__alert_settings', function(e){
				$scope.reload();
			});
		};
		
		$scope.get_data = function (panels) {
			$scope.loading = true;
			$http({
				method: 'POST',
				withCredentials: true,
				url: config.cep_engine_url+'/list_alert_stmts'
					}).success(function (response) {
						if(response.success) {
							if(response.data.l1 != '' || response.data.l2 != '' || response.data.l3 != '') {
								$scope.loading = false;
								panels[0].rows = response.data.l1;
								panels[1].rows = response.data.l2;
								panels[2].rows = response.data.l3;
							} else {
								$scope.loading = false;
							}
						} else {
							if(response.message == 'Required login') {
								$scope.loginChecked = false;
							} else {
								$scope.error = response.message;
							}
						}
					}).error(function (response) {
						$scope.error = response.message;
					});
		};
		
		$scope.reload = function () {
			$scope.error = false;
			$scope.loading = false;
			$scope.panels[0].rows = [];
			$scope.panels[1].rows = [];
			$scope.panels[2].rows = [];
			
			
			$scope.get_data($scope.panels);
		}
		
		$scope.duplication_check = function () {
			for(var i=0; i<$scope.panels.length; i++) {
				for(var j=0; j<$scope.panels[i].rows.length; j++) {
					if($scope.panels[i].rows[j].edit == true) {
						return {panel:$scope.panels[i], row:$scope.panels[i].rows[j]};
					}
				}
			}
			return false;
		}
		
		$scope.toggle = function (row) {
			var edited_contents = $scope.duplication_check();
			
			if(edited_contents != false) {
				if(confirm('수정중인 알림을 취소하고 계속 진행하시겠습니까?')) {
					$scope.cancel(edited_contents.panel, edited_contents.row);
				} else {
					return ;
				}
			}
			
			row.edit = !row.edit;
			
			//TODO : makes pre_data to cancel the editing
			row.pre_data = angular.copy(row);
		};
		
		$scope.put_data = function(row, level) {
			var p_data = {
    				'name': row.name, 
    				'epl': row.epl, 
    				'routingKey': 'alert.', 
    				'toMail': row.mail, 
    				'desc': row.desc,
    				'level': 'l'+ level};
    		
    		$http({
				method: 'POST',
				url: config.cep_engine_url+'/put_stmt',
				withCredentials: true,
				headers: {'Content-Type': 'application/x-www-form-urlencoded'},
				data: $.param(p_data),
					}).success(function(response) {
						if(response.success) {
							$scope.error = false;
							row.edit = false;
						} else {
							if(response.message == 'Required login') {
								$scope.loginChecked = false;
							} else {
								$scope.error = response.message;
							}
						}
					}).error(function (response) {
						$scope.error = response.message;
					});
		};
		
		$scope.remove_data = function (panel, row) {
			var r_data = {
    				'stmtId': row.stmtId, 
    				'routingKey': 'alert.'};
    		
    		$http({
				method: 'POST',
				url: config.cep_engine_url+'/remove_stmt',
				withCredentials: true,
				headers: {'Content-Type': 'application/x-www-form-urlencoded'},
				data: $.param(r_data),
					}).success(function(response) {
						if(response.success) {
							if(panel!='') {		// used in remove
								panel.rows = _.without(panel.rows, row);
							} 
						} else {
							if(response.message == 'Required login') {
								$scope.loginChecked = false;
							} else {
								$scope.error = response.message;
							}
						}
					}).error(function (response) {
						$scope.error = response.message;
					});
		}
		
		$scope.add_setting = function (panel) {
			var edited_contents = $scope.duplication_check();
			
			if(edited_contents == false) {
				panel.rows.push({stmtId:false, name:'', epl:'', mail:'', desc:'', edit:true});
			}
			else {
				if(confirm('수정중인 알림을 취소하고 계속 진행하시겠습니까?')) {
					$scope.cancel(edited_contents.panel, edited_contents.row);
					panel.rows.push({stmtId:false, name:'', epl:'', mail:'', desc:'', edit:true});
				} else {
					return ;
				}
			}
	    };
		
		$scope.remove = function (panel, row) {
			$scope.error = false;
			if(row.stmtId==false) {
				panel.rows = _.without(panel.rows, row);
			}
			else {
				$scope.remove_data(panel, row);
			}
	    };
		
	    var contents_check = function(row) {
	    	if(_.isUndefined(row.name) || row.name=="" || row.name==null) {
	    		$scope.error = '"Name"은 필수 항목입니다.';
	    		return false;
	    	}
	    	if(_.isUndefined(row.epl) || row.epl=="" || row.epl==null) {
	    		$scope.error = '"EPL"은 필수 항목입니다.';
	    		return false;
	    	}
	    	if(_.isUndefined(row.mail) || row.mail=="" || row.mail==null) {
	    		$scope.error = '"Mail"은 필수 항목입니다.';
	    		return false;
	    	}
	    	return true;
	    };
	    
	    $scope.apply = function (panel, row) {
	    	if(row.stmtId==false) {
	    		if(contents_check(row)) {
	    			$scope.put_data(row, panel.level);
	    		}	    		
	    	}
	    	else {
	    		if(contents_check(row)) {
			    	$scope.remove_data('', row);
			    	$scope.put_data(row, panel.level);
	    		}
	    	}
	    };
	    
	    $scope.cancel = function (panel, row) {
	    	row.edit = false;
	    	$scope.error = false;
	    	if(row.stmtId==false) {
	    		panel.rows = _.without(panel.rows, row);
	    	}
	    	else {   		
	    		row.name = row.pre_data.name;
	    		row.epl = row.pre_data.epl;
	    		row.mail = row.pre_data.mail;
	    		row.desc = row.pre_data.desc;
	    	}
	    };
	});
});