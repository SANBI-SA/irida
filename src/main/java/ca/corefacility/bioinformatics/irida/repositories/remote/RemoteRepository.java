package ca.corefacility.bioinformatics.irida.repositories.remote;

import java.util.List;

import ca.corefacility.bioinformatics.irida.exceptions.IridaOAuthException;
import ca.corefacility.bioinformatics.irida.model.IridaResourceSupport;
import ca.corefacility.bioinformatics.irida.model.RemoteAPI;

public interface RemoteRepository<Type extends IridaResourceSupport> {
	/**
	 * Read an individual resource
	 * 
	 * @param uri
	 *            The URI of the resource to read
	 * @param remoteAPI
	 *            the API to read from
	 * @return An object of Type
	 */
	public Type read(String uri, RemoteAPI remoteAPI);

	/**
	 * List the resources available from this service
	 * 
	 * @param remoteAPI
	 *            The API to read from
	 * @param uri
	 *            the URI of the resource to list.
	 * @return A {@code List<Type>} of the resources available
	 */
	public List<Type> list(String uri, RemoteAPI remoteAPI);

	/**
	 * Get the status of the remote service
	 * 
	 * @param remoteAPI
	 *            The API to check status for
	 * @return true if the service is active
	 */
	public boolean getServiceStatus(RemoteAPI remoteAPI) throws IridaOAuthException;
}
