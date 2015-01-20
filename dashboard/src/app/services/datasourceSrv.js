define([
  'angular',
  'underscore',
  'config',
  './influxdb/influxdbDatasource'
],
function (angular, _, config) {
  'use strict';

  var module = angular.module('kibana.services');

  module.service('datasourceSrv', function($q, $http, $injector) {
    var datasources = {};
    var metricSources = [];
    var annotationSources = [];
    var grafanaDB = {};

    this.init = function() {
      _.each(config.datasources, function(value, key) {
        datasources[key] = this.datasourceFactory(value);
        if (value.default) {
          this.default = datasources[key];
        }
      }, this);

      if (!this.default) {
        this.default = datasources[_.keys(datasources)[0]];
        this.default.default = true;
      }
    };

    this.datasourceFactory = function(ds) {
      var Datasource = null;
      switch(ds.type) {
      case 'influxdb':
        Datasource = $injector.get('InfluxDatasource');
        break;
      }
      return new Datasource(ds);
    };

    this.get = function(name) {
      if (!name) { return this.default; }
      if (datasources[name]) { return datasources[name]; }

      throw "Unable to find datasource: " + name;
    };

    this.init();
  });
});
