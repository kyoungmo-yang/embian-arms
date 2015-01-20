define([
  'angular',
  'underscore',
  'config',
  'kbn'
], function (angular, _, config, kbn) {
  'use strict';

  var module = angular.module('kibana.services');

  module.factory('filterSrv', function(dashboard, $rootScope, $timeout, $routeParams, ejsResource) {
    // defaults
    var _d = {
      templateParameters: [],
      time: {}
    };
    // For convenience
    var ejs = ejsResource(config.elasticsearch);
	
    var result = {
	    ids: function() {
	      return dashboard.current.services.filter.ids;
	    },

	   list: function() {
	      return dashboard.current.services.filter.list;
	   },
		
	   getBoolFilter: function(ids) {
	      var bool = ejs.BoolFilter();
	      // there is no way to introspect the BoolFilter and find out if it has a filter. We must keep note.
	      var added_a_filter = false;
		  var _this = this;
	      _.each(ids,function(id) {
	        if(dashboard.current.services.filter.list[id].active) {
	          added_a_filter = true;

	          switch(dashboard.current.services.filter.list[id].mandate)
	          {
	          case 'mustNot':
	            bool.mustNot(_this.getEjsObj(id));
	            break;
	          case 'either':
	            bool.should(_this.getEjsObj(id));
	            break;
	          default:
	            bool.must(_this.getEjsObj(id));
	          }
	        }
	      });
	      // add a match filter so we'd get some data
	      if (!added_a_filter) {
	        bool.must(ejs.MatchAllFilter());
	      }
	      return bool;
	  },
	  
      getEjsObj: function(id) {
        return this.toEjsObj(dashboard.current.services.filter.list[id]);
      },

      toEjsObj: function (filter) {
        if(!filter.active) {
          return false;
        }
        switch(filter.type)
        {
        case 'time':
          var _f = ejs.RangeFilter(filter.field).from(kbn.parseDate(filter.from).valueOf());
          if(!_.isUndefined(filter.to)) {
            _f = _f.to(kbn.parseDate(filter.to).valueOf());
          }
          return _f;
        case 'range':
          return ejs.RangeFilter(filter.field)
            .from(filter.from)
            .to(filter.to);
        case 'querystring':
          return ejs.QueryFilter(ejs.QueryStringQuery(filter.query)).cache(true);
        case 'field':
          return ejs.QueryFilter(ejs.QueryStringQuery(filter.field+":("+filter.query+")")).cache(true);
        case 'terms':
          return ejs.TermsFilter(filter.field,filter.value);
        case 'exists':
          return ejs.ExistsFilter(filter.field);
        case 'missing':
          return ejs.MissingFilter(filter.field);
        default:
          return false;
        }
      },
	  

      updateTemplateData: function(initial) {
        var _templateData = {};
        _.each(this.templateParameters, function(templateParameter) {
          if (initial) {
            var urlValue = $routeParams[ templateParameter.name ];
            if (urlValue) {
              templateParameter.current = { text: urlValue, value: urlValue };
            }
          }
          if (!templateParameter.current || !templateParameter.current.value) {
            return;
          }
          _templateData[templateParameter.name] = templateParameter.current.value;
        });
        this._templateData = _templateData;
      },

      addTemplateParameter: function(templateParameter) {
        this.templateParameters.push(templateParameter);
        this.updateTemplateData();
      },

      applyTemplateToTarget: function(target) {
        if (target.indexOf('[[') === -1) {
          return target;
        }

        return _.template(target, this._templateData, this.templateSettings);
      },

      setTime: function(time, refresh, doSave) {
		refresh = typeof refresh !== 'undefined' ? refresh : true;
		doSave = typeof doSave !== 'undefined' ? doSave : true;
		
        _.extend(this.time, time);

        // disable refresh if we have an absolute time
        if (time.to !== 'now' || !refresh) {
			if(this.old_refresh == null) {
				this.old_refresh = this.dashboard.refresh;
			}
			dashboard.set_interval(false);
        }
        else if (this.old_refresh && this.old_refresh !== this.dashboard.refresh) {
          dashboard.set_interval(this.old_refresh);
          this.old_refresh = null;
        }

        $timeout(function() {
          dashboard.refresh();
        },0);
		
		if(doSave) {
			this._saveTimeFilter();
		}
      },

      timeRange: function(parse) {
        var _t = this.time;
        if(_.isUndefined(_t) || _.isUndefined(_t.from)) {
          return false;
        }
        if(parse === false) {
          return {
            from: _t.from,
            to: _t.to
          };
        } else {
          var _from = _t.from;
          var _to = _t.to || new Date();

          return {
            from : kbn.parseDate(_from),
            to : kbn.parseDate(_to)
          };
        }
      },

      removeTemplateParameter: function(templateParameter) {
        this.templateParameters = _.without(this.templateParameters, templateParameter);
        this.dashboard.services.filter.list = this.templateParameters;
      },

	  getCookie: function(cname) {
	      var name = cname + "=";
	      var ca = document.cookie.split(';');
	      for(var i=0; i<ca.length; i++) {
	          var c = ca[i];
	          while (c.charAt(0)==' ') c = c.substring(1);
	          if (c.indexOf(name) != -1) return c.substring(name.length,c.length);
	      }
	      return false;
	  },
	  
	  setCookie: function(cname, cvalue, exdays) {
	      var d = new Date();
	      d.setTime(d.getTime() + (exdays*24*60*60*1000));
	      var expires = "expires="+d.toUTCString();
	      document.cookie = cname + "=" + cvalue + "; " + expires;
	  },
	  
	  _loadTimeFilter: function() {
		var filter = this.getCookie('__srm_filter');
		if(!filter) {
			return false;
		}
		
		return angular.fromJson(filter);
	  },
	  
	  _saveTimeFilter: function() {
		  this.setCookie('__srm_filter', angular.toJson(this.time), 365);
	  },
	  
      init: function(dashboard) {
        _.defaults(this, _d);
        this.dashboard = dashboard;
        this.templateSettings = { interpolate : /\[\[([\s\S]+?)\]\]/g };

        if(dashboard.services && dashboard.services.filter) {
		  var timefilter = this._loadTimeFilter();
		  if(timefilter) {
			  dashboard.services.filter.time = timefilter;
		  }
		  
          this.time = dashboard.services.filter.time;
          this.templateParameters = dashboard.services.filter.list || [];
          this.updateTemplateData(true);
        }

      }
    };
    return result;
  });

});
