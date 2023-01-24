package it.smartcommunitylab.aac.profiles.scope;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import it.smartcommunitylab.aac.claims.model.SerializableClaimDefinition;
import it.smartcommunitylab.aac.profiles.ProfileResourceAuthority;
import it.smartcommunitylab.aac.profiles.claims.AccountsProfileClaim;
import it.smartcommunitylab.aac.profiles.claims.BasicProfileClaim;
import it.smartcommunitylab.aac.profiles.claims.CustomProfileClaim;
import it.smartcommunitylab.aac.profiles.claims.EmailsProfileClaim;
import it.smartcommunitylab.aac.scope.base.AbstractInternalApiResource;

public class ProfileResource extends
        AbstractInternalApiResource<AbstractProfileScope, SerializableClaimDefinition> {

    public static final String RESOURCE_ID = "aac.profile";
    public static final String AUTHORITY = ProfileResourceAuthority.AUTHORITY;

    private final Set<String> identifiers;

    public ProfileResource(String realm, String baseUrl) {
        this(realm, baseUrl, Collections.emptyList());
    }

    public ProfileResource(String realm, String baseUrl, Collection<String> identifiers) {
        super(AUTHORITY, realm, baseUrl, RESOURCE_ID);

        // define scopes
        Set<AbstractProfileScope> scopes = new HashSet<>();
        // always add core profiles
        scopes.add(new BasicProfileScope(realm));
        scopes.add(new EmailProfileScope(realm));
        scopes.add(new AccountProfileScope(realm));

        this.identifiers = new HashSet<>(identifiers);

        if (this.identifiers != null) {
            // build custom scopes
            // note: we expect profiles to be defined
            this.identifiers.forEach(i -> scopes.add(new CustomProfileScope(i, realm)));
        }

        setScopes(scopes);

        // add claims definitions
        Set<SerializableClaimDefinition> definitions = new HashSet<>();
        // always add core profiles
        definitions.add(BasicProfileClaim.DEFINITION);
        definitions.add(EmailsProfileClaim.DEFINITION);
        definitions.add(AccountsProfileClaim.DEFINITION);

        if (this.identifiers != null) {
            // build custom definitions
            // note: we expect profiles to be defined
            this.identifiers.stream()
                    .forEach(i -> definitions.add(CustomProfileClaim.DEFINITION(i)));
        }

        setClaims(definitions);
    }

    public ProfileResource(String realm, String baseUrl, String... identifiers) {
        this(realm, baseUrl, Arrays.asList(identifiers));
    }

    @JsonIgnore
    public Set<String> getIdentifiers() {
        return identifiers;
    }

//
//    // TODO replace with keys for i18n
//    @Override
//    public String getName() {
//        return "User profiles";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Access user profiles: basic, account, custom";
//    }
}
