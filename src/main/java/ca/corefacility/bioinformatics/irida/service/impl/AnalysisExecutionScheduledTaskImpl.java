package ca.corefacility.bioinformatics.irida.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;

import ca.corefacility.bioinformatics.irida.exceptions.ExecutionManagerException;
import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowException;
import ca.corefacility.bioinformatics.irida.exceptions.IridaWorkflowNotFoundException;
import ca.corefacility.bioinformatics.irida.model.enums.AnalysisCleanedState;
import ca.corefacility.bioinformatics.irida.model.enums.AnalysisState;
import ca.corefacility.bioinformatics.irida.model.workflow.analysis.Analysis;
import ca.corefacility.bioinformatics.irida.model.workflow.execution.galaxy.GalaxyWorkflowStatus;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.repositories.analysis.submission.AnalysisSubmissionRepository;
import ca.corefacility.bioinformatics.irida.service.AnalysisExecutionScheduledTask;
import ca.corefacility.bioinformatics.irida.service.CleanupAnalysisSubmissionCondition;
import ca.corefacility.bioinformatics.irida.service.analysis.execution.AnalysisExecutionService;

import com.google.common.collect.Sets;

/**
 * Implementation of analysis execution tasks. This will scan for
 * {@link AnalysisSubmission}s and execute the {@link Analysis} defined by the
 * submissions.
 * 
 *
 */
public class AnalysisExecutionScheduledTaskImpl implements AnalysisExecutionScheduledTask {
	
	private Object prepareAnalysesLock = new Object();
	private Object executeAnalysesLock = new Object();
	private Object monitorRunningAnalysesLock = new Object();
	private Object transferAnalysesResultsLock = new Object();
	private Object cleanupAnalysesResultsLock = new Object();

	private static final Logger logger = LoggerFactory.getLogger(AnalysisExecutionScheduledTaskImpl.class);

	private AnalysisSubmissionRepository analysisSubmissionRepository;
	private AnalysisExecutionService analysisExecutionService;
	private final CleanupAnalysisSubmissionCondition cleanupCondition;

