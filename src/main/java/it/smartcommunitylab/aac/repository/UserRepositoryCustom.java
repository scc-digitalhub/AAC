package it.smartcommunitylab.aac.repository;

import java.util.List;

import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.User;

/**
 * extension of the repository interface to perform custom user search
 * @author raman
 *
 */
public interface UserRepositoryCustom {

	List<User> getUsersByAttributes(List<Attribute> list);
	
    public void insertAsNew(Long id, String name);
}
