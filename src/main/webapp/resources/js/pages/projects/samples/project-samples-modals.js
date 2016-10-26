(function (ng, $, moment, Clipboard, Project, page, project) {
  "use strict";

  /**
   * Handles all AngularUI modal opening functionality
   * @param $uibModal
   * @returns {{openMergeModal: openMergeModal, openAssociatedProjectsModal: openAssociatedProjectsModal}}
   */
  function modalService($uibModal) {

    function _copyMoveModal(config) {
      return $uibModal.open({
        templateUrl: config.url + "&" + $.param({sampleIds: config.selectedSamples}),
        openedClass: "copy-modal",
        controllerAs: "copyModalCtrl",
        controller  : ["$uibModalInstance", function ($uibModalInstance) {
          var vm = this;

          vm.generateSelect2 = function () {
            return function (data) {
              return data.projects.filter(function (p) {
                return p.identifier != project.id;
              }).map(function (p) {
                return ({
                  id  : p.identifier,
                  text: p.text || p.name
                });
              });
            };
          };

          vm.cancel = function () {
            $uibModalInstance.dismiss();
          };

          vm.doCopy = function () {
            $uibModalInstance.close({
              sampleIds: config.selectedSamples,
              newProjectId: vm.project
            });
          };
        }]
      }).result;
    }

    function openMoveModal(selectedSamples) {
      return _copyMoveModal({
        selectedSamples: selectedSamples,
        url: page.urls.modals.move
      });
    }

    function openCopyModal(selectedSamples) {
      return _copyMoveModal({
        selectedSamples: selectedSamples,
        url: page.urls.modals.copy
      });
    }

    /**
     * Open the modal to remove samples from a project.
     * @param ids
     * @returns {*}
     */
    function openRemoveModal(ids) {
      return $uibModal.open({
        templateUrl : page.urls.modals.remove + "?" + $.param({sampleIds: ids}),
        openedClass : 'remove-modal',
        controllerAs: "removeCtrl",
        controller  : ["$uibModalInstance", function ($uibModalInstance) {
          var vm = this;

          vm.cancel = function () {
            $uibModalInstance.dismiss();
          };

          vm.remove = function () {
            $uibModalInstance.close();
          };
        }]
      }).result;
    }

    /**
     * Open the modal to handle merging samples
     * @param sampleIds - ids for samples to merge
     * @returns {*}
     */
    function openMergeModal(sampleIds) {
      return $uibModal.open({
        templateUrl : page.urls.modals.merge + "?" + $.param({sampleIds: sampleIds}),
        openedClass : 'merge-modal',
        controllerAs: "mergeCtrl",
        controller  : "MergeController",
        resolve     : {
          samples: function () {
            return sampleIds;
          }
        }
      }).result;
    }

    /**
     * Open the modal to display/hide associated projects
     * @param currentlyDisplayed
     * @returns {*}
     */
    function openAssociatedProjectsModal(currentlyDisplayed) {
      return $uibModal.open({
        templateUrl: "associated-projects.modal.html",
        controllerAs: "associatedProjectsCtrl",
        controller: "AssociatedProjectsModalController",
        resolve: {
          display: function () {
            return currentlyDisplayed;
          }
        }
      }).result;
    }

    function openFilterModal() {
      return $uibModal.open({
        templateUrl: "filter.modal.html",
        controllerAs: "filterCtrl",
        openedClass : 'filter-modal',
        controller: "FilterModalController"
      }).result;
    }

    function openLinkerModal(ids) {
      var modal = $uibModal.open({
        templateUrl: page.urls.samples.linker + "?" + $.param({projectId: page.project.id, ids: ids}),
        openedClass: 'linker-modal',
        controllerAs: "lCtrl",
        controller: ["$uibModalInstance", function ($uibModalInstance) {
          var vm = this;
          vm.close = $uibModalInstance.close;
        }]
      });

      // Set up copy to clipboard functionality.
      modal.opened.then(function () {
        new Clipboard(".clipboard-btn");
      });

      return modal.result;
    }

    function openGalaxyModal(ids, projectId) {
      return $uibModal.open({
        templateUrl: page.urls.modals.galaxy,
        controllerAs: "gCtrl",
        controller: "GalaxyDialogCtrl",
        resolve    : {
          sampleIds: function () {
            return ids;
          },
          openedByCart: function () {
            return false;
          },
          multiProject: function () {
            return false;
          },
          projectId: function () {
            return projectId;
          }
        }
      });
    }

    return {
      openFilterModal            : openFilterModal,
      openMoveModal              : openMoveModal,
      openCopyModal              : openCopyModal,
      openRemoveModal            : openRemoveModal,
      openMergeModal             : openMergeModal,
      openAssociatedProjectsModal: openAssociatedProjectsModal,
      openLinkerModal            : openLinkerModal,
      openGalaxyModal            : openGalaxyModal
    };
  }

  /**
   * Controller for handling interaction with the associated projects display modal
   * @param $uibModalInstance - AngularUI modal instance
   * @param associatedProjectsService
   * @param display - Projects to display
   * @constructor
   */
  function AssociatedProjectsModalCtrl($uibModalInstance, associatedProjectsService, display) {
    var vm = this;
    vm.projects = {};
    vm.display = ng.copy(display);
    vm.local = {};

    // Get the local project
    associatedProjectsService.getLocal().then(function (result) {
      // Check to see if they are already displayed.
      vm.projects.local = [];
      result.data.forEach(function(item) {
        var project = new Project(item);
        project.selected = vm.display.local.indexOf(project.getId()) > -1;
        vm.projects.local.push(project);
      });
    });

    vm.rowClick = function($event) {
      $event.stopPropagation();
    };

    vm.close = function () {
      $uibModalInstance.dismiss();
    };

    vm.showProjects = function () {
      // Just want the ids
      vm.display.local = [];
      vm.projects.local.forEach(function (project) {
        if (project.selected) {
          vm.display.local.push(project.getId());
        }
      });

      $uibModalInstance.close(vm.display);
    };
  }

  /**
   * Controller for handling interaction with the merge samples modal.
   * @param $uibModalInstance
   * @param samples
   * @constructor
   */
  function MergeModalController($uibModalInstance, sampleIds) {
    var vm = this;
    vm.selected = sampleIds[0];

    // If user enters a custom name it is not allowed to have spaces
    vm.validNameRE = /^[a-zA-Z0-9-_]+$/;

    /**
     * Closes the modal window without making any changes
     */
    vm.cancel = function () {
      $uibModalInstance.dismiss();
    };


    vm.doMerge = function () {
      $uibModalInstance.close({
        ids          : sampleIds,
        mergeSampleId: vm.selected,
        newName      : vm.name
      });
    };
  }

  /**
   * Container for the current state of the samples filter.
   * @constructor
   */
  function FilterStateService($rootScope) {
    var defaultState = {
      date: {
        startDate: null,
        endDate: null
      }
    },
      currState = {};
    ng.copy(defaultState, currState);

    this.getState = function() {
      var state = {};
      ng.copy(currState, state);
      return state;
    };

    this.setState = function(s) {
      ng.copy(s, currState);
      $rootScope.$broadcast("FILTER_TABLE", {filter: {
        name: s.name,
        minDate: !!s.date.startDate  ? s.date.startDate.valueOf() : "",
        endDate: !!s.date.endDate  ? s.date.endDate.valueOf() : ""
      }});
    };

    $rootScope.$on('CLEAR_FILTERS', function () {
      ng.copy(defaultState, currState);
    });

    $rootScope.$on('CLEAR_FILTER_PROPERTY', function (event, args) {
      if(currState[args.property] !== undefined && currState[args.property].length > 0) {
        delete currState[args.property];
      } else if(currState.date[args.property]){
        currState.date[args.property] = null;
      }
      $rootScope.$broadcast("FILTER_TABLE", {filter: currState});
    });
  }

  /**
   * Controller for handling filtering samples by properties
   * @param $uibModalInstance
   * @param stateService
   * @constructor
   */
  function FilterModalController($uibModalInstance, stateService) {
    var vm = this;
    vm.filter = stateService.getState();

    vm.options = {ranges: {}};
    vm.options.ranges[page.i18n.dateFilter.days30] = [moment().subtract(30, 'days'), moment()];
    vm.options.ranges[page.i18n.dateFilter.days60] = [moment().subtract(60, 'days'), moment()];
    vm.options.ranges[page.i18n.dateFilter.days120] = [moment().subtract(120, 'days'), moment()];

    if(vm.filter.date.startDate !== null) {
      vm.options.startDate = vm.filter.date.startDate;
    }
    if(vm.filter.date.endDate !== null) {
      vm.options.endDate = vm.filter.date.endDate;
    }

    /**
     * Closes the modal window without making any changes
     */
    vm.cancel = function() {
      $uibModalInstance.dismiss();
    };

    /**
     * Return the filter state to the calling function
     */
    vm.doFilter = function() {
      stateService.setState(vm.filter);
      $uibModalInstance.dismiss();
    };
  }

  ng.module("irida.projects.samples.modals", ["irida.projects.samples.service", "irida.directives.select2", "ngMessages", "ui.bootstrap", "daterangepicker"])
    .factory("modalService", ["$uibModal", modalService])
    .service("FilterStateService", ["$rootScope", FilterStateService])
    .controller("AssociatedProjectsModalController", ["$uibModalInstance", "AssociatedProjectsService", "display", AssociatedProjectsModalCtrl])
    .controller("MergeController", ["$uibModalInstance", "samples", MergeModalController])
    .controller("FilterModalController", ["$uibModalInstance", "FilterStateService", FilterModalController])
  ;
}(window.angular, window.jQuery, window.moment, window.Clipboard, window.Project, window.PAGE, window.project));