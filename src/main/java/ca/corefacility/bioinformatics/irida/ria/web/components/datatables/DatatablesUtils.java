package ca.corefacility.bioinformatics.irida.ria.web.components.datatables;

import java.util.Map;

import org.springframework.data.domain.Sort;

import com.github.dandelion.datatables.core.ajax.ColumnDef;
import com.github.dandelion.datatables.core.ajax.DatatablesCriterias;
import com.google.common.collect.ImmutableMap;

/**
 * Handles decipher requests made using {@link DatatablesCriterias}
 */
public class DatatablesUtils {
	private static int DEFAULT_PAGE_SIZE = 10;
	public static String SORT_DIRECTION = "direction";
	public static String SORT_STRING = "sort_string";

	/**
	 * Switch the {@link DatatablesCriterias} {@link ColumnDef.SortDirection} for a {@link Sort.Direction}
	 *
	 * @param criterias {@link DatatablesCriterias}
	 * @return {@link Sort.Direction}
	 */
	public static Map<String, Object> getSortProperties(DatatablesCriterias criterias) {
		ColumnDef sortedColumn = criterias.getSortedColumnDefs().get(0);
		return ImmutableMap.of(
				SORT_DIRECTION, generateSortDirection(sortedColumn),
				SORT_STRING, sortedColumn.getName()
		);
	}

	public static Sort.Direction generateSortDirection(ColumnDef def) {
		return def.getSortDirection().equals(ColumnDef.SortDirection.ASC) ?
				Sort.Direction.ASC :
				Sort.Direction.DESC;
	}

	/**
	 * Determine the current page based on {@link DatatablesCriterias}
	 *
	 * @param criterias {@link DatatablesCriterias}
	 * @return {@link Integer} the current page of the datatable
	 */
	public static int getCurrentPage(DatatablesCriterias criterias) {
		int pageSize = criterias.getLength() > 0 ? criterias.getLength() : DEFAULT_PAGE_SIZE;
		return (int) Math.floor(criterias.getStart() / pageSize);
	}
}
