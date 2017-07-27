package ca.corefacility.bioinformatics.irida.model.enums;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.common.collect.Sets;

import ca.corefacility.bioinformatics.irida.model.workflow.analysis.Analysis;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisAssemblyAnnotation;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisAssemblyAnnotationCollection;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisPhylogenomicsPipeline;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisSISTRTyping;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.AnalysisSANBIBacterialVariantCalling;

/**
 * Defines a specific type of an analysis.
 * 
 *
 */
@XmlEnum
public enum AnalysisType {
	/**
	 * A SANBI bacterial variant calling analysis type for generating fasta trees.
	 */
	@XmlEnumValue("sanbi-bacterial-variant-calling")
	SANBI_BACTERIAL_VARIANT_CALLING("sanbi-bacterial-variant-calling", AnalysisSANBIBacterialVariantCalling.class),

	/**
	 * A phylogenomics analysis type for generating phylogenomic trees.
	 */
	@XmlEnumValue("phylogenomics")
	PHYLOGENOMICS("phylogenomics", AnalysisPhylogenomicsPipeline.class),

	/**
	 * SISTR Typing.
	 */
	@XmlEnumValue("sistr-typing")
	SISTR_TYPING("sistr-typing", AnalysisSISTRTyping.class),
	
	/**
	 * An assembly and annotation analysis type on a single sample.
	 */
	@XmlEnumValue("assembly-annotation")
	ASSEMBLY_ANNOTATION("assembly-annotation", AnalysisAssemblyAnnotation.class),

	/**
	 * An assembly and annotation analysis type on a collection of samples.
	 */
	@XmlEnumValue("assembly-annotation-collection")
	ASSEMBLY_ANNOTATION_COLLECTION("assembly-annotation-collection", AnalysisAssemblyAnnotationCollection.class),

	/**
	 * A default analysis type.
	 */
	@XmlEnumValue("default")
	DEFAULT("default", Analysis.class);

	/**
	 * Creates an {@link AnalysisType} from the corresponding String.
	 * 
	 * @param type
	 *            The string defining which type to return.
	 * @return The corresponding {@link AnalysisType}.
	 */
	public static AnalysisType fromString(String type) {
		checkNotNull(type, "type is null");
		checkArgument(typeMap.containsKey(type), "no corresponding AnalysisType for " + type);

		return typeMap.get(type);
	}

	private static Map<String, AnalysisType> typeMap = new HashMap<>();

	private String type;
	private Class<? extends Analysis> analysisClass;

	/**
	 * Sets of a Map used to convert a string to an AnalysisType
	 */
	static {
		for (AnalysisType type : AnalysisType.values()) {
			typeMap.put(type.toString(), type);
		}
	}

	private AnalysisType(String type, Class<? extends Analysis> analysisClass) {
		this.type = type;
		this.analysisClass = analysisClass;
	}

	/**
	 * Gets the particular {@link Analysis} class corresponding to this type.
	 * 
	 * @return An {@link Analysis} class for this type.
	 */
	public Class<? extends Analysis> getAnalysisClass() {
		return analysisClass;
	}

	/**
	 * Generates an array of all {@link AnalysisType}s minus the
	 * {@link AnalysisType.DEFAULT}.
	 * 
	 * @return An array of all {@link AnalysisType}s minus the
	 *         {@link AnalysisType.DEFAULT}
	 */
	public static AnalysisType[] valuesMinusDefault() {
		AnalysisType[] values = AnalysisType.values();
		Set<AnalysisType> valuesSet = Sets.newHashSet(values);
		valuesSet.remove(AnalysisType.DEFAULT);
		return valuesSet.toArray(new AnalysisType[values.length - 1]);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return type;
	}
}
