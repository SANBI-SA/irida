import angular from 'angular';
import {LinelistModule} from './linelist.module';

const app = angular.module('irida');
app.requires.push(LinelistModule);
