/* eslint new-cap: [2, {"capIsNewExceptions": ["DataTable"]}] */
/**
 * @file AngularJS Component for display rows from the metadata file that match
 * Sample names on the server.
 */
import $ from 'jquery';
import 'DataTables/datatables.js';
import {dom, formatBasicHeaders} from '../../../../utilities/datatables.utilities';

const resultsFoundComponent = {
  templateUrl: 'results.found.component.tmpl.html',
  bindings: {
    rows: '='
  },
  controller() {
    this.$onInit = () => {
      if (this.rows.length > 0) {
        const headers = Object.keys(this.rows[0]);
        const columns = formatBasicHeaders(headers);
        $('#found-table').DataTable({
          dom,
          scrollX: true,
          sScrollX: '100%',
          data: this.rows,
          columns
        });
      }
    };
  }
};

export default resultsFoundComponent;
