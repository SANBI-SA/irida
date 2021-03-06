package ca.corefacility.bioinformatics.irida.ria.integration.pipelines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.corefacility.bioinformatics.irida.ria.integration.AbstractIridaUIITChromeDriver;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.LoginPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.pipelines.PipelinesPhylogenomicsPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.pipelines.PipelinesSelectionPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.projects.AssociatedProjectEditPage;
import ca.corefacility.bioinformatics.irida.ria.integration.pages.projects.ProjectSamplesPage;
import ca.corefacility.bioinformatics.irida.ria.integration.utilities.RemoteApiUtilities;

import com.github.springtestdbunit.annotation.DatabaseSetup;

/**
 * <p>
 * Testing for launching a phylogenomics pipeline.
 * </p>
 *
 */
@DatabaseSetup("/ca/corefacility/bioinformatics/irida/ria/web/pipelines/PipelinePhylogenomicsView.xml")
public class PipelinesPhylogenomicsPageIT extends AbstractIridaUIITChromeDriver {
	private static final Logger logger = LoggerFactory.getLogger(PipelinesPhylogenomicsPageIT.class);
	private PipelinesPhylogenomicsPage page;

	@Before
	public void setUpTest() {
		page = new PipelinesPhylogenomicsPage(driver());
	}

	@Ignore
	@Test
	public void testPageSetup() {
		addSamplesToCart();

		logger.info("Checking Phylogenomics Page Setup.");
		assertEquals("Should display the correct number of reference files in the select input.", 2,
				page.getReferenceFileCount());
		assertEquals("Should display the correct number of samples.", 2, page.getNumberOfSamplesDisplayed());
	}

	@Ignore
	@Test
	public void testSubmitWithTransientReferenceFile() {
		LoginPage.loginAsUser(driver());

		// Add sample from a project that user is a "Project User" and has no
		// reference files.
		ProjectSamplesPage samplesPage = ProjectSamplesPage.gotToPage(driver(), 2);
		samplesPage.selectSample(1);
		samplesPage.addSelectedSamplesToCart();

		PipelinesSelectionPage.goToPhylogenomicsPipeline(driver());
		assertTrue("Should display a warning to the user that there are no reference files.",
				page.isNoReferenceWarningDisplayed());
		page.selectReferenceFile();
		assertTrue("Page should display reference file name.", page.isReferenceFileNameDisplayed());
	}

	@Ignore
	@Test
	public void testPipelineSubmission() {
		addSamplesToCart();

		page.clickLaunchPipelineBtn();
		assertTrue("Message should be displayed when the pipeline is submitted", page.isPipelineSubmittedMessageShown());
		assertTrue("Message should be displayed once the pipeline finished submitting",
				page.isPipelineSubmittedSuccessMessageShown());
	}

	@Ignore
	@Test
	public void testCheckPipelineStatusAfterSubmit() {
		addSamplesToCart();

		page.clickLaunchPipelineBtn();
		assertTrue("Message should be displayed once the pipeline finished submitting",
				page.isPipelineSubmittedSuccessMessageShown());
		page.clickSeePipeline();

		assertTrue("Should be on analysis page", driver().getCurrentUrl().endsWith("/analysis"));
	}

	@Ignore
	@Test
	public void testClearPipelineAndGetSamples() {
		addSamplesToCart();

		page.clickLaunchPipelineBtn();
		assertTrue("Message should be displayed once the pipeline finished submitting",
				page.isPipelineSubmittedSuccessMessageShown());
		page.clickClearAndFindMore();

		assertTrue("Should be on projects page", driver().getCurrentUrl().endsWith("/projects"));
		assertFalse("cart should be empty", page.isCartCountVisible());
	}

	@Ignore
	@Test
	public void testRemoveSample() {
		addSamplesToCart();

		int numberOfSamplesDisplayed = page.getNumberOfSamplesDisplayed();

		page.removeFirstSample();
		int laterNumber = page.getNumberOfSamplesDisplayed();

		assertEquals("should have 1 less sample than before", numberOfSamplesDisplayed - 1, laterNumber);
		assertEquals("cart samples count should equal samples on page", laterNumber, page.getCartCount());
	}

	@Ignore
	@Test
	public void testRemoveAllSample() {
		addSamplesToCart();

		page.removeFirstSample();
		page.removeFirstSample();

		assertTrue("user should be redirected to pipelinese page", driver().getCurrentUrl().endsWith("/pipelines"));
	}

