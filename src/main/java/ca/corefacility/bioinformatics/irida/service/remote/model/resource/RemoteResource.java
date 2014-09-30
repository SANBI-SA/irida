package ca.corefacility.bioinformatics.irida.service.remote.model.resource;

import java.util.List;

/**
 * Methods that must be implemented by resources read from a remote Irida API
 * 
 * @author Thomas Matthews <thomas.matthews@phac-aspc.gc.ca>
 */
public interface RemoteResource {
	/**
	 * Get the numeric identifier for this resource
	 * 
	 * @return
	 */
	public String getIdentifier();

	/**
	 * Set the numeric identifier for this resource
	 * 
	 * @param identifier
	 */
	public void setIdentifier(String identifier);

	/**
	 * Get the objects this resource links to
	 * 
	 * @return
	 */
	public List<RESTLink> getLinks();

	/**
	 * Set the objects this resource links to
	 * 
	 * @param links
	 */
	public void setLinks(List<RESTLink> links);

}
