package ca.corefacility.bioinformatics.irida.service.impl.analysis.submission;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;
import javax.validation.Validator;

import org.hibernate.TransientPropertyValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import ca.corefacility.bioinformatics.irida.exceptions.EntityExistsException;
import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.EntityRevisionDeletedException;
import ca.corefacility.bioinformatics.irida.exceptions.ExecutionManagerException;
import ca.corefacility.bioinformatics.irida.exceptions.NoPercentageCompleteException;
import ca.corefacility.bioinformatics.irida.model.enums.AnalysisCleanedState;
import ca.corefacility.bioinformatics.irida.model.enums.AnalysisState;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.project.ReferenceFile;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFilePair;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequencingObject;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SingleEndSequenceFile;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.model.workflow.IridaWorkflow;
import ca.corefacility.bioinformatics.irida.model.workflow.description.IridaWorkflowDescription;
import ca.corefacility.bioinformatics.irida.model.workflow.execution.galaxy.GalaxyWorkflowStatus;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.AnalysisSubmission;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.IridaWorkflowNamedParameters;
import ca.corefacility.bioinformatics.irida.model.workflow.submission.ProjectAnalysisSubmissionJoin;
import ca.corefacility.bioinformatics.irida.pipeline.upload.galaxy.GalaxyHistoriesService;
import ca.corefacility.bioinformatics.irida.repositories.analysis.submission.AnalysisSubmissionRepository;
import ca.corefacility.bioinformatics.irida.repositories.analysis.submission.ProjectAnalysisSubmissionJoinRepository;
import ca.corefacility.bioinformatics.irida.repositories.referencefile.ReferenceFileRepository;
import ca.corefacility.bioinformatics.irida.repositories.user.UserRepository;
import ca.corefacility.bioinformatics.irida.service.AnalysisSubmissionService;
import ca.corefacility.bioinformatics.irida.service.SequencingObjectService;
import ca.corefacility.bioinformatics.irida.service.analysis.execution.galaxy.AnalysisExecutionServiceGalaxyCleanupAsync;
import ca.corefacility.bioinformatics.irida.service.impl.CRUDServiceImpl;

/**
 * Implementation of an AnalysisSubmissionService.
 * 
 *
 */
