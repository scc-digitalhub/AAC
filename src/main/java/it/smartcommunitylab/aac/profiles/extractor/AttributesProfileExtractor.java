package it.smartcommunitylab.aac.profiles.extractor;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.core.model.Attribute;
import it.smartcommunitylab.aac.core.model.UserAccount;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.model.CustomProfile;

public class AttributesProfileExtractor extends AbstractUserProfileExtractor {

    // profile identifier
    private final String identifier;

    // attributes mapping
    private final Map<String, Collection<String>> mapping;

    public AttributesProfileExtractor(String identifier, Map<String, Collection<String>> mapping) {
        Assert.hasText(identifier, "identifier can not be null or empty");
        Assert.notNull(mapping, "attributes mapping can not be null");
        this.identifier = identifier;
        this.mapping = Collections.unmodifiableMap(mapping);
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public CustomProfile extractUserProfile(UserIdentity identity) throws InvalidDefinitionException {
        if (identity == null) {
            return null;
        }

        return extract(identity.getAccount(), identity.getAttributes());
    }

    @Override
    public CustomProfile extractUserProfile(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return null;
        }

        // TODO decide how to merge identities into a single profile
        // for now get first identity, should be last logged in
        UserIdentity id = identities.iterator().next();
        CustomProfile profile = extract(id.getAccount(), id.getAttributes());
        return profile;
    }

    @Override
    public Collection<? extends CustomProfile> extractUserProfiles(User user) throws InvalidDefinitionException {
        // fetch identities
        Collection<UserIdentity> identities = user.getIdentities();

        if (identities.isEmpty()) {
            return Collections.emptyList();
        }

        return identities.stream().map(id -> extract(id.getAccount(), id.getAttributes())).collect(Collectors.toList());

    }

    private CustomProfile extract(UserAccount account, Collection<UserAttributes> attributes) {

        CustomProfile profile = new CustomProfile(identifier);

        // from account we can get only basic
        // TODO evaluate dropping username in custom profiles
        if (mapping.containsKey("username")) {
            profile.addAttribute("username", account.getUsername());
        }
        if (mapping.containsKey("authority")) {
            profile.addAttribute("authority", account.getAuthority());
        }
        if (mapping.containsKey("provider")) {
            profile.addAttribute("provider", account.getProvider());
        }
        if (mapping.containsKey("realm")) {
            profile.addAttribute("realm", account.getRealm());
        }

        List<String> reserved = Arrays.asList(RESERVED);

        // from attributes fetch mapped props
        for (Map.Entry<String, Collection<String>> entry : mapping.entrySet()) {
            String key = entry.getKey();
            if (!reserved.contains(key)) {
                Collection<String> sets = entry.getValue();
                Attribute attr = this.getAttribute(attributes, key, sets);
                if (attr != null) {
                    profile.addAttribute(key, attr.getValue());
                }
            }
        }

        return profile;
    }

    private static final String[] RESERVED = { "username", "provider", "authority", "realm" };

}
