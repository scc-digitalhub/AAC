package it.smartcommunitylab.aac.core.base;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.store.AttributeStore;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserAuthenticatedPrincipal;
import it.smartcommunitylab.aac.core.provider.IdentityAttributeProvider;

public abstract class AbstractIdentityAttributeProvider<P extends UserAuthenticatedPrincipal, U extends UserAccount>
        extends AbstractProvider<UserAttributes>
        implements IdentityAttributeProvider<P, U> {

    // services
    protected AttributeStore attributeStore;

    protected AbstractIdentityAttributeProvider(String authority, String provider, String realm) {
        super(authority, provider, realm);
    }

    public void setAttributeStore(AttributeStore attributeStore) {
        this.attributeStore = attributeStore;
    }

    @Override
    public final String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    @Override
    public Collection<UserAttributes> convertPrincipalAttributes(P principal, U account) {
        String id = principal.getPrincipalId();
        Map<String, Serializable> attributes = principal.getAttributes();

        // store all attributes from principal
        Set<Entry<String, Serializable>> storeAttributes = new HashSet<>();
        for (Entry<String, Serializable> e : attributes.entrySet()) {
            Entry<String, Serializable> es = new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue());
            storeAttributes.add(es);
        }

        if (attributeStore != null) {
            // store attributes linked to id
            attributeStore.setAttributes(id, storeAttributes);
        }

        // call extract to transform
        return extractUserAttributes(account, attributes);

    }

    @Override
    public Collection<UserAttributes> getAccountAttributes(U account) {
        // account can't be null
        if (account == null) {
            throw new IllegalArgumentException();
        }

        String id = account.getAccountId();
        Map<String, Serializable> principalAttributes = Collections.emptyMap();

        if (attributeStore != null) {
            // read from store
            principalAttributes = attributeStore.findAttributes(id);
        }

        // call extract to transform
        return extractUserAttributes(account, principalAttributes);
    }

    @Override
    public void deleteAccountAttributes(String subjectId) {
        if (attributeStore != null) {
            // cleanup from store
            attributeStore.deleteAttributes(subjectId);
        }
    }

    /*
     * Extract operation to be implemented by subclasses
     */
    protected abstract List<UserAttributes> extractUserAttributes(U account,
            Map<String, Serializable> principalAttributes);

}
