package it.smartcommunitylab.aac.core.provider;

import java.io.Serializable;
import java.util.Collection;

import it.smartcommunitylab.aac.model.User;

public interface UserExtendedAttributeProvider {

    public String getKey();

    public Collection<Serializable> fetchExtendedAttributes(User user, String realm);

    public void deleteExtendedAttributes(String userId, String realm);

}
