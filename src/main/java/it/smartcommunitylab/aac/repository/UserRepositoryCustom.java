package it.smartcommunitylab.aac.repository;

import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.User;

import java.util.List;

/**
 * extension of the repository interface to perform custom user search
 * @author raman
 *
 */
public interface UserRepositoryCustom {

	List<User> getUsersByAttributes(List<Attribute> list);

}
