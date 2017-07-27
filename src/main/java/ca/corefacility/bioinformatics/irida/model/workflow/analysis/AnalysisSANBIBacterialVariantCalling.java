package ca.corefacility.bioinformatics.irida.model.workflow.analysis;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Metadata for SANBI Bacterial Variant Calling.
 * 
 *
 */
@Entity
@Table(name = "analysis_sanbi_bacterial_variant_calling")
public class AnalysisSANBIBacterialVariantCalling extends Analysis {

	/**
	 * required for hibernate, marked as private so nobody else uses it.
	 */
	@SuppressWarnings("unused")
	private AnalysisSANBIBacterialVariantCalling() {
		super();
	}

	public AnalysisSANBIBacterialVariantCalling(final String executionManagerAnalysisId,
			final Map<String, AnalysisOutputFile> analysisOutputFilesMap) {
		super(executionManagerAnalysisId, analysisOutputFilesMap);
	}

	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallResults() {
		return getAnalysisOutputFile("phylogeneticTree");
	}
}
