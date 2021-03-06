package ca.corefacility.bioinformatics.irida.ria.unit.web.samples;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import ca.corefacility.bioinformatics.irida.model.enums.ProjectRole;
import ca.corefacility.bioinformatics.irida.model.joins.impl.ProjectSampleJoin;
import ca.corefacility.bioinformatics.irida.model.joins.impl.ProjectUserJoin;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.sample.Sample;
import ca.corefacility.bioinformatics.irida.model.sample.SampleSequencingObjectJoin;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFile;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequenceFilePair;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SequencingObject;
import ca.corefacility.bioinformatics.irida.model.sequenceFile.SingleEndSequenceFile;
import ca.corefacility.bioinformatics.irida.model.user.Role;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.ria.unit.TestDataFactory;
import ca.corefacility.bioinformatics.irida.ria.web.samples.SamplesController;
import ca.corefacility.bioinformatics.irida.service.ProjectService;
import ca.corefacility.bioinformatics.irida.service.SequencingObjectService;
import ca.corefacility.bioinformatics.irida.service.sample.MetadataTemplateService;
import ca.corefacility.bioinformatics.irida.service.sample.SampleService;
import ca.corefacility.bioinformatics.irida.service.user.UserService;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

/**
 */
public class SamplesControllerTest {
	public static final String[] MULTIPARTFILE_PATHS = { "src/test/resources/files/test_file_A.fastq",
			"src/test/resources/files/test_file_B.fastq" };
	public static final String[] MULTIPARTFILE_PAIR_PATHS = { "src/test/resources/files/pairs/pair_test_R1_001.fastq",
			"src/test/resources/files/pairs/pair_test_R2_001.fastq" };

	// Services
	private SamplesController controller;
	private SampleService sampleService;
	private UserService userService;
	private ProjectService projectService;
	private SequencingObjectService sequencingObjectService;
	private MetadataTemplateService metadataTemplateService;
	private MessageSource messageSource;

	@Before
	public void setUp() {
		sampleService = mock(SampleService.class);
		userService = mock(UserService.class);
		projectService = mock(ProjectService.class);
		sequencingObjectService = mock(SequencingObjectService.class);
		metadataTemplateService = mock(MetadataTemplateService.class);
		messageSource = mock(MessageSource.class);
		controller = new SamplesController(sampleService, userService, projectService, sequencingObjectService, metadataTemplateService,
				messageSource);
	}

	// ************************************************************************************************
	// PAGE REQUESTS
	// ************************************************************************************************

	@Test
	public void testGetSampleSpecificPage() {
		String userName = "bob";
		Principal principal = () -> userName;
		User user = new User();
		user.setSystemRole(Role.ROLE_ADMIN);
		when(userService.getUserByUsername(userName)).thenReturn(user);
		
		
		Model model = new ExtendedModelMap();
		Sample sample = TestDataFactory.constructSample();
		when(sampleService.read(sample.getId())).thenReturn(sample);
		String result = controller.getSampleSpecificPage(model, sample.getId(), principal);
		assertEquals("Returns the correct page name", "samples/sample", result);
		assertTrue("Model contains the sample", model.containsAttribute("sample"));
	}

	@Test
	public void testGetEditSampleSpecificPage() {
		Model model = new ExtendedModelMap();
		Sample sample = TestDataFactory.constructSample();
		when(sampleService.read(sample.getId())).thenReturn(sample);
		String result = controller.getEditSampleSpecificPage(model, sample.getId());
		assertEquals("Returns the correct page name", "samples/sample_edit", result);
		assertTrue("Model contains the sample", model.containsAttribute("sample"));
		assertTrue("Model should ALWAYS have an error attribute", model.containsAttribute("errors"));
	}

	@Test
	public void testUpdateSample() {
		Model model = new ExtendedModelMap();
		Sample sample = TestDataFactory.constructSample();
		final String contextPath = "/some-nonsense";
		String organism = "E. coli";
		String geographicLocationName = "The Forks";
		Map<String, Object> updatedValues = ImmutableMap.of(SamplesController.ORGANISM, organism,
				SamplesController.GEOGRAPHIC_LOCATION_NAME, geographicLocationName);
		Map<String, String> update = ImmutableMap.of(SamplesController.ORGANISM, organism,
				SamplesController.GEOGRAPHIC_LOCATION_NAME, geographicLocationName);
		when(sampleService.updateFields(sample.getId(), updatedValues)).thenReturn(sample);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
				"/projects/5/samples/" + sample.getId() + "/edit");
		String result = controller.updateSample(model, sample.getId(), null, null, update, request);
		assertTrue("Returns the correct redirect", result.contains(sample.getId() + "/details"));
		assertTrue("Should be a redirect response.", result.startsWith("redirect:"));
		assertFalse("Redirect should **not** contain the context path.", result.contains(contextPath));
		assertTrue("Model should be populated with updated attributes",
				model.containsAttribute(SamplesController.ORGANISM));
		assertTrue("Model should be populated with updated attributes",
				model.containsAttribute(SamplesController.GEOGRAPHIC_LOCATION_NAME));
		assertFalse("Model should not be populated with non-updated attributes",
				model.containsAttribute(SamplesController.LATITUDE));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetSampleFiles() throws IOException {
		ExtendedModelMap model = new ExtendedModelMap();
		String userName = "bob";
		Principal principal = () -> userName;
		Long sampleId = 1L;
		Sample sample = new Sample();
		SequenceFile file = new SequenceFile(Paths.get("/tmp"));
		file.setId(2L);
		User user = new User();
		Project project = new Project();

		List<SampleSequencingObjectJoin> files = Lists.newArrayList(new SampleSequencingObjectJoin(sample,
				new SingleEndSequenceFile(file)));

		when(sampleService.read(sampleId)).thenReturn(sample);
		when(sequencingObjectService.getSequencesForSampleOfType(sample, SingleEndSequenceFile.class))
				.thenReturn(files);
		when(userService.getUserByUsername(userName)).thenReturn(user);
		when(projectService.getProjectsForSample(sample)).thenReturn(
				Lists.newArrayList(new ProjectSampleJoin(project, sample)));
		when(projectService.userHasProjectRole(user, project, ProjectRole.PROJECT_OWNER)).thenReturn(true);

		String sampleFiles = controller.getSampleFilesWithoutProject(model, sampleId, principal);

		assertEquals(SamplesController.SAMPLE_FILES_PAGE, sampleFiles);
		assertTrue((boolean) model.get(SamplesController.MODEL_ATTR_CAN_MANAGE_SAMPLE));

		verify(sampleService).read(sampleId);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SingleEndSequenceFile.class);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SequenceFilePair.class);
	}

	@Test
	public void testGetSampleFilesAsAdmin() throws IOException {
		ExtendedModelMap model = new ExtendedModelMap();
		String userName = "bob";
		Principal principal = () -> userName;
		Long sampleId = 1L;
		Sample sample = new Sample();
		SequenceFile file = new SequenceFile(Paths.get("/tmp"));
		file.setId(2L);
		User user = new User();
		user.setSystemRole(Role.ROLE_ADMIN);

		List<SampleSequencingObjectJoin> files = Lists.newArrayList(new SampleSequencingObjectJoin(sample,
				new SingleEndSequenceFile(file)));

		when(sampleService.read(sampleId)).thenReturn(sample);
		when(sequencingObjectService.getSequencesForSampleOfType(sample, SingleEndSequenceFile.class))
				.thenReturn(files);
		when(userService.getUserByUsername(userName)).thenReturn(user);

		String sampleFiles = controller.getSampleFilesWithoutProject(model, sampleId, principal);

		assertEquals(SamplesController.SAMPLE_FILES_PAGE, sampleFiles);
		assertTrue((boolean) model.get(SamplesController.MODEL_ATTR_CAN_MANAGE_SAMPLE));

		verify(sampleService).read(sampleId);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SingleEndSequenceFile.class);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SequenceFilePair.class);
		verifyZeroInteractions(projectService);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testGetSampleFilesNoAccess() throws IOException {
		ExtendedModelMap model = new ExtendedModelMap();
		String userName = "bob";
		Principal principal = () -> userName;
		Long sampleId = 1L;
		Sample sample = new Sample();
		SequenceFile file = new SequenceFile(Paths.get("/tmp"));
		file.setId(2L);
		User user = new User();
		Project project = new Project();

		List<SampleSequencingObjectJoin> files = Lists.newArrayList(new SampleSequencingObjectJoin(sample,
				new SingleEndSequenceFile(file)));

		when(sampleService.read(sampleId)).thenReturn(sample);

		when(sequencingObjectService.getSequencesForSampleOfType(sample, SingleEndSequenceFile.class))
				.thenReturn(files);

		when(userService.getUserByUsername(userName)).thenReturn(user);
		when(projectService.getProjectsForSample(sample)).thenReturn(
				Lists.newArrayList(new ProjectSampleJoin(project, sample)));
		when(userService.getUsersForProject(project)).thenReturn(
				(Lists.newArrayList(new ProjectUserJoin(project, user, ProjectRole.PROJECT_USER))));

		String sampleFiles = controller.getSampleFilesWithoutProject(model, sampleId, principal);

		assertEquals(SamplesController.SAMPLE_FILES_PAGE, sampleFiles);
		assertFalse((boolean) model.get(SamplesController.MODEL_ATTR_CAN_MANAGE_SAMPLE));

		verify(sampleService).read(sampleId);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SingleEndSequenceFile.class);
		verify(sequencingObjectService).getSequencesForSampleOfType(sample, SequenceFilePair.class);
	}

	@Test
	public void testRemoveFileFromSample() {
		Long sampleId = 1L;
		Long fileId = 2L;
		Sample sample = new Sample();
		SequencingObject file = new SingleEndSequenceFile(new SequenceFile(Paths.get("/tmp")));

		when(sampleService.read(sampleId)).thenReturn(sample);
		when(sequencingObjectService.readSequencingObjectForSample(sample, fileId)).thenReturn(file);

		RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();
		HttpServletRequest request = new MockHttpServletRequest();
		controller.removeSequencingObjectFromSample(attributes, sampleId, fileId, request, Locale.US);

		verify(sampleService).removeSequencingObjectFromSample(sample, file);
	}

	// ************************************************************************************************
	// AJAX REQUESTS
	// ************************************************************************************************

	@Test
	public void testUploadSequenceFiles() throws IOException {
		Sample sample = TestDataFactory.constructSample();
		when(sampleService.read(sample.getId())).thenReturn(sample);

		List<MultipartFile> fileList = createMultipartFileList(MULTIPARTFILE_PATHS);

		ArgumentCaptor<SingleEndSequenceFile> sequenceFileArgumentCaptor = ArgumentCaptor
				.forClass(SingleEndSequenceFile.class);

		HttpServletResponse response = new MockHttpServletResponse();
		controller.uploadSequenceFiles(sample.getId(), fileList, response);

		assertEquals("Response is ok", HttpServletResponse.SC_OK, response.getStatus());
		verify(sequencingObjectService, times(2)).createSequencingObjectInSample(sequenceFileArgumentCaptor.capture(),
				eq(sample));
		assertEquals("Should have the correct file name", "test_file_B.fastq", sequenceFileArgumentCaptor.getValue()
				.getLabel());
	}

	@Test
	public void testUploadSequenceFilePairs() throws IOException {
		Sample sample = TestDataFactory.constructSample();
		when(sampleService.read(sample.getId())).thenReturn(sample);

		List<MultipartFile> fileList = createMultipartFileList(MULTIPARTFILE_PAIR_PATHS);

		ArgumentCaptor<SequenceFilePair> sequenceFileArgumentCaptor = ArgumentCaptor.forClass(SequenceFilePair.class);

		HttpServletResponse response = new MockHttpServletResponse();
		controller.uploadSequenceFiles(sample.getId(), fileList, response);

		assertEquals("Response is ok", HttpServletResponse.SC_OK, response.getStatus());

		verify(sequencingObjectService)
				.createSequencingObjectInSample(sequenceFileArgumentCaptor.capture(), eq(sample));

		assertEquals("Should have the correct file name", "pair_test_R1_001.fastq", sequenceFileArgumentCaptor
				.getValue().getForwardSequenceFile().getLabel());
		assertEquals("Should have the correct file name", "pair_test_R2_001.fastq", sequenceFileArgumentCaptor
				.getValue().getReverseSequenceFile().getLabel());
	}

	@Test
	public void testUploadSequenceFilePairsAndSingle() throws IOException {
		Sample sample = TestDataFactory.constructSample();
		when(sampleService.read(sample.getId())).thenReturn(sample);

		List<MultipartFile> fileList = createMultipartFileList(ArrayUtils.addAll(MULTIPARTFILE_PATHS,
				MULTIPARTFILE_PAIR_PATHS));

		ArgumentCaptor<SequencingObject> sequenceFileArgumentCaptor = ArgumentCaptor.forClass(SequencingObject.class);

		HttpServletResponse response = new MockHttpServletResponse();
		controller.uploadSequenceFiles(sample.getId(), fileList, response);

		assertEquals("Response is ok", HttpServletResponse.SC_OK, response.getStatus());
		verify(sequencingObjectService, times(3)).createSequencingObjectInSample(sequenceFileArgumentCaptor.capture(),
				eq(sample));

		List<SequencingObject> allValues = sequenceFileArgumentCaptor.getAllValues();

		assertEquals("Should have created 2 single end sequence files", 2,
				allValues.stream().filter(o -> o instanceof SingleEndSequenceFile).count());
		assertEquals("Should have created 1 file pair", 1, allValues.stream()
				.filter(o -> o instanceof SequenceFilePair).count());
	}

	/**
	 * Create a list of {@link MultipartFile}
	 * 
	 * @param list
	 *            A list of paths to files.
	 * @return
	 * @throws IOException
	 */
	private List<MultipartFile> createMultipartFileList(String[] list) throws IOException {
		List<MultipartFile> fileList = new ArrayList<>();
		for (String pathName : list) {
			Path path = Paths.get(pathName);
			byte[] bytes = Files.readAllBytes(path);
			fileList.add(new MockMultipartFile(path.getFileName().toString(), path.getFileName().toString(),
					"octet-stream", bytes));
		}
		return fileList;
	}
}
