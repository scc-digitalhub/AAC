package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import it.smartcommunitylab.aac.attributes.AccountAttributesSet;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.AccountProfile;
import it.smartcommunitylab.aac.profiles.model.BasicProfile;

public class AccountProfileExtractor extends AbstractUserProfileExtractor {

    @Override
    public String getIdentifier() {
        return AccountProfile.IDENTIFIER;
    }

    
    @Override
    public AccountProfile extractUserProfile(User user)
            throws InvalidDefinitionException {

        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to select primary identity
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        AccountProfile profile = extract(id.getAccount(), id.getAttributes());

        return profile;
    }

    @Override
    public AccountProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public Collection<AccountProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());
    }

    private AccountProfile extract(UserAccount account, Collection<UserAttributes> attributes) {
        AccountProfile profile = new AccountProfile();
        profile.setAuthority(account.getAuthority());
        profile.setProvider(account.getProvider());
        profile.setRealm(account.getRealm());

        profile.setUserId(account.getUserId());
        profile.setUsername(account.getUsername());

        // look for accountProfile extra attributes
        Map<String, String> extra = new HashMap<>();
        Optional<UserAttributes> attrs = attributes.stream()
                .filter(a -> a.getIdentifier().equals(AccountAttributesSet.IDENTIFIER)).findFirst();
        if (attrs.isPresent()) {
            // add every extra property
            attrs.get().getAttributes().forEach(attr -> {
                if (!AccountAttributesSet.USER_ID.equals(attr.getKey()) &&
                        !AccountAttributesSet.USERNAME.equals(attr.getKey()) &&
                        attr.getType() == AttributeType.STRING) {
                    extra.put(attr.getKey(), attr.getValue().toString());
                }
            });
        }

        profile.setAttributes(extra);

        return profile;

    }

}
