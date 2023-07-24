package it.smartcommunitylab.aac.core.provider;

import it.smartcommunitylab.aac.core.model.AttributeSet;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import java.util.Collection;

public interface AttributeService<M extends ConfigMap, C extends AttributeProviderConfig<M>>
    extends AttributeProvider<M, C> {
    /*
     * Attribute management
     */

    public Collection<UserAttributes> putUserAttributes(String subjectId, Collection<AttributeSet> attributes);

    public UserAttributes putUserAttributes(String subjectId, String setId, AttributeSet attributes);
}
