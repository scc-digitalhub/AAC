package it.smartcommunitylab.aac.apimanager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.Config.ROLE_SCOPE;
import it.smartcommunitylab.aac.common.Utils;
import it.smartcommunitylab.aac.model.Role;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.repository.UserRepository;
import it.smartcommunitylab.aac.wso2.model.DataList;
import it.smartcommunitylab.aac.wso2.model.RoleModel;
import it.smartcommunitylab.aac.wso2.model.Subscription;

@Component
@Transactional
public class APIRoleManager {

	@Autowired
	private UserRepository userRepository;	
	
	public void fillRoles(DataList<Subscription> subs, String domain) {
		for (Subscription sub: subs.getList()) {
			String subscriber = sub.getSubscriber();
			String info[] = Utils.extractInfoFromTenant(subscriber);
			final String name = info[0];
			
			List<User> users = userRepository.findByAttributeEntities("internal", "email", name);
			if (users == null || users.size() == 0) continue;
			
			User user = users.get(0);
			
			Set<Role> userRoles = user.getRoles();
			List<String> roleNames = userRoles.stream().filter(x -> domain.equals(x.getContext()) && ROLE_SCOPE.application.equals(x.getScope())).map(r -> r.getRole()).collect(Collectors.toList());
			sub.setRoles(roleNames);
		}
	}	
	
	public List<String> updateLocalRoles(RoleModel roleModel, String domain) {
		String info[] = Utils.extractInfoFromTenant(roleModel.getUser());
		
		final String name = info[0];
		
		List<User> users = userRepository.findByAttributeEntities("internal", "email", name);
		User user = users.get(0);

		Set<Role> userRoles = new HashSet<Role>(user.getRoles());

		if (roleModel.getRemoveRoles() != null) {
			for (String role : roleModel.getRemoveRoles()) {
				Role r = new Role(ROLE_SCOPE.application, role, domain);
				userRoles.remove(r);
			}
		}
		if (roleModel.getAddRoles() != null) {
			for (String role : roleModel.getAddRoles()) {
				Role r = new Role(ROLE_SCOPE.application, role, domain);
				userRoles.add(r);
			}
		}
		user.getRoles().clear();
		user.getRoles().addAll(userRoles);

		userRepository.save(user);
		
		return userRoles.stream().filter(x -> domain.equals(x.getContext()) && ROLE_SCOPE.application.equals(x.getScope())).map(r -> r.getRole()).collect(Collectors.toList());
	}	
	
}
