package it.smartcommunitylab.aac.profiles.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import it.smartcommunitylab.aac.attributes.persistence.AttributeEntity;
import it.smartcommunitylab.aac.attributes.persistence.AttributeSetEntity;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.UserDetails;
import it.smartcommunitylab.aac.core.service.UserService;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.profiles.extractor.AccountProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.AttributeSetProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.AttributesProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.BasicProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.EmailProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.OpenIdProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.UserProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;

@Service
public class ProfileService {

    private final UserService userService;
    private final AttributeService attributeService;

    // internal extractors
    private BasicProfileExtractor basicProfileExtractor;
    private OpenIdProfileExtractor openidProfileExtractor;
    private AccountProfileExtractor accountProfileExtractor;
    private EmailProfileExtractor emailProfileExtractor;

    private Map<String, UserProfileExtractor> systemExtractors;

    // TODO add custom extractors
    private Map<String, UserProfileExtractor> customExtractors;

//    // loading cache for set extractors
//    private final LoadingCache<String, UserProfileExtractor> mappingExtractors = CacheBuilder.newBuilder()
//            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
//            .maximumSize(100)
//            .build(new CacheLoader<String, UserProfileExtractor>() {
//                @Override
//                public UserProfileExtractor load(final String id) throws Exception {
//                    AttributeSetEntity set = attributeService.findAttributeSet(id);
//                    if (set == null) {
//                        throw new IllegalArgumentException("no attribute set matching the given identifier");
//
//                    }
//                    List<AttributeEntity> attributes = attributeService.listAttributes(id);
//
//                    // build a attribute extractor with keys matching the given set
//                    Map<String, Collection<String>> mapping = new HashMap<>();
//                    attributes.forEach(a -> {
//                        mapping.put(a.getKey(), Collections.singleton(id));
//                    });
//
//                    return new AttributesProfileExtractor(id, mapping);
//                }
//            });

    // loading cache for set dump extractors
    private final LoadingCache<String, UserProfileExtractor> setExtractors = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
            .maximumSize(100)
            .build(new CacheLoader<String, UserProfileExtractor>() {
                @Override
                public UserProfileExtractor load(final String id) throws Exception {
                    AttributeSetEntity set = attributeService.findAttributeSet(id);
                    if (set == null) {
                        throw new IllegalArgumentException("no attribute set matching the given identifier");

                    }

                    // build an extractor matching the set via provided name (not unique)
                    return new AttributeSetProfileExtractor(id);
                }
            });

    public ProfileService(UserService userService, AttributeService attributeService) {
        Assert.notNull(userService, "user service is required");
        Assert.notNull(attributeService, "attribute service is required");
        this.userService = userService;
        this.attributeService = attributeService;

        // build extractors
        this.systemExtractors = new HashMap<>();
        this.basicProfileExtractor = new BasicProfileExtractor();
        this.openidProfileExtractor = new OpenIdProfileExtractor();
        this.accountProfileExtractor = new AccountProfileExtractor();
        this.emailProfileExtractor = new EmailProfileExtractor();

        systemExtractors.put(basicProfileExtractor.getIdentifier(), basicProfileExtractor);
        systemExtractors.put(openidProfileExtractor.getIdentifier(), openidProfileExtractor);
        systemExtractors.put(accountProfileExtractor.getIdentifier(), accountProfileExtractor);
        systemExtractors.put(emailProfileExtractor.getIdentifier(), emailProfileExtractor);

        customExtractors = Collections.emptyMap();

    }

    /*
     * Profiles from userDetails - shortcuts
     */

    public AbstractProfile getProfile(UserDetails userDetails, String identifier) throws InvalidDefinitionException {
        return getProfile(userService.getUser(userDetails), identifier);
    }

    public Collection<? extends AbstractProfile> getProfiles(UserDetails userDetails, String identifier)
            throws InvalidDefinitionException {
        return getProfiles(userService.getUser(userDetails), identifier);
    }

    /*
     * Profiles from user
     */

    private AbstractProfile getProfile(User user, String identifier) throws InvalidDefinitionException {
        UserProfileExtractor ext = getExtractor(identifier);

        AbstractProfile profile = ext.extractUserProfile(user);

        if (profile == null) {
            throw new InvalidDefinitionException("invalid profile");
        }

        return profile;
    }

    private Collection<? extends AbstractProfile> getProfiles(User user, String identifier)
            throws InvalidDefinitionException {
        UserProfileExtractor ext = getExtractor(identifier);

        return ext.extractUserProfiles(user);

    }

    /*
     * Profiles from db
     */

    public AbstractProfile getProfile(String realm, String subjectId, String identifier)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getProfile(user, identifier);
    }

    public Collection<? extends AbstractProfile> getProfiles(String realm, String subjectId, String identifier)
            throws NoSuchUserException, InvalidDefinitionException {
        User user = userService.getUser(subjectId, realm);
        return getProfiles(user, identifier);
    }

    /*
     * Extractors
     */

    public UserProfileExtractor getExtractor(String identifier) throws InvalidDefinitionException {
        UserProfileExtractor ext = systemExtractors.get(identifier);

        // fetch custom extractor for profile
        if (ext == null) {
            ext = customExtractors.get(identifier);
        }

        // try to build a default extractor for an attribute set
        if (ext == null) {
            try {
                // TODO use either attribute mapping or script mapping, avoid full set export
                ext = setExtractors.get(identifier);
            } catch (Exception e) {
                ext = null;
            }
        }

//        // try to build a default extractor for a generic set
//        if (ext == null) {
//            try {
//                ext = setExtractors.get(identifier);
//            } catch (Exception e) {
//                ext = null;
//            }
//        }

        if (ext == null) {
            throw new InvalidDefinitionException("no such profile defined");
        }

        return ext;

    }

}
