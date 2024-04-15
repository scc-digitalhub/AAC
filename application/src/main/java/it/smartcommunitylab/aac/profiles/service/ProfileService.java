/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.profiles.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import it.smartcommunitylab.aac.attributes.model.AttributeSet;
import it.smartcommunitylab.aac.attributes.service.AttributeService;
import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.profiles.extractor.AccountProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.AttributeSetProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.BasicProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.EmailProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.OpenIdProfileExtractor;
import it.smartcommunitylab.aac.profiles.extractor.UserProfileExtractor;
import it.smartcommunitylab.aac.profiles.model.AbstractProfile;
import it.smartcommunitylab.aac.users.model.User;
import it.smartcommunitylab.aac.users.model.UserDetails;
import it.smartcommunitylab.aac.users.service.UserService;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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
    private final LoadingCache<String, UserProfileExtractor> setExtractors = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.HOURS) // expires 1 hour after fetch
        .maximumSize(100)
        .build(
            new CacheLoader<String, UserProfileExtractor>() {
                @Override
                public UserProfileExtractor load(final String id) throws Exception {
                    AttributeSet set = attributeService.findAttributeSet(id);
                    if (set == null) {
                        throw new IllegalArgumentException("no attribute set matching the given identifier");
                    }

                    // build an extractor matching the set via provided name (not unique)
                    return new AttributeSetProfileExtractor(id);
                }
            }
        );

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
