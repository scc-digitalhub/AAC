package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.UserAttributes;

public interface AttributeService extends AttributeProvider {

    /*
     * Attribute management
     */

    public Collection<UserAttributes> putAttributes(String subjectId, Collection<AttributeSet> attributes);

    public void deleteAttributes(String subjectId, String setId);

}