	/**
	 * Builds a new AnalysisExecutionScheduledTaskImpl with the given service
	 * classes.
	 * 
	 * @param analysisSubmissionRepository
	 *            A repository for {@link AnalysisSubmission}s.
	 * @param analysisExecutionServiceGalaxy
	 *            A service for executing {@link AnalysisSubmission}s.
	 * @param cleanupCondition
	 *            The condition defining when an {@link AnalysisSubmission}
	 *            should be cleaned up.
	 */
	@Autowired
	public AnalysisExecutionScheduledTaskImpl(AnalysisSubmissionRepository analysisSubmissionRepository,
			AnalysisExecutionService analysisExecutionServiceGalaxy, CleanupAnalysisSubmissionCondition cleanupCondition) {
		this.analysisSubmissionRepository = analysisSubmissionRepository;
		this.analysisExecutionService = analysisExecutionServiceGalaxy;
		this.cleanupCondition = cleanupCondition;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Future<AnalysisSubmission>> prepareAnalyses() {
		synchronized (prepareAnalysesLock) {
			logger.trace("Running prepareAnalyses");

			List<AnalysisSubmission> analysisSubmissions = analysisSubmissionRepository
					.findByAnalysisState(AnalysisState.NEW);

			Set<Future<AnalysisSubmission>> submissions = Sets.newHashSet();

			// check to see if execution service wants any more jobs
			int capacity = analysisExecutionService.getCapacity();
			if (capacity > 0) {

				if (capacity < analysisSubmissions.size()) {
					logger.debug("Attempting to submit more jobs than capacity, list will be trimmed: "
							+ analysisSubmissions.size() + "=>" + capacity);
					// only submit up to capacity
					analysisSubmissions = analysisSubmissions.subList(0, capacity);
				}

				for (AnalysisSubmission analysisSubmission : analysisSubmissions) {
					logger.debug("Preparing " + analysisSubmission);

					try {
						submissions.add(analysisExecutionService.prepareSubmission(analysisSubmission));
					} catch (ExecutionManagerException | IridaWorkflowNotFoundException | IOException e) {
						logger.error("Error preparing submission " + analysisSubmission, e);
					}
				}
			} else {
				logger.trace("AnalysisExecutionService at max capacity.  No jobs updated.");
			}

			return submissions;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Future<AnalysisSubmission>> executeAnalyses() {
		synchronized(executeAnalysesLock) {
			logger.trace("Running executeAnalyses");
			
			List<AnalysisSubmission> analysisSubmissions = analysisSubmissionRepository
					.findByAnalysisState(AnalysisState.PREPARED);
	
			Set<Future<AnalysisSubmission>> submissions = Sets.newHashSet();
	
			for (AnalysisSubmission analysisSubmission : analysisSubmissions) {
				logger.debug("Executing " + analysisSubmission);
	
				try {
					submissions.add(analysisExecutionService.executeAnalysis(analysisSubmission));
				} catch (ExecutionManagerException | IridaWorkflowException e) {
					logger.error("Error executing submission " + analysisSubmission, e);
				}
			}
	
			return submissions;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Future<AnalysisSubmission>> monitorRunningAnalyses() {
		synchronized(monitorRunningAnalysesLock) {
			logger.trace("Running monitorRunningAnalyses");

			List<AnalysisSubmission> analysisSubmissions = analysisSubmissionRepository
					.findByAnalysisState(AnalysisState.RUNNING);
	
			Set<Future<AnalysisSubmission>> submissions = Sets.newHashSet();
	
			for (AnalysisSubmission analysisSubmission : analysisSubmissions) {
				logger.trace("Checking state of " + analysisSubmission);
	
				try {
					GalaxyWorkflowStatus workflowStatus = analysisExecutionService
							.getWorkflowStatus(analysisSubmission);
					submissions.add(handleWorkflowStatus(workflowStatus, analysisSubmission));
				} catch (ExecutionManagerException | RuntimeException e) {
					logger.error("Error checking state for " + analysisSubmission, e);
					analysisSubmission.setAnalysisState(AnalysisState.ERROR);
					submissions.add(new AsyncResult<>(analysisSubmissionRepository.save(analysisSubmission)));
				}
			}
	
			return submissions;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Future<AnalysisSubmission>> transferAnalysesResults() {
		synchronized (transferAnalysesResultsLock) {
			logger.trace("Running transferAnalysesResults");

			List<AnalysisSubmission> analysisSubmissions = analysisSubmissionRepository
					.findByAnalysisState(AnalysisState.FINISHED_RUNNING);

			Set<Future<AnalysisSubmission>> submissions = Sets.newHashSet();

			for (AnalysisSubmission analysisSubmission : analysisSubmissions) {
				logger.debug("Transferring results for " + analysisSubmission);

				try {
					submissions.add(analysisExecutionService.transferAnalysisResults(analysisSubmission));
				} catch (ExecutionManagerException | IOException | IridaWorkflowException e) {
					logger.error("Error transferring submission " + analysisSubmission, e);
				}
			}

			return submissions;
		}
	}

	/**
	 * Handles checking the status of a workflow in an execution manager.
	 * 
	 * @param workflowStatus
	 *            The status of the workflow.
	 * @param analysisSubmission
	 *            The {@link AnalysisSubmission}.
	 * @return A {@link Future} with an {@link AnalysisSubmission} for this
	 *         submission.
	 */
	private Future<AnalysisSubmission> handleWorkflowStatus(GalaxyWorkflowStatus workflowStatus,
			AnalysisSubmission analysisSubmission) {
		Future<AnalysisSubmission> returnedSubmission;

		// Immediately switch overall workflow state to "ERROR" if an error occurred, even if some tools are still running.
		if (workflowStatus.errorOccurred()) {
			logger.error("Workflow for analysis " + analysisSubmission + " in error state " + workflowStatus);

			analysisSubmission.setAnalysisState(AnalysisState.ERROR);
			returnedSubmission = new AsyncResult<>(analysisSubmissionRepository.save(analysisSubmission));
		} else if (workflowStatus.completedSuccessfully()) {
			logger.debug("Analysis finished " + analysisSubmission);

			analysisSubmission.setAnalysisState(AnalysisState.FINISHED_RUNNING);
			returnedSubmission = new AsyncResult<>(analysisSubmissionRepository.save(analysisSubmission));
		} else if (workflowStatus.isRunning()) {
			logger.trace("Workflow for analysis " + analysisSubmission + " is running: proportion complete "
					+ workflowStatus.getProportionComplete());
			returnedSubmission = new AsyncResult<>(analysisSubmission);
		} else {
			// If one of the above combinations did not match, assume an error occurred.
			logger.error("Workflow for analysis " + analysisSubmission + " is neither complete, in error, or still running. Switching to error state " + workflowStatus);

			analysisSubmission.setAnalysisState(AnalysisState.ERROR);
			returnedSubmission = new AsyncResult<>(analysisSubmissionRepository.save(analysisSubmission));
		}

		return returnedSubmission;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<Future<AnalysisSubmission>> cleanupAnalysisSubmissions() {
		synchronized (cleanupAnalysesResultsLock) {
			logger.trace("Running cleanupAnalysisSubmissions");

			List<AnalysisSubmission> analysisSubmissions = analysisSubmissionRepository.findByAnalysisState(
					AnalysisState.COMPLETED, AnalysisCleanedState.NOT_CLEANED);
			analysisSubmissions.addAll(analysisSubmissionRepository.findByAnalysisState(AnalysisState.ERROR,
					AnalysisCleanedState.NOT_CLEANED));

			Set<Future<AnalysisSubmission>> cleanedSubmissions = Sets.newHashSet();

			for (AnalysisSubmission submission : analysisSubmissions) {
				if (AnalysisCleanedState.NOT_CLEANED.equals(submission.getAnalysisCleanedState())
						&& cleanupCondition.shouldCleanupSubmission(submission)) {
					logger.trace("Attempting to clean up submission " + submission);

					try {
						Future<AnalysisSubmission> cleanedSubmissionFuture = analysisExecutionService
								.cleanupSubmission(submission);
						cleanedSubmissions.add(cleanedSubmissionFuture);
					} catch (ExecutionManagerException e) {
						logger.error("Error cleaning submission " + submission, e);
					}
				}
			}

			return cleanedSubmissions;
		}
	}
}
