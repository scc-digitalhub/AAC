package it.smartcommunitylab.aac.internal.provider;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.attributes.AttributeService;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.core.base.AbstractProvider;
import it.smartcommunitylab.aac.core.model.UserAttributes;
import it.smartcommunitylab.aac.core.model.UserIdentity;
import it.smartcommunitylab.aac.core.persistence.AttributeEntity;
import it.smartcommunitylab.aac.core.provider.AttributeProvider;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;
import it.smartcommunitylab.aac.internal.persistence.InternalUserAccountRepository;

/*
 * Internal implementation reads from account repository and attribute store
 * 
 * TODO extension: DB based via AccountService to support external account repositories
 * DAOAttributeProvider(AttributeService) where service is JDBCDao with custom queries
 * merging attributes from model and store should be done in a custom internalattrservice
 */
public class InternalAttributeProvider extends AbstractProvider implements AttributeProvider {

    private final InternalUserAccountRepository accountRepository;
    private final AttributeService attributeService;

    public InternalAttributeProvider(String providerId, InternalUserAccountRepository accountRepository,
            AttributeService attributeService, String realm) {
        super(SystemKeys.AUTHORITY_INTERNAL, providerId, realm);

        Assert.notNull(accountRepository, "accountRepository is mandatory");
//        Assert.notNull(attributeService, "attribute service is mandatory");

        this.accountRepository = accountRepository;
        this.attributeService = attributeService;
    }

    @Override
    public String getProvider() {
        // we use realm also as providerId
        // for internal service we can not have multiple providers per realm
        return getRealm();
    }

//    @Override
//    public UserAttributes getAttributes(String userId) throws NoSuchUserException {
//        // get internal id
//        Long id = Long.parseLong(parseResourceId(userId));
//        List<AttributeEntity> attributes = attributeService.findAttributes(getAuthority(), getProvider(),
//                Long.toString(id));
//
//        // map from account
//        Set<Map.Entry<String, String>> attrs = new HashSet<>();
//
//        // we need to fetch the account
//        InternalUserAccount account = accountRepository.findOne(id);
//        if (account == null) {
//            throw new NoSuchUserException();
//        }
//
//        // static mapping
//        attrs.add(new AbstractMap.SimpleEntry<>("email", account.getEmail()));
//        attrs.add(new AbstractMap.SimpleEntry<>("name", account.getName()));
//        attrs.add(new AbstractMap.SimpleEntry<>("surname", account.getSurname()));
//        attrs.add(new AbstractMap.SimpleEntry<>("username", account.getUsername()));
//
//        // fetch additional
//        attrs.addAll(attributes.stream()
//                .filter(a -> StringUtils.hasText(a.getValue()))
//                .map(a -> new AbstractMap.SimpleEntry<>(a.getKey(), a.getValue()))
//                .collect(Collectors.toSet()));
//
//        return new BaseAttributesImpl(
//                getAuthority(), getProvider(),
//                userId,
//                attrs);
//
//    }
//
//    @Override
//    public Collection<AttributeSet> listAttributeSets() {
//        return Stream.of("email", "profile")
//                .map(s -> new AttributeSet(getAuthority(), getProvider(), s, listAttributes(s)))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Collection<String> listAttributes(String setId) {
//        if ("email".equals(setId)) {
//            return Arrays.asList(ATTRIBUTES_EMAIL);
//        }
//
//        if ("profile".equals(setId)) {
//            return Arrays.asList(ATTRIBUTES_PROFILE);
//        }
//
//        if ("username".equals(setId)) {
//            return Arrays.asList(ATTRIBUTES_USERNAME);
//        }
//
//        return Collections.emptyList();
//
//    }
//
//    @Override
//    public UserAttributes getAttributes(String userId, String setId) throws NoSuchUserException {
//        // fetch and filter
//        Collection<String> keys = listAttributes(setId);
//        UserAttributes attributes = getAttributes(userId);
//
//        // filter
//        Set<Map.Entry<String, String>> attrs = attributes.getAttributes().entrySet().stream()
//                .filter(a -> keys.contains(a.getKey()))
//                .collect(Collectors.toSet());
//
//        return new BaseAttributesImpl(
//                getAuthority(), getProvider(),
//                userId,
//                attrs);
//
//    }

    @Override
    public String getType() {
        return SystemKeys.RESOURCE_ATTRIBUTES;
    }

    public static final String[] ATTRIBUTES_EMAIL = { "email" };
    public static final String[] ATTRIBUTES_USERNAME = { "username" };
    public static final String[] ATTRIBUTES_PROFILE = { "name", "surname" };

    @Override
    public Collection<it.smartcommunitylab.aac.core.model.AttributeSet> listCustomAttributeSets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<String> listCustomAttributes(String setId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canProvide(String globalSetId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public UserAttributes provideAttributes(UserIdentity identity, String globalSetId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserAttributes> convertAttributes(UserIdentity identity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserAttributes> convertAttributes(Collection<UserAttributes> attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAttributes convertAttributes(UserAttributes attributes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<UserAttributes> getUserAttributes(String userId) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UserAttributes getUserAttributes(String userId, String setId) throws NoSuchUserException {
        // TODO Auto-generated method stub
        return null;
    }

}
