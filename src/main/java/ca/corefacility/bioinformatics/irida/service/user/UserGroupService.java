package ca.corefacility.bioinformatics.irida.service.user;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;

import ca.corefacility.bioinformatics.irida.exceptions.UserGroupWithoutOwnerException;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.model.user.group.UserGroup;
import ca.corefacility.bioinformatics.irida.model.user.group.UserGroupJoin;
import ca.corefacility.bioinformatics.irida.model.user.group.UserGroupJoin.UserGroupRole;
import ca.corefacility.bioinformatics.irida.model.user.group.UserGroupProjectJoin;
import ca.corefacility.bioinformatics.irida.service.CRUDService;

/**
 * Service for working with {@link UserGroup}s.
 * 
 */
public interface UserGroupService extends CRUDService<Long, UserGroup> {

	/**
	 * Get all of the users in the group.
	 * 
	 * @param userGroup
	 *            the group to get users for.
	 * @return the users in the group.
	 */
	public Collection<UserGroupJoin> getUsersForGroup(final UserGroup userGroup);

	/**
	 * Add a user to the group with the specified role.
	 * 
	 * @param user
	 *            the user to add to the group
	 * @param userGroup
	 *            the group to add the user to
	 * @param role
	 *            the role that the user should have in the group
	 * @return the relationship created between the user and group.
	 */
	public UserGroupJoin addUserToGroup(final User user, final UserGroup userGroup, final UserGroupRole role);

	/**
	 * Change the role for the {@link User} in the {@link UserGroup}.
	 * 
	 * @param user
	 *            the user to change
	 * @param userGroup
	 *            the group to change
	 * @param role
	 *            the new role
	 * @return the modified relationship
	 * @throws UserGroupWithoutOwnerException
	 *             when the user group doesn't have an owner as a result of the
	 *             role change.
	 */
	public UserGroupJoin changeUserGroupRole(final User user, final UserGroup userGroup, final UserGroupRole role)
			throws UserGroupWithoutOwnerException;

	/**
	 * Remove the {@link User} from the {@link UserGroup}.
	 * 
	 * @param user
	 *            the user to remove
	 * @param userGroup
	 *            the group from which to remove the user.
	 * @throws UserGroupWithoutOwnerException
	 *             when the user group doesn't have an owner as a result of the
	 *             removal.
	 */
	public void removeUserFromGroup(final User user, final UserGroup userGroup) throws UserGroupWithoutOwnerException;

	/**
	 * Filter the list of users in the {@link UserGroup} by username.
	 * 
	 * @param username
	 *            the username string to filter on
	 * @param userGroup
	 *            the user group to filter for
	 * @param page
	 *            the current page
	 * @param size
	 *            the size of the page
	 * @param order
	 *            the order of sorting
	 * @param sortProperties
	 *            the properties to sort on
	 * @return a page of {@link UserGroupJoin}.
	 */
	public Page<UserGroupJoin> filterUsersByUsername(final String username, final UserGroup userGroup, int page,
			int size, Direction order, String... sortProperties);

	/**
	 * Get the set of {@link User} that are not currently in the
	 * {@link UserGroup}.
	 * 
	 * @param userGroup
	 *            the group to get the set of non-members
	 * @return the set of users not in the group.
	 */
	public Collection<User> getUsersNotInGroup(final UserGroup userGroup);

	/**
	 * Get a page of {@link UserGroupProjectJoin} for a specific {@link Project}
	 * .
	 * 
	 * @param searchName
	 *            the name to search with.
	 * @param project
	 *            the project
	 * @param page
	 *            the current page
	 * @param size
	 *            the size of the page
	 * @param order
	 *            the order
	 * @param sortProperties
	 *            the properties to sort on
	 * @return a page of {@link UserGroupProjectJoin}.
	 */
	public Page<UserGroupProjectJoin> getUserGroupsForProject(final String searchName, final Project project, int page,
			int size, Direction order, String... sortProperties);

	/**
	 * Get a collection of {@link UserGroup} that aren't already on a
	 * {@link Project}.
	 * 
	 * @param project
	 *            the project
	 * @param filter
	 *            the name to filter on
	 * @return the groups not already on the project.
	 */
	public List<UserGroup> getUserGroupsNotOnProject(final Project project, final String filter);
}