@Service
public class AnalysisSubmissionServiceImpl extends CRUDServiceImpl<Long, AnalysisSubmission> implements
		AnalysisSubmissionService {
	private static final Logger logger = LoggerFactory.getLogger(AnalysisSubmissionServiceImpl.class);
	
	/**
	 * A {@link Map} defining the progress transitions points for each state in
	 * an {@link AnalysisSubmission}.
	 */
	// @formatter:off
	public static final Map<AnalysisState,Float> STATE_PERCENTAGE = ImmutableMap.<AnalysisState,Float>builder().
			put(AnalysisState.NEW,                  0.0f).
			put(AnalysisState.PREPARING,            10.0f).
			put(AnalysisState.PREPARED,             11.0f).
			put(AnalysisState.SUBMITTING,           15.0f).
			put(AnalysisState.RUNNING,              20.0f).
			put(AnalysisState.FINISHED_RUNNING,     90.0f).
			put(AnalysisState.COMPLETING,           95.0f).
			put(AnalysisState.COMPLETED,            100.0f).
			build();
	// @formatter:on
	
	private static final float RUNNING_PERCENT = STATE_PERCENTAGE.get(AnalysisState.RUNNING);
	private static final float FINISHED_RUNNING_PERCENT = STATE_PERCENTAGE.get(AnalysisState.FINISHED_RUNNING);
	
	private UserRepository userRepository;
	private AnalysisSubmissionRepository analysisSubmissionRepository;
	private ProjectAnalysisSubmissionJoinRepository pasRepository;
	private final ReferenceFileRepository referenceFileRepository;
	private final GalaxyHistoriesService galaxyHistoriesService;
	private final SequencingObjectService sequencingObjectService;
	
	// required, but not constructor injected because we have circular dependencies :(
	@Autowired
	private AnalysisExecutionServiceGalaxyCleanupAsync analysisExecutionService;

	/**
	 * Builds a new AnalysisSubmissionServiceImpl with the given information.
	 * 
	 * @param analysisSubmissionRepository
	 *            A repository for accessing analysis submissions.
	 * @param userRepository
	 *            A repository for accessing user information.
	 * @param referenceFileRepository
	 *            the reference file repository
	 * @param sequencingObjectService
	 *            the {@link SequencingObject} service.

	 * @param galaxyHistoriesService
	 *            The {@link GalaxyHistoriesService}.
	 * @param validator
	 *            A validator.
	 */
	@Autowired
	public AnalysisSubmissionServiceImpl(AnalysisSubmissionRepository analysisSubmissionRepository,
			UserRepository userRepository, final ReferenceFileRepository referenceFileRepository,
			final SequencingObjectService sequencingObjectService, final GalaxyHistoriesService galaxyHistoriesService,
			ProjectAnalysisSubmissionJoinRepository pasRepository, Validator validator) {
		super(analysisSubmissionRepository, validator, AnalysisSubmission.class);
		this.userRepository = userRepository;
		this.analysisSubmissionRepository = analysisSubmissionRepository;
		this.referenceFileRepository = referenceFileRepository;
		this.galaxyHistoriesService = galaxyHistoriesService;
		this.sequencingObjectService = sequencingObjectService;
		this.pasRepository = pasRepository;
	}
	
	public void setAnalysisExecutionService(final AnalysisExecutionServiceGalaxyCleanupAsync analysisExecutionService) {
		this.analysisExecutionService = analysisExecutionService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#analysisSubmissionId, 'canReadAnalysisSubmission')")
	public AnalysisState getStateForAnalysisSubmission(Long analysisSubmissionId) throws EntityNotFoundException {
		checkNotNull(analysisSubmissionId, "analysisSubmissionId is null");

		AnalysisSubmission submission = this.read(analysisSubmissionId);

		return submission.getAnalysisState();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#id, 'canReadAnalysisSubmission')")
	public AnalysisSubmission read(Long id) throws EntityNotFoundException {
		return super.read(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#idents, 'canReadAnalysisSubmission')")
	public Iterable<AnalysisSubmission> readMultiple(Iterable<Long> idents) {
		return super.readMultiple(idents);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public Iterable<AnalysisSubmission> findAll() {
		return super.findAll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_USER')")
	public Boolean exists(Long id) {
		return super.exists(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#id, 'canReadAnalysisSubmission')")
	public Revisions<Integer, AnalysisSubmission> findRevisions(Long id) throws EntityRevisionDeletedException {
		return super.findRevisions(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#id, 'canReadAnalysisSubmission')")
	public Page<Revision<Integer, AnalysisSubmission>> findRevisions(Long id, Pageable pageable)
			throws EntityRevisionDeletedException {
		return super.findRevisions(id, pageable);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public Page<AnalysisSubmission> list(int page, int size, Direction order, String... sortProperties)
			throws IllegalArgumentException {
		return super.list(page, size, order, sortProperties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public Page<AnalysisSubmission> list(int page, int size, Direction order) {
		return super.list(page, size, order);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public long count() {
		return super.count();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#id, 'canUpdateAnalysisSubmission')")
	@Transactional
	public void delete(Long id) throws EntityNotFoundException {
		final AnalysisSubmission submission = read(id);
		
		if (AnalysisCleanedState.NOT_CLEANED.equals(submission.getAnalysisCleanedState())) {
			// We're "CLEANING" it right now!
			submission.setAnalysisCleanedState(AnalysisCleanedState.CLEANING);
			try {
				analysisExecutionService.cleanupSubmission(submission).get();
			} catch (final ExecutionManagerException e) {
				logger.error("Failed to cleanup analysis submission before deletion,"
						+ " but proceeding with deletion anyway.", e);
			} catch (final Throwable e) {
				logger.error("An unexpected exception happened when cleaning the analysis submission,"
						+ " but proceeding with deletion anyway.", e);
			}
		} else {
			logger.debug("Not cleaning submission [" + id + "] when deleting, it's already cleaned.");
		}
		
		super.delete(id);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#object, 'canReadAnalysisSubmission')")
	public AnalysisSubmission update(AnalysisSubmission object) {
		return super.update(object);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_USER')")
	public AnalysisSubmission create(AnalysisSubmission analysisSubmission) throws ConstraintViolationException,
			EntityExistsException {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		User user = userRepository.loadUserByUsername(userDetails.getUsername());
		analysisSubmission.setSubmitter(user);
		
		try {
			return super.create(analysisSubmission);
		} catch (final InvalidDataAccessApiUsageException e) {
			// if the exception is because we're using unsaved properties, try to wrap the exception with a sane-er message.
			if (e.getCause() != null) {
				final Throwable primaryCause = e.getCause();
				if (primaryCause.getCause() != null
						&& primaryCause.getCause() instanceof TransientPropertyValueException) {
					final TransientPropertyValueException propertyException = (TransientPropertyValueException) primaryCause
							.getCause();
					if (Objects.equals("namedParameters", propertyException.getPropertyName())) {
						throw new UnsupportedOperationException(
								"You must save the named properties *before* you use them in a submission.", e);
					}
				}
			}

			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_USER')")
	public Page<AnalysisSubmission> search(Specification<AnalysisSubmission> specification, int page, int size,
			Direction order, String... sortProperties) {
		return super.search(specification, page, size, order, sortProperties);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or authentication.name == #user.username")
	public Set<AnalysisSubmission> getAnalysisSubmissionsForUser(User user) {
		checkNotNull(user, "user is null");

		return analysisSubmissionRepository.findBySubmitter(user);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_USER')")
	public Set<AnalysisSubmission> getAnalysisSubmissionsForCurrentUser() {
		UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		User user = userRepository.loadUserByUsername(userDetails.getUsername());
		return getAnalysisSubmissionsForUser(user);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	@Transactional
	@PreAuthorize("hasRole('ROLE_USER')")
	public Collection<AnalysisSubmission> createSingleSampleSubmission(IridaWorkflow workflow, Long ref,
			List<SingleEndSequenceFile> sequenceFiles, List<SequenceFilePair> sequenceFilePairs,
			Map<String, String> params, IridaWorkflowNamedParameters namedParameters, String name,
			String analysisDescription, List<Project> projectsToShare) {
		final Collection<AnalysisSubmission> createdSubmissions = new HashSet<AnalysisSubmission>();
		// Single end reads
		IridaWorkflowDescription description = workflow.getWorkflowDescription();
			
		if (description.acceptsSingleSequenceFiles()) {
			final Map<Sample, SingleEndSequenceFile> samplesMap = sequencingObjectService
					.getUniqueSamplesForSequencingObjects(Sets.newHashSet(sequenceFiles));
			for (final Sample s : samplesMap.keySet()) {
				// Build the analysis submission
				AnalysisSubmission.Builder builder = AnalysisSubmission.builder(workflow.getWorkflowIdentifier());
				builder.name(name + "_" + s.getSampleName());
				builder.inputFiles(ImmutableSet.of(samplesMap.get(s)));

				// Add reference file
				if (ref != null && description.requiresReference()) {
					// Note: This cannot be empty if through the UI if the
					// pipeline required a reference file.
					ReferenceFile referenceFile = referenceFileRepository.findOne(ref);
					builder.referenceFile(referenceFile);
				}

				if (description.acceptsParameters()) {
					if (namedParameters != null) {
						builder.withNamedParameters(namedParameters);
					} else {
						if (!params.isEmpty()) {
							// Note: This cannot be empty if through the UI if
							// the pipeline required params.
							builder.inputParameters(params);
						}
					}
				}

				// Create the submission
				createdSubmissions.add(create(builder.build()));
			}
			
		}

		// Paired end reads
		if (description.acceptsPairedSequenceFiles()) {
			final Map<Sample, SequenceFilePair> samplesMap = sequencingObjectService
					.getUniqueSamplesForSequencingObjects(Sets.newHashSet(sequenceFilePairs));
			for (final Sample s : samplesMap.keySet()) {
				// Build the analysis submission
				AnalysisSubmission.Builder builder = AnalysisSubmission.builder(workflow.getWorkflowIdentifier());
				builder.name(name + "_" + s.getSampleName());
				builder.inputFiles(ImmutableSet.of(samplesMap.get(s)));

				// Add reference file
				if (ref != null && description.requiresReference()) {
					ReferenceFile referenceFile = referenceFileRepository.findOne(ref);
					builder.referenceFile(referenceFile);
				}

				if (description.acceptsParameters()) {
					if (namedParameters != null) {
						builder.withNamedParameters(namedParameters);
					} else {
						if (!params.isEmpty()) {
							// Note: This cannot be empty if through the UI if
							// the pipeline required params.
							builder.inputParameters(params);
						}
					}
				}

				// Add description to submission, can be null
				builder.analysisDescription(analysisDescription);

				// Create the submission
				createdSubmissions.add(create(builder.build()));
			}
		}
		
		// Share with the required projects
		for (AnalysisSubmission submission : createdSubmissions) {
			for (Project project : projectsToShare) {
				pasRepository.save(new ProjectAnalysisSubmissionJoin(project, submission));
			}
		}
		
		return createdSubmissions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Transactional
	@PreAuthorize("hasRole('ROLE_USER')")
	public AnalysisSubmission createMultipleSampleSubmission(IridaWorkflow workflow, Long ref,
			List<SingleEndSequenceFile> sequenceFiles, List<SequenceFilePair> sequenceFilePairs,
			Map<String, String> params, IridaWorkflowNamedParameters namedParameters, String name,
			String newAnalysisDescription, List<Project> projectsToShare) {
		AnalysisSubmission.Builder builder = AnalysisSubmission.builder(workflow.getWorkflowIdentifier());
		builder.name(name);
		IridaWorkflowDescription description = workflow.getWorkflowDescription();

		// Add reference file
		if (ref != null && description.requiresReference()) {
			ReferenceFile referenceFile = referenceFileRepository.findOne(ref);
			builder.referenceFile(referenceFile);
		}

		// Add any single end sequencing files.
		if (description.acceptsSingleSequenceFiles()) {
			if (!sequenceFiles.isEmpty()) {
				builder.inputFiles(Sets.newHashSet(sequenceFiles));
			}
		}

		// Add any paired end sequencing files.
		if (description.acceptsPairedSequenceFiles()) {
			if (!sequenceFilePairs.isEmpty())
			{
				builder.inputFiles(Sets.newHashSet(sequenceFilePairs));
			}
		}

		if (description.acceptsParameters()) {
			if (namedParameters != null) {
				builder.withNamedParameters(namedParameters);
			} else {
				if (!params.isEmpty()) {
					// Note: This cannot be empty if through the UI if
					// the pipeline required params.
					builder.inputParameters(params);
				}
			}
		}

		// Add description to submission, can be null
		builder.analysisDescription(newAnalysisDescription);

		// Create the submission
		AnalysisSubmission submission = create(builder.build());

		// Share with the required projects
		for (Project project : projectsToShare) {
			pasRepository.save(new ProjectAnalysisSubmissionJoin(project, submission));
		}

		return submission;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN') or hasPermission(#id, 'canReadAnalysisSubmission')")
	public float getPercentCompleteForAnalysisSubmission(Long id) throws EntityNotFoundException,
			ExecutionManagerException, NoPercentageCompleteException {
		AnalysisSubmission analysisSubmission = read(id);
		AnalysisState analysisState = analysisSubmission.getAnalysisState();

		switch (analysisState) {
		case NEW:
		case PREPARING:
		case PREPARED:
		case SUBMITTING:
			return STATE_PERCENTAGE.get(analysisState);

			/**
			 * If the analysis is in a state of {@link AnalysisState.RUNNING}
			 * then we are able to ask Galaxy for the proportion of jobs that
			 * are complete. We can scale this value between RUNNING_PERCENT
			 * (10%) and FINISHED_RUNNING_PERCENT (90%) so that after all jobs
			 * are complete we are only at 90%. The remaining 10% involves
			 * transferring files back to Galaxy.
			 * 
			 * For example, if there are 10 out of 20 jobs finished on Galaxy,
			 * then the proportion of jobs complete is 10/20 = 0.5. So, the
			 * percent complete for the overall analysis is: percentComplete =
			 * 10 + (90 - 10) * 0.5 = 50%.
			 * 
			 * If there are 20 out of 20 jobs finished in Galaxy, then the
			 * percent complete is: percentComplete = 10 + (90 - 10) * 1.0 =
			 * 90%.
			 */
		case RUNNING:
			String workflowHistoryId = analysisSubmission.getRemoteAnalysisId();
			GalaxyWorkflowStatus workflowStatus = galaxyHistoriesService.getStatusForHistory(workflowHistoryId);
			return RUNNING_PERCENT + (FINISHED_RUNNING_PERCENT - RUNNING_PERCENT)
					* workflowStatus.getProportionComplete();
			
		case FINISHED_RUNNING:
		case COMPLETING:
		case COMPLETED:
			return STATE_PERCENTAGE.get(analysisState);
		default:
			throw new NoPercentageCompleteException("No valid percent complete for state " + analysisState);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@PreAuthorize("hasPermission(#submission, 'canUpdateAnalysisSubmission') AND hasPermission(#project, 'canReadProject')")
	@Override
	public ProjectAnalysisSubmissionJoin shareAnalysisSubmissionWithProject(AnalysisSubmission submission,
			Project project) {
		return pasRepository.save(new ProjectAnalysisSubmissionJoin(project, submission));
	}

	/**
	 * {@inheritDoc}
	 */
	@PreAuthorize("hasPermission(#submission, 'canUpdateAnalysisSubmission') AND hasPermission(#project, 'canReadProject')")
	@Override
	public void removeAnalysisProjectShare(AnalysisSubmission submission, Project project) {
		ProjectAnalysisSubmissionJoin projectSubmissionShare = pasRepository.getProjectSubmissionShare(submission,
				project);
		pasRepository.delete(projectSubmissionShare);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public Collection<AnalysisSubmission> findAnalysesByState(Collection<AnalysisState> states) {
		return analysisSubmissionRepository.findByAnalysisState(states);
	}
}