	@Ignore
	@Test
	public void testModifyParameters() {
		addSamplesToCart();
		page.clickPipelineParametersBtn();
		assertEquals("Should have the proper pipeline name in title", "Default Parameters",
				page.getParametersModalTitle());

		// set the value for the ALternative Allele Fraction
		String value = page.getAlternativeAlleleFractionValue();
		String newValue = "10";
		page.setAlternativeAlleleFraction(newValue);
		assertEquals("Should not have the same value as the default after being changed", newValue,
				page.getAlternativeAlleleFractionValue());
		page.clickSetDefaultAlternativeAlleleFraction();
		assertEquals("Value should be reset to the default value", value, page.getAlternativeAlleleFractionValue());
	}

	@Ignore
	@Test
	public void testModifyParametersAgain() throws InterruptedException {
		addSamplesToCart();
		page.clickPipelineParametersBtn();
		assertEquals("Should have the proper pipeline name in title", "Default Parameters",
				page.getParametersModalTitle());

		// set the value for the ALternative Allele Fraction
		String newValue = "10";
		page.setAlternativeAlleleFraction(newValue);
		assertEquals("Should be set to the new value.", newValue, page.getAlternativeAlleleFractionValue());

		page.clickUseParametersButton();

		// open the dialog again and make sure that the changed values are still
		// there:
		page.clickPipelineParametersBtn();
		assertEquals("alternative allele fraction should have the same value as the new value after being changed",
				newValue, page.getAlternativeAlleleFractionValue());
	}

	@Ignore
	@Test
	public void testModifyAndSaveParameters() {
		addSamplesToCart();
		page.clickPipelineParametersBtn();
		assertEquals("Should have the proper pipeline name in title", "Default Parameters",
				page.getParametersModalTitle());

		// set the value for the ALternative Allele Fraction
		String newValue = "10";
		final String savedParametersName = "Saved parameters name.";
		page.setAlternativeAlleleFraction(newValue);
		assertEquals("Should have updated alternative allele fractiion value to new value.", newValue,
				page.getAlternativeAlleleFractionValue());
		page.clickSaveParameters();
		assertTrue("Page should have shown name for parameters field with selected parameters name.",
				page.isNameForParametersVisible());
		page.setNameForSavedParameters(savedParametersName);
		page.clickUseParametersButton();
		assertEquals("Selected parameter set should be the saved one.", savedParametersName,
				page.getSelectedParameterSet());
		// now test that we can run the pipeline
		page.clickLaunchPipelineBtn();
		assertTrue("Message should be displayed once the pipeline finished submitting",
				page.isPipelineSubmittedSuccessMessageShown());
	}

	@Test
	@Ignore
	public void testRemoteSample() throws InterruptedException{
		LoginPage.loginAsAdmin(driver());
		// add the api
		RemoteApiUtilities.addRemoteApi(driver());

		// associate a project from that api
		AssociatedProjectEditPage apEditPage = new AssociatedProjectEditPage(driver());
		apEditPage.goTo(1L);
		apEditPage.viewRemoteTab();
		apEditPage.clickAssociatedButton(1L);
		assertTrue(apEditPage.checkNotyStatus("success"));

		ProjectSamplesPage samplesPage = ProjectSamplesPage.gotToPage(driver(), 1);

		samplesPage.selectSample(0);
		samplesPage.addSelectedSamplesToCart();
		
//		samplesPage.enableRemoteProjects();
//
//		samplesPage.selectSampleByClass("remote-sample");
//		samplesPage.addSamplesToGlobalCart();
		
		PipelinesSelectionPage.goToPhylogenomicsPipeline(driver());
		
		assertTrue(page.isRemoteSampleDisplayed());

		page.clickLaunchPipelineBtn();
		assertTrue("Message should be displayed when the pipeline is submitted", page.isPipelineSubmittedMessageShown());
		assertTrue("Message should be displayed once the pipeline finished submitting",
				page.isPipelineSubmittedSuccessMessageShown());
	}

	private void addSamplesToCart() {
		LoginPage.loginAsUser(driver());
		ProjectSamplesPage samplesPage = ProjectSamplesPage.gotToPage(driver(), 1);
		samplesPage.selectSample(0);
		samplesPage.selectSample(1);
		samplesPage.addSelectedSamplesToCart();
		PipelinesSelectionPage.goToPhylogenomicsPipeline(driver());
	}
}
