package ca.corefacility.bioinformatics.irida.ria.web.oauth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ca.corefacility.bioinformatics.irida.model.IridaClientDetails;
import ca.corefacility.bioinformatics.irida.repositories.specification.IridaClientDetailsSpecification;
import ca.corefacility.bioinformatics.irida.ria.utilities.Formats;
import ca.corefacility.bioinformatics.irida.ria.utilities.components.DataTable;
import ca.corefacility.bioinformatics.irida.ria.web.BaseController;
import ca.corefacility.bioinformatics.irida.service.IridaClientDetailsService;

/**
 * Controller for all {@link IridaClientDetails} related views
 * 
 */
@Controller
@RequestMapping(value = "/clients")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ClientsController extends BaseController {
	private static final Logger logger = LoggerFactory.getLogger(ClientsController.class);

	public static final String CLIENTS_PAGE = "clients/list";
	public static final String CLIENT_DETAILS_PAGE = "clients/client_details";
	public static final String ADD_CLIENT_PAGE = "clients/create";
	public static final String EDIT_CLIENT_PAGE = "clients/edit";

	private final IridaClientDetailsService clientDetailsService;
	private final MessageSource messageSource;

	private final String SORT_BY_ID = "id";
	private final List<String> SORT_COLUMNS = Lists.newArrayList(SORT_BY_ID, "clientId", "authorizedGrantTypes",
			"createdDate");
	private static final String SORT_ASCENDING = "asc";

	private final List<String> AVAILABLE_GRANTS = Lists.newArrayList("password", "authorization_code");

	private final List<Integer> AVAILABLE_TOKEN_VALIDITY = Lists.newArrayList(
			// 30 minutes
			1800,
			// 1 hour
			3600,
			// 2 hours
			7200,
			// 6 hours
			21600,
			// 12 hours
			43200,
			// 1 day
			86400,
			// 2 days
			172800,
			// 7 days
			604800);

	private final List<Integer> AVAILABLE_REFRESH_TOKEN_VALIDITY = Lists.newArrayList(
			// 7 days
			604800,
			// 1 month
			2592000,
			// 3 months
			7776000,
			// 6 months
			15552000);

	@Autowired
	public ClientsController(IridaClientDetailsService clientDetailsService, MessageSource messageSource) {
		this.clientDetailsService = clientDetailsService;
		this.messageSource = messageSource;
	}

	/**
	 * Request for the page to display a list of all clients available.
	 * 
	 * @return The name of the page.
	 */
	@RequestMapping
	public String getClientsPage() {
		return CLIENTS_PAGE;
	}

	/**
	 * Read an individual client
	 * 
	 * @param clientId
	 *            The ID of the client to display
	 * @param model
	 *            The model object for this view
	 * @return The view name of the client details page
	 */
	@RequestMapping("/{clientId}")
	public String read(@PathVariable Long clientId, Model model) {
		IridaClientDetails client = clientDetailsService.read(clientId);

		String grants = StringUtils.collectionToDelimitedString(client.getAuthorizedGrantTypes(), ", ");
		String scopes = StringUtils.collectionToDelimitedString(client.getScope(), ", ");
		String autoApproveScopes = StringUtils.collectionToDelimitedString(client.getAutoApprovableScopes(), ", ");
		model.addAttribute("client", client);

		model.addAttribute("grants", grants);
		model.addAttribute("scopes", scopes);
		model.addAttribute("autoApproveScopes", autoApproveScopes);
		int allTokensForClient = clientDetailsService.countTokensForClient(client);
		int activeTokensForClient = clientDetailsService.countActiveTokensForClient(client);
		
		model.addAttribute("activeTokens",activeTokensForClient);
		model.addAttribute("expiredTokens",allTokensForClient - activeTokensForClient);
		
		return CLIENT_DETAILS_PAGE;
	}

	/**
	 * Delete all tokens for a given {@link IridaClientDetails}
	 * 
	 * @param id
	 *            The database id of the {@link IridaClientDetails} to revoke
	 *            tokens for
	 * @return redirect back to the client page
	 */
	@RequestMapping("/revoke")
	public String revokeTokens(@RequestParam Long id) {
		IridaClientDetails read = clientDetailsService.read(id);
		clientDetailsService.revokeTokensForClient(read);
		return "redirect:/clients/" + id;
	}

	/**
	 * Get the page to edit {@link IridaClientDetails}
	 * 
	 * @param clientId
	 *            The ID of the {@link IridaClientDetails}
	 * @param model
	 *            Model for the view
	 * @return view name for editing client details
	 */
	@RequestMapping(value = "/{clientId}/edit", method = RequestMethod.GET)
	public String getEditPage(@PathVariable Long clientId, Model model) {
		IridaClientDetails client = clientDetailsService.read(clientId);

		model.addAttribute("client", client);
		// in practise our clients only have 1 grant type, adding it to model to
		// make it easier
		if (!client.getAuthorizedGrantTypes().isEmpty()) {
			model.addAttribute("selectedGrant", client.getAuthorizedGrantTypes().iterator().next());
		}
		
		Set<String> scopes = client.getScope();
		for (String scope : scopes) {
			model.addAttribute("given_scope_" + scope, true);
		}

		Set<String> autoScopes = client.getAutoApprovableScopes();
		for (String autoScope : autoScopes) {
			model.addAttribute("given_scope_auto_" + autoScope,true);
		}
		
		if(client.getAuthorizedGrantTypes().contains("refresh_token")){
			model.addAttribute("refresh", true);
		}
		
		getAddClientPage(model);

		return EDIT_CLIENT_PAGE;
	}

	/**
	 * Submit client details edit
	 * 
	 * @param clientId
	 *            the long ID of the {@link IridaClientDetails} to edit
	 * @param accessTokenValiditySeconds
	 *            The new accessTokenValiditySeconds
	 * @param authorizedGrantTypes
	 *            the new authorizedGrantTypes
	 * @param scope_read
	 *            whether to allow read scope
	 * @param scope_write
	 *            whether to allow write scope
	 * @param scope_auto_read
	 *            whether to allow automatic authorization for the read scope
	 * @param scope_auto_write
	 *            whether to allow automatic authorization for the write scope
	 * @param new_secret
	 *            whether to generate a new client secret
	 * @param model
	 *            Model for the view
	 * @param locale
	 *            Locale of the logged in user
	 * @return Redirect to the client details page if successful, the edit page
	 *         if there are errors
	 */
	@RequestMapping(value = "/{clientId}/edit", method = RequestMethod.POST)
	public String postEditClient(@PathVariable Long clientId,
			@RequestParam(required = false, defaultValue = "0") Integer accessTokenValiditySeconds,
			@RequestParam(required = false, defaultValue = "") String authorizedGrantTypes,
			@RequestParam(required = false, defaultValue = "") String scope_read,
			@RequestParam(required = false, defaultValue = "") String scope_write,
			@RequestParam(required = false, defaultValue = "") String scope_auto_read,
			@RequestParam(required = false, defaultValue = "") String scope_auto_write,
			@RequestParam(required = false, defaultValue = "") String refresh,
			@RequestParam(required = false, defaultValue = "0") Integer refreshTokenValidity,
			@RequestParam(required = false, defaultValue = "") String new_secret, Model model, Locale locale) {
		IridaClientDetails readClient = clientDetailsService.read(clientId);

		if (accessTokenValiditySeconds != 0) {
			readClient.setAccessTokenValiditySeconds(accessTokenValiditySeconds);
		}
		if (!Strings.isNullOrEmpty(authorizedGrantTypes)) {
			readClient.setAuthorizedGrantTypes(Sets.newHashSet(authorizedGrantTypes));
		}

		Set<String> scopes = new HashSet<>();
		Set<String> autoScopes = new HashSet<>();
		if (scope_write.equals("write")) {
			scopes.add("write");
			if(scope_auto_write.equals("write")) {
				autoScopes.add("write");
			}
		}
		if (scope_read.equals("read")) {
			scopes.add("read");
			if(scope_auto_read.equals("read")) {
				autoScopes.add("read");
			}
		}
		
		readClient.setScope(scopes);
		readClient.setAutoApprovableScopes(autoScopes);
		
		if (!Strings.isNullOrEmpty(new_secret)) {
			String clientSecret = generateClientSecret();;
			readClient.setClientSecret(clientSecret);
		}
		
		if (refresh.equals("refresh")) {
			readClient.getAuthorizedGrantTypes().add("refresh_token");
		} else {
			readClient.getAuthorizedGrantTypes().remove("refresh_token");
		}
		
		if(refreshTokenValidity != 0){
			readClient.setRefreshTokenValiditySeconds(refreshTokenValidity);
		}

		String response;
		try {
			clientDetailsService.update(readClient);
			response = "redirect:/clients/" + clientId;
		} catch (RuntimeException e) {
			handleCreateUpdateException(e, model, locale, scope_write, scope_read, scope_auto_write, scope_auto_read, readClient.getClientId(),
					accessTokenValiditySeconds);
			response = getEditPage(clientId, model);
		}

		return response;
	}

	/**
	 * Get the create client page
	 * 
	 * @param model
	 *            Model for the view
	 * @return The name of the create client page
	 */
	@RequestMapping(value = "/create", method = RequestMethod.GET)
	public String getAddClientPage(Model model) {
		if (!model.containsAttribute("errors")) {
			model.addAttribute("errors", new HashMap<String, String>());
		}

		model.addAttribute("available_grants", AVAILABLE_GRANTS);

		model.addAttribute("available_token_validity", AVAILABLE_TOKEN_VALIDITY);
		model.addAttribute("available_refresh_token_validity", AVAILABLE_REFRESH_TOKEN_VALIDITY);

		// set the default token validity
		if (!model.containsAttribute("given_tokenValidity")) {
			model.addAttribute("given_tokenValidity", IridaClientDetails.DEFAULT_TOKEN_VALIDITY);
		}
		
		model.addAttribute("refresh_validity", IridaClientDetails.DEFAULT_REFRESH_TOKEN_VALIDITY);

		return ADD_CLIENT_PAGE;
	}

	/**
	 * Create a new client
	 * 
	 * @param client
	 *            The client to add
	 * @param scope_read
	 *            if the client should be allowed to read from the server (value
	 *            should be "read").
	 * @param scope_write
	 *            if the client should be allowed to write to the server (value
	 *            should be "write").
	 * @param scope_auto_read
	 *            whether to allow automatic authorization for the read scope
	 * @param scope_auto_write
	 *            whether to allow automatic authorization for the write scope
	 * @param model
	 *            Model for the view
	 * @param locale
	 *            Locale of the current user session
	 * @return Redirect to the newly created client page, or back to the
	 *         creation page in case of an error.
	 */
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	public String postCreateClient(@ModelAttribute IridaClientDetails client,
			@RequestParam(required = false, defaultValue = "") String scope_read,
			@RequestParam(required = false, defaultValue = "") String scope_write,
			@RequestParam(required = false, defaultValue = "") String scope_auto_read,
			@RequestParam(required = false, defaultValue = "") String scope_auto_write,
			@RequestParam(required = false, defaultValue = "") String refresh, Model model, Locale locale) {
		client.setClientSecret(generateClientSecret());

		Set<String> autoScopes = new HashSet<>();
		Set<String> scopes = new HashSet<>();
		if (scope_write.equals("write")) {
			scopes.add("write");
			if(scope_auto_write.equals("write")) {
				autoScopes.add("write");
			}
		}

		if (scope_read.equals("read")) {
			scopes.add("read");
			if(scope_auto_read.equals("read")) {
				autoScopes.add("read");
			}
		}
		
		if(refresh.equals("refresh")){
			client.getAuthorizedGrantTypes().add("refresh_token");
		}

		client.setScope(scopes);
		client.setAutoApprovableScopes(autoScopes);
		
		

		String responsePage = null;
		try {
			IridaClientDetails create = clientDetailsService.create(client);
			responsePage = "redirect:/clients/" + create.getId();
		} catch (RuntimeException ex) {
			handleCreateUpdateException(ex, model, locale, scope_write, scope_read, scope_auto_read, scope_auto_write, client.getClientId(),
					client.getAccessTokenValiditySeconds());
			responsePage = getAddClientPage(model);
		}

		return responsePage;
	}

	/**
	 * Remove a client with the given id
	 * 
	 * @param id
	 *            The ID to remove
	 * @return redirect to the clients list
	 */
	@RequestMapping(value = "/remove", method = RequestMethod.POST)
	public String removeClient(@RequestParam Long id) {
		clientDetailsService.delete(id);

		return "redirect:/clients";
	}

	/**
	 * Ajax request page for getting a list of all clients
	 * 
	 * @param start
	 *            The start element of the page
	 * @param length
	 *            The page length
	 * @param draw
	 *            Whether to draw the table
	 * @param sortColumn
	 *            The column to sort on
	 * @param direction
	 *            The direction of the sort
	 * @param searchValue
	 *            The string search value for the table
	 * @return a Map for the table
	 */
	@RequestMapping(value = "/ajax/list", produces = MediaType.APPLICATION_JSON_VALUE)
	public @ResponseBody Map<String, Object> getAjaxClientList(
			@RequestParam(DataTable.REQUEST_PARAM_START) Integer start,
			@RequestParam(DataTable.REQUEST_PARAM_LENGTH) Integer length,
			@RequestParam(DataTable.REQUEST_PARAM_DRAW) Integer draw,
			@RequestParam(value = DataTable.REQUEST_PARAM_SORT_COLUMN, defaultValue = "0") Integer sortColumn,
			@RequestParam(value = DataTable.REQUEST_PARAM_SORT_DIRECTION, defaultValue = "asc") String direction,
			@RequestParam(DataTable.REQUEST_PARAM_SEARCH_VALUE) String searchValue) {

		String sortString;

		try {
			sortString = SORT_COLUMNS.get(sortColumn);
		} catch (IndexOutOfBoundsException ex) {
			sortString = SORT_BY_ID;
		}

		Sort.Direction sortDirection = direction.equals(SORT_ASCENDING) ? Sort.Direction.ASC : Sort.Direction.DESC;

		int pageNum = start / length;

		Page<IridaClientDetails> search = clientDetailsService.search(
				IridaClientDetailsSpecification.searchClient(searchValue), pageNum, length, sortDirection, sortString);

		List<List<String>> clientsData = new ArrayList<>();
		for (IridaClientDetails client : search) {
			// get the number of active tokens for the client
			int activeTokensForClient = clientDetailsService.countActiveTokensForClient(client);

			String grants = StringUtils.collectionToDelimitedString(client.getAuthorizedGrantTypes(), ", ");

			List<String> row = new ArrayList<>();
			row.add(client.getId().toString());
			row.add(client.getClientId());
			row.add(grants);
			row.add(Formats.DATE.format(client.getCreatedDate()));
			row.add(String.valueOf(activeTokensForClient));

			clientsData.add(row);
		}

		Map<String, Object> map = new HashMap<>();
		map.put(DataTable.RESPONSE_PARAM_DRAW, draw);
		map.put(DataTable.RESPONSE_PARAM_RECORDS_TOTAL, search.getTotalElements());
		map.put(DataTable.RESPONSE_PARAM_RECORDS_FILTERED, search.getTotalElements());

		map.put(DataTable.RESPONSE_PARAM_DATA, clientsData);
		return map;
	}

	/**
	 * Generate a temporary password for a user
	 * 
	 * @return A temporary password
	 */
	private static String generateClientSecret() {
		int PASSWORD_LENGTH = 42;
		int ALPHABET_SIZE = 26;
		int SINGLE_DIGIT_SIZE = 10;
		int RANDOM_LENGTH = PASSWORD_LENGTH - 3;

		List<Character> pwdArray = new ArrayList<>(PASSWORD_LENGTH);
		SecureRandom random = new SecureRandom();

		// 1. Create 1 random uppercase.
		pwdArray.add((char) ('A' + random.nextInt(ALPHABET_SIZE)));

		// 2. Create 1 random lowercase.
		pwdArray.add((char) ('a' + random.nextInt(ALPHABET_SIZE)));

		// 3. Create 1 random number.
		pwdArray.add((char) ('0' + random.nextInt(SINGLE_DIGIT_SIZE)));

		// 4. Create 5 random.
		int c = 'A';
		int rand = 0;
		for (int i = 0; i < RANDOM_LENGTH; i++) {
			rand = random.nextInt(3);
			switch (rand) {
			case 0:
				c = '0' + random.nextInt(SINGLE_DIGIT_SIZE);
				break;
			case 1:
				c = 'a' + random.nextInt(ALPHABET_SIZE);
				break;
			case 2:
				c = 'A' + random.nextInt(ALPHABET_SIZE);
				break;
			}
			pwdArray.add((char) c);
		}

		// 5. Shuffle.
		Collections.shuffle(pwdArray, random);

		// 6. Create string.
		Joiner joiner = Joiner.on("");
		return joiner.join(pwdArray);
	}

	/**
	 * Handle the errors that might occur when creating or updating
	 * {@link IridaClientDetails}
	 * 
	 * @param caughtException
	 *            The exception that was thrown when creating or updating
	 * @param model
	 *            Model for the view to display errors
	 * @param locale
	 *            Locale of the logged in user
	 * @param scope_write
	 *            The value entered for scope_write
	 * @param scope_read
	 *            The value entered for scope_read
	 * @param scope_auto_write
	 *            The value entered for scope_auto_write
	 * @param scope_auto_read
	 *            The value entered for scope_auto_read
	 * @param clientId
	 *            The entered client ID
	 * @param accesstokenValidity
	 *            The entered accesstokenValidity
	 * @return The number of errors that were found
	 */
	private int handleCreateUpdateException(RuntimeException caughtException, Model model, Locale locale,
			String scope_write, String scope_read, String scope_auto_write, String scope_auto_read, String clientId, Integer accesstokenValidity) {
		Map<String, Object> errors = new HashMap<>();

		try {
			throw caughtException;
		} catch (DataIntegrityViolationException ex) {
			if (ex.getMessage().contains(IridaClientDetails.CLIENT_ID_CONSTRAINT_NAME)) {
				errors.put("clientId", messageSource.getMessage("client.add.clientId.exists", null, locale));
			}
		} catch (ConstraintViolationException ex) {
			errors.putAll(getErrorsFromViolationException(ex));
		}

		if (!errors.isEmpty()) {
			model.addAttribute("errors", errors);

			logger.debug("Client Details couldn't be created or updated.");

			model.addAttribute("given_clientId", clientId);
			model.addAttribute("given_tokenValidity", accesstokenValidity);
			if (scope_write.equals("write")) {
				model.addAttribute("given_scope_write", scope_write);
			}
			if (scope_read.equals("read")) {
				model.addAttribute("given_scope_read", scope_read);
			}
			if (scope_auto_write.equals("write")) {
				model.addAttribute("given_scope_auto_write", scope_auto_write);
			}
			if (scope_auto_read.equals("read")) {
				model.addAttribute("given_scope_auto_read", scope_auto_read);
			}
		}

		return errors.size();
	}
}
