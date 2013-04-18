/*
 * Copyright 2013 Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.corefacility.bioinformatics.irida.repositories;

import ca.corefacility.bioinformatics.irida.exceptions.user.UserNotFoundException;
import ca.corefacility.bioinformatics.irida.model.User;
import ca.corefacility.bioinformatics.irida.model.roles.impl.Identifier;

/**
 * Specialized repository for {@link User}.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
public interface UserRepository extends CRUDRepository<Identifier, User> {

    /**
     * Get a user from the database with the supplied username.
     *
     * @param username the user's username.
     * @return the user corresponding to the username.
     * @throws UserNotFoundException If no user can be found with the supplied
     * username.
     */
    public User getUserByUsername(String username) throws UserNotFoundException;
}