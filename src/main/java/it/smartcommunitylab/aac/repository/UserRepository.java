/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */
package it.smartcommunitylab.aac.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.model.User;
/**
 * Persistent repository of {@link User} entities
 * @author raman
 *
 */
@Repository
public interface UserRepository extends CustomJpaRepository<User, Long>, UserRepositoryCustom {

	@Query("select u from User u where u.name=?1")
	User findByName(String text);	
	
	@Query("select u from User u where u.fullName like ?1")
	List<User> findByFullNameLike(String text);
	
	@Query("select distinct u from User u left join u.attributeEntities a where a.authority.name=?1 and a.key=?2 and a.value=?3")
	List<User> findByAttributeEntities(String authority, String attribute, String value);

	User findByUsername(String username);

	@Query("select distinct u from User u left join u.roles r where r.role=?1 and (r.context=?2 or ?2 is null and r.context is null) and (r.space = ?3 or ?3 is null and r.space is null)")
	List<User> findByFullRole(String role, String context, String space, Pageable pageable);

	@Query("select distinct u from User u left join u.roles r where r.role=?1 and (r.context=?2 or ?2 is null and r.context is null)")
	List<User> findByRole(String role, String context, Pageable pageable);

	@Query("select distinct u from User u left join u.roles r where (r.context=?1 or ?1 is null and r.context is null) and (r.space = ?2 or ?2 is null and r.space is null)")
	List<User> findByRoleContext(String context, String space, Pageable pageable);

	@Query("select distinct u from User u left join u.roles r where (r.context=?1 or ?1 is null and r.context is null) and (r.space = ?2 or ?2 is null and r.space is null) and (r.role = ?3 or ?3 is null)")
	List<User> findByRoleContextAndRole(String context, String space, String role, Pageable pageable);
	
	@Query("select distinct u from User u left join u.roles r where ((r.context is null and r.space =?1) or (r.context = ?1) or (r.context LIKE CONCAT(?1,'/%')) or (r.context = ?3 and r.space = ?4)) and (r.role = ?2 or ?2 is null)")
	List<User> findByRoleContextAndRoleNested(String canonical, String role, String context, String space, Pageable pageable);
}
