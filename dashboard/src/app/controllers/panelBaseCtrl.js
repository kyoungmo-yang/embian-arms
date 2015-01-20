define([
  'angular',
  'underscore',
  'jquery'
],
function (angular, _, $) {
  'use strict';

  // This function needs $inject annotations, update below
  // when changing arguments to this function
  function PanelBaseCtrl($scope, $rootScope, $timeout) {

    $scope.inspector = {};

    $scope.updateColumnSpan = function(span) {
      $scope.panel.span = span;

      $timeout(function() {
        $scope.$emit('render');
      });
    };

    $scope.enterFullscreenMode = function(options) {
      var docHeight = $(window).height();
      var editHeight = Math.floor(docHeight * 0.3);
      var fullscreenHeight = Math.floor(docHeight * 0.7);
      var oldTimeRange = $scope.range;

      $scope.height = options.edit ? editHeight : fullscreenHeight;
      $scope.editMode = options.edit;

      if (!$scope.fullscreen) {
        var closeEditMode = $rootScope.$on('panel-fullscreen-exit', function() {
          $scope.editMode = false;
          $scope.fullscreen = false;
          delete $scope.height;

          closeEditMode();

          $timeout(function() {
            if (oldTimeRange !== $scope.range) {
              $scope.dashboard.refresh();
            }
            else {
              $scope.$emit('render');
            }
          });
        });
      }

      $(window).scrollTop(0);

      $scope.fullscreen = true;

      $rootScope.$emit('panel-fullscreen-enter');

      $timeout(function() {
        $scope.$emit('render');
      });

    };

    $scope.toggleFullscreenEdit = function() {
      if ($scope.editMode) {
        $rootScope.$emit('panel-fullscreen-exit');
        return;
      }

      $scope.enterFullscreenMode({edit: true});
    };

    $scope.toggleFullscreen = function() {
      if ($scope.fullscreen && !$scope.editMode) {
        $rootScope.$emit('panel-fullscreen-exit');
        return;
      }

      $scope.enterFullscreenMode({ edit: false });
    };

  }

  PanelBaseCtrl['$inject'] = ['$scope', '$rootScope', '$timeout'];

  return PanelBaseCtrl;

});