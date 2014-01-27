package ca.corefacility.bioinformatics.irida.pipeline.data.galaxy.impl.integration;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import ca.corefacility.bioinformatics.irida.config.IridaApiServicesConfig;
import ca.corefacility.bioinformatics.irida.config.data.IridaApiTestDataSourceConfig;
import ca.corefacility.bioinformatics.irida.config.pipeline.data.galaxy.LocalGalaxyConfig;
import ca.corefacility.bioinformatics.irida.config.processing.IridaApiTestMultithreadingConfig;
import ca.corefacility.bioinformatics.irida.exceptions.UploadException;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyAccountEmail;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxyObjectName;
import ca.corefacility.bioinformatics.irida.model.upload.galaxy.GalaxySample;
import ca.corefacility.bioinformatics.irida.pipeline.data.galaxy.impl.GalaxyUploadResult;
import ca.corefacility.bioinformatics.irida.pipeline.data.galaxy.impl.GalaxyUploader;

import com.github.springtestdbunit.DbUnitTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { IridaApiServicesConfig.class,
		IridaApiTestDataSourceConfig.class, IridaApiTestMultithreadingConfig.class, LocalGalaxyConfig.class})
@ActiveProfiles("test")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, DbUnitTestExecutionListener.class })
public class GalaxyUploaderIT
{
	@Autowired
	private LocalGalaxy localGalaxy;
	
	@Autowired
	private GalaxyUploader galaxyUploader;
	
	private List<Path> dataFilesSingle;
	
	@Before
	public void setup() throws URISyntaxException
	{		
	    setupDataFiles();
	}
	
	private void setupDataFiles() throws URISyntaxException
	{
		Path dataFile1 = Paths.get(GalaxyUploaderIT.class.getResource("testData1.fastq").toURI());
		
		dataFilesSingle = new ArrayList<Path>();
		dataFilesSingle.add(dataFile1);
	}
	
	@Test(expected=UploadException.class)
	public void testNoGalaxyConnectionUpload() throws ConstraintViolationException, UploadException
	{
		GalaxyUploader unconnectedGalaxyUploader = new GalaxyUploader();
		
		GalaxyObjectName libraryName = new GalaxyObjectName("GalaxyUploader_testNoGalaxyConnection");
		
		List<GalaxySample> samples = new ArrayList<GalaxySample>();
		GalaxySample galaxySample1 = new GalaxySample(new GalaxyObjectName("testData1"), dataFilesSingle);
		samples.add(galaxySample1);
		
		unconnectedGalaxyUploader.uploadSamples(samples, libraryName, localGalaxy.getAdminName());
	}
	
	@Test
	public void testCheckGalaxyConnection() throws ConstraintViolationException, UploadException
	{
		GalaxyUploader unconnectedGalaxyUploader = new GalaxyUploader();

		assertTrue(galaxyUploader.isConnected());
		assertFalse(unconnectedGalaxyUploader.isConnected());
	}
	
	@Test
	public void testUploadSamples() throws URISyntaxException, MalformedURLException, ConstraintViolationException, UploadException
	{
		GalaxyObjectName libraryName = new GalaxyObjectName("GalaxyUploader_testUploadSamples");
		String localGalaxyURL = localGalaxy.getGalaxyURL().substring(0,localGalaxy.getGalaxyURL().length()-1); // remove trailing '/'
		
		List<GalaxySample> samples = new ArrayList<GalaxySample>();
		GalaxySample galaxySample1 = new GalaxySample(new GalaxyObjectName("testData1"), dataFilesSingle);
		samples.add(galaxySample1);
		
		GalaxyUploadResult actualUploadResult =
				galaxyUploader.uploadSamples(samples, libraryName, localGalaxy.getAdminName());
		assertNotNull(actualUploadResult);
		assertEquals(libraryName.getName(), actualUploadResult.getLibraryName());
		assertEquals(new URL(localGalaxyURL + "/library"), actualUploadResult.getDataLocation());
	}
	
	@Test(expected=ConstraintViolationException.class)
	public void testUploadSampleInvalidUserName() throws URISyntaxException, ConstraintViolationException, UploadException
	{	
		GalaxyObjectName libraryName = new GalaxyObjectName("testUploadSampleInvalidUserName");
		GalaxyAccountEmail userEmail = new GalaxyAccountEmail("invalid_user");
		GalaxySample galaxySample = new GalaxySample(new GalaxyObjectName("testData"), dataFilesSingle);
		List<GalaxySample> samples = new ArrayList<GalaxySample>();
		samples.add(galaxySample);
		
		galaxyUploader.uploadSamples(samples, libraryName, userEmail);
	}
	
	@Test(expected=ConstraintViolationException.class)
	public void testUploadSampleInvalidSampleName() throws URISyntaxException, ConstraintViolationException, UploadException
	{	
		GalaxyObjectName libraryName = new GalaxyObjectName("testUploadSampleInvalidSampleName");
		GalaxySample galaxySample = new GalaxySample(new GalaxyObjectName("<invalidSample>"), dataFilesSingle);
		List<GalaxySample> samples = new ArrayList<GalaxySample>();
		samples.add(galaxySample);
		
		galaxyUploader.uploadSamples(samples, libraryName, localGalaxy.getUser1Name());
	}
	
	@Test(expected=ConstraintViolationException.class)
	public void testUploadSampleInvalidLibraryName() throws URISyntaxException, ConstraintViolationException, UploadException
	{	
		GalaxyObjectName libraryName = new GalaxyObjectName("<invalidLibrary>");
		GalaxySample galaxySample = new GalaxySample(new GalaxyObjectName("testData"), dataFilesSingle);
		List<GalaxySample> samples = new ArrayList<GalaxySample>();
		samples.add(galaxySample);
		
		galaxyUploader.uploadSamples(samples, libraryName, localGalaxy.getUser1Name());
	}
}
