package ca.corefacility.bioinformatics.irida.web.spring.view;

import ca.corefacility.bioinformatics.irida.web.assembler.resource.sequencefile.SequenceFileResource;
import ca.corefacility.bioinformatics.irida.web.controller.api.GenericController;
import com.google.common.net.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Write out FASTQ formatted sequence files to the client.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
public class FastqView extends AbstractView {
    public static final String DEFAULT_CONTENT_TYPE = "application/fastq";
    private static final Logger logger = LoggerFactory.getLogger(FastqView.class);

    /**
     * Default constructor
     */
    public FastqView() {
        setContentType(DEFAULT_CONTENT_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SequenceFileResource sfr = (SequenceFileResource) model.get(GenericController.RESOURCE_NAME);
        Path fileContent = sfr.getPath();
        String filename = fileContent.getFileName().toString();
        logger.trace("Sending file to client [" + filename + "]");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.setHeader(HttpHeaders.CONTENT_TYPE, DEFAULT_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(Files.size(fileContent)));
        OutputStream os = response.getOutputStream();
        Files.copy(fileContent, os);
        os.flush();
        os.close();
    }
}