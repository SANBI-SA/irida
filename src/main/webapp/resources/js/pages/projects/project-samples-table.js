// The contents of this file gets load via Dandelion Datatables to the end of IIFE.

// Add the toolbar to the table since this does not exist until datatables loads the table.

var toolbarDiv = document.querySelector(".filter-row > div"),
    toolbar = document.querySelector("#toolbar");
toolbarDiv.appendChild(toolbar);

var filterDiv= document.querySelector("#samplesTable_filter"),
  filterBtns = document.querySelector("#filter-btns");
filterDiv.appendChild(filterBtns);

/**
 * Add tags for clearing filters
 */
var filteredTags = document.querySelector("#filtered-tags");
document.querySelector(".datatables-active-filters").appendChild(filteredTags);


// Need to dynamically insert the 0 selected counts
document.querySelector(".selected-counts").innerHTML = PAGE.i18n.selectedCounts.none;

// Handle clicking the table rows.
document.querySelector("#samplesTable tbody").addEventListener("click", datatable.tbodyClickEvent, false);