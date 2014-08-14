package ca.corefacility.bioinformatics.irida.service.analysis;

import java.nio.file.Path;
import java.util.Set;

import ca.corefacility.bioinformatics.irida.model.workflow.analysis.Analysis;
import ca.corefacility.bioinformatics.irida.service.analysis.impl.galaxy.RemoteWorkflow;

public interface AnalysisSubmission<T extends RemoteWorkflow<?>> {

	public void setSequenceFiles(Set<Path> sequenceFiles);

	public void setReferenceFile(Path referenceFile);

	public void setRemoteWorkflow(T remoteWorkflow);

	public void setAnalysisType(Class<? extends Analysis> analysisType);

	public T getRemoteWorkflow();

	public Set<Path> getSequenceFiles();

	public Path getReferenceFile();
}
