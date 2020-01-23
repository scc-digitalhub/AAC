/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.repository;

import it.smartcommunitylab.aac.model.Registration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author raman
 *
 */
@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long>{

	@Query("select r from Registration r where r.email=?1")
	Registration findByEmail(String email);
	
	//TODO check userId is not UNIQUE in db
    @Query("select r from Registration r where r.userId=?1")
    Registration findByUserId(String userId);	
	
	@Query("select r from Registration r where r.confirmationKey=?1")
	Registration findByConfirmationKey(String confirmationKey);
	
	
}
