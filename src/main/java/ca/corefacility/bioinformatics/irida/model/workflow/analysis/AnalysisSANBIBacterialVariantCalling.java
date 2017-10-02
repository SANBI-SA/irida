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
	public AnalysisOutputFile getSANBIVariantCallTree() {
		return getAnalysisOutputFile("output_tree");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallOutputAlignment() {
		return getAnalysisOutputFile("output_alignment");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallSnpeffVcf() {
		return getAnalysisOutputFile("snpeff_output_vcf");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallFreebayesVcf() {
		return getAnalysisOutputFile("freebayes_output_vcf");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallGenomeCov() {
		return getAnalysisOutputFile("genomecov_file");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallMetricsFile() {
		return getAnalysisOutputFile("metrics_file");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallMarkDuplicateFile() {
		return getAnalysisOutputFile("markduplicates_outFile");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallCollectAlignSummary() {
		return getAnalysisOutputFile("collect_align_summary_outfile");
	}
	@JsonIgnore
	public AnalysisOutputFile getSANBIVariantCallNovoAlign() {
		return getAnalysisOutputFile("novoalign_file");
	}
}
