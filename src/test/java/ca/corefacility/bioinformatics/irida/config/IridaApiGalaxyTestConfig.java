package ca.corefacility.bioinformatics.irida.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import ca.corefacility.bioinformatics.irida.config.workflow.IridaWorkflowsGalaxyIntegrationTestConfig;
import ca.corefacility.bioinformatics.irida.config.analysis.AnalysisExecutionServiceTestConfig;
import ca.corefacility.bioinformatics.irida.config.analysis.GalaxyExecutionTestConfig;
import ca.corefacility.bioinformatics.irida.config.data.IridaApiTestDataSourceConfig;
import ca.corefacility.bioinformatics.irida.config.pipeline.data.galaxy.NonWindowsLocalGalaxyConfig;
import ca.corefacility.bioinformatics.irida.config.pipeline.data.galaxy.WindowsLocalGalaxyConfig;
import ca.corefacility.bioinformatics.irida.config.processing.IridaApiTestMultithreadingConfig;
import ca.corefacility.bioinformatics.irida.config.services.IridaApiServicesConfig;
import ca.corefacility.bioinformatics.irida.config.workflow.IridaWorkflowsTestConfig;

import com.google.common.util.concurrent.MoreExecutors;

/**
 * Configuration for any integration tests requiring the use of Galaxy. Used to
 * make sure the configuration is the same for every test requiring Galaxy to
 * avoid duplicate Galaxy beans being created.
 * 
 *
 */
@Configuration
@Import({ GalaxyExecutionTestConfig.class, IridaApiServicesConfig.class, IridaApiTestDataSourceConfig.class, IridaApiTestMultithreadingConfig.class,
		NonWindowsLocalGalaxyConfig.class, WindowsLocalGalaxyConfig.class,
		AnalysisExecutionServiceTestConfig.class, IridaWorkflowsTestConfig.class, IridaWorkflowsGalaxyIntegrationTestConfig.class })
@Profile("test")
public class IridaApiGalaxyTestConfig {
	
	/**
	 * Sets up an {@link Unmarshaller} for workflow objects.
	 * 
	 * @return An {@link Unmarshaller} for workflow objects.
	 */
	@Bean
	public Unmarshaller workflowDescriptionUnmarshaller() {
		Jaxb2Marshaller jaxb2marshaller = new Jaxb2Marshaller();
		jaxb2marshaller.setPackagesToScan(new String[] { "ca.corefacility.bioinformatics.irida.model.workflow" });
		return jaxb2marshaller;
	}

	/**
	 * @return An ExecutorService executing code in the same thread for testing
	 *         purposes.
	 */
	@Bean
	public Executor uploadExecutor() {
		return MoreExecutors.sameThreadExecutor();
	}
}
