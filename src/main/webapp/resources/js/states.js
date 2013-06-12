/**
 * User:    Josh Adam <josh.adam@phac-aspc.gc.ca>
 * Date:    2013-06-04
 * Time:    9:11 AM
 * License: MIT
 */
(function (ng, app) {
  'use strict';
  app.config(function ($stateProvider) {
    $stateProvider
    /**
     * Landing Page
     */
      .state('projects', {
        url: '/',
        templateUrl: '/partials/projects.html',
        resolve: {
          data: function ($q, ajaxService) {
            var defer = $q.defer();
            ajaxService.get('/api/projects').then(function (data) {
              defer.resolve(data);
            });
            return defer.promise;
          }
        },
        controller: function ($scope, data) {
          $scope.projects = data.resource.resources;
        }
      })
    /**
     * Project Page
     */
      .state('projects.detail', {
        url: 'projects/:projectId',
        templateUrl: '/partials/projects.detail.html',
        resolve: {
          data: function ($q, $stateParams, ajaxService) {
            var defer = $q.defer();
            ajaxService.get('/api/projects/' + $stateParams.projectId).then(function (data) {
              defer.resolve(data);
            });
            return defer.promise;
          }
        },
        controller: function ($scope, $stateParams, linkFormatter, data) {
          console.log("PROJECT");
          console.log(data);
          $scope.project = {
            id: $stateParams.projectId,
            name: data.resource.name,
            users: data.relatedResources.users.resources,
            samples: data.relatedResources.samples.resources,
            links: linkFormatter.clean(data.resource.links)
          };
        }
      })
      .state('projects.detail.samples', {
        url: '/samples',
        templateUrl: '/partials/projects.detail.samples.html',
        controller: function () {
          console.log("SAMPLE");
        }
      })
//      .state('projects.details.')
      .state('projects.detail.users', {
        url: '/users',
        templateUrl: '/partials/projects.detail.users.html',
        controller: function () {
          console.log("USERS");
        }
      })
      .state('login', {
        url: '/login',

        templateUrl: '/partials/login.html',
        controller: function () {
          console.log("LOGIN");
        }
      });
  });
})
  (angular, NGS);