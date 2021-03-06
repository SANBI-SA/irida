package ca.corefacility.bioinformatics.irida.web.controller.test.integration.analysis;

import static ca.corefacility.bioinformatics.irida.web.controller.test.integration.util.ITestAuthUtils.asAdmin;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.Link;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

import ca.corefacility.bioinformatics.irida.config.data.IridaApiJdbcDataSourceConfig;
import ca.corefacility.bioinformatics.irida.config.services.IridaApiPropertyPlaceholderConfig;
import ca.corefacility.bioinformatics.irida.config.services.IridaApiServicesConfig;
import ca.corefacility.bioinformatics.irida.model.enums.AnalysisState;
import ca.corefacility.bioinformatics.irida.web.controller.api.RESTAnalysisSubmissionController;
import ca.corefacility.bioinformatics.irida.web.spring.view.NewickFileView;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { IridaApiJdbcDataSourceConfig.class,
		IridaApiPropertyPlaceholderConfig.class, IridaApiServicesConfig.class })
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
@ActiveProfiles("it")
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/web/controller/test/integration/analysis/RESTAnalysisSubmissionControllerIT.xml")
@DatabaseTearDown("classpath:/ca/corefacility/bioinformatics/irida/test/integration/TableReset.xml")
/**
 * Test for functions of {@link RESTAnalysisSubmissionController}
 */
public class RESTAnalysisSubmissionControllerIT {

	public static final String ANALYSIS_BASE = "/api/analysisSubmissions";
	
	private static final Logger logger = LoggerFactory.getLogger(RESTAnalysisSubmissionControllerIT.class);
	
	@Autowired
	@Qualifier("outputFileBaseDirectory")
	private Path outputFileBaseDirectory;

	@Test
	public void testReadSubmission() {
		asAdmin().expect().body("resource.name", equalTo("another analysis")).and()
				.body("resource.analysisState", equalTo(AnalysisState.COMPLETED.toString()))
				.body("resource.links.rel", hasItems(Link.REL_SELF, RESTAnalysisSubmissionController.ANALYSIS_REL))
				.when().get(ANALYSIS_BASE + "/1");
	}

	@Test
	public void testReadIncompleteSubmission() {
		asAdmin().expect().body("resource.analysisState", equalTo(AnalysisState.PREPARING.toString()))
				.body("resource.links.rel", not(hasItems(RESTAnalysisSubmissionController.ANALYSIS_REL))).when()
				.get(ANALYSIS_BASE + "/2");
	}

	@Test
	public void testGetAnalysis() {
		asAdmin()
				.expect()
				.body("resource.executionManagerAnalysisId", equalTo("XYZABC"))
				.and()
				.body("resource.links.rel",
						hasItems(Link.REL_SELF, RESTAnalysisSubmissionController.FILE_REL + "/tree")).when()
				.get(ANALYSIS_BASE + "/1/analysis");
	}

	@Test
	public void testGetOutputFile() throws IOException {
		
		final Path snpTree = Paths.get("src/test/resources/files/snp_tree.tree");
		try {
			Files.createDirectories(outputFileBaseDirectory.resolve(snpTree.getParent()));
		} catch (final FileAlreadyExistsException e) {
			logger.info("Directory already exists for snp tree.");
		}
		try {
			Files.copy(snpTree, outputFileBaseDirectory.resolve(snpTree));
		} catch (final FileAlreadyExistsException e) {
			logger.info("Already moved snp tree into directory.");
		}
		
		asAdmin().expect().body("resource.executionManagerFileId", equalTo("123-456-789")).and()
				.body("resource.label", equalTo("snp_tree.tree")).body("resource.links.rel", hasItems(Link.REL_SELF))
				.when().get(ANALYSIS_BASE + "/1/analysis/file/tree");

		// get the tree file
		asAdmin().given().header("Accept", NewickFileView.DEFAULT_CONTENT_TYPE).expect().body(containsString("c6706"))
				.when().get(ANALYSIS_BASE + "/1/analysis/file/tree");
	}
}
