package it.smartcommunitylab.aac.core.service;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.core.persistence.RealmEntity;
import it.smartcommunitylab.aac.core.persistence.RealmEntityRepository;
import it.smartcommunitylab.aac.dto.CustomizationBean;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.oauth.model.OAuth2ConfigurationMap;

@Service
@Transactional
public class RealmService implements InitializingBean {

    public static final Set<String> RESERVED_SLUG;

    static {
        Set<String> s = new HashSet<>();
        s.add(SystemKeys.REALM_SYSTEM);
        s.add(SystemKeys.REALM_COMMON);
        s.add(SystemKeys.REALM_INTERNAL);
        s.add("aac");
        RESERVED_SLUG = s;
    }

    private final RealmEntityRepository realmRepository;

    // static immutable systemRealm
    private final Realm systemRealm;

    public RealmService(RealmEntityRepository realmRepository) {
        Assert.notNull(realmRepository, "realm repository is mandatory");
        this.realmRepository = realmRepository;

        // build system realm
        systemRealm = new Realm(SystemKeys.REALM_SYSTEM, SystemKeys.REALM_SYSTEM);
        systemRealm.setEditable(false);
        systemRealm.setPublic(false);
        if (realmRepository.findBySlug(SystemKeys.REALM_SYSTEM) == null) {
            RealmEntity re = new RealmEntity();
            re.setSlug(systemRealm.getSlug());
            re.setName(systemRealm.getName());
            re.setEditable(false);
            re.setPublic(false);
            realmRepository.save(re);
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(systemRealm, "system realm can not be null");
    }

    public Realm addRealm(String slug, String name, boolean isEditable, boolean isPublic)
            throws AlreadyRegisteredException {
        if (!StringUtils.hasText(slug)) {
            throw new RegistrationException("a valid slug is required");
        }

        if (SystemKeys.REALM_GLOBAL.equals(slug) || SystemKeys.REALM_SYSTEM.equals(slug)) {
            throw new IllegalArgumentException("system realms are immutable");
        }

        if (RESERVED_SLUG.contains(slug)) {
            throw new RegistrationException("slug is reserved");
        }

        if (!StringUtils.hasText(name)) {
            name = slug;
        }

        RealmEntity r = realmRepository.findBySlug(slug);
        if (r != null) {
            throw new AlreadyRegisteredException("slug already exists");
        }

        r = new RealmEntity();
        r.setSlug(slug);
        r.setName(name);
        r.setEditable(isEditable);
        r.setPublic(isPublic);

        r = realmRepository.save(r);

        return toRealm(r);

    }

    @Transactional(readOnly = true)
    public Realm findRealm(String slug) {
        if (!StringUtils.hasText(slug)) {
            return null;
        }

        if (SystemKeys.REALM_SYSTEM.equals(slug)) {
            return systemRealm;
        }

        RealmEntity r = realmRepository.findBySlug(slug);
        if (r == null) {
            return null;
        }

        return toRealm(r);
    }

    @Transactional(readOnly = true)
    public Realm getRealm(String slug) throws NoSuchRealmException {
        Realm realm = findRealm(slug);
        if (realm == null) {
            throw new NoSuchRealmException();
        }

        return realm;
    }

    public Realm updateRealm(String slug, String name, boolean isEditable, boolean isPublic,
            Map<String, Serializable> oauthConfigurationMap,
            Map<String, Map<String, String>> customizations)
            throws NoSuchRealmException {
        if (SystemKeys.REALM_GLOBAL.equals(slug) || SystemKeys.REALM_SYSTEM.equals(slug)) {
            throw new IllegalArgumentException("system realms are immutable");
        }

        RealmEntity r = realmRepository.findBySlug(slug);
        if (r == null) {
            throw new NoSuchRealmException();
        }

        r.setName(name);
        r.setEditable(isEditable);
        r.setPublic(isPublic);

        Map<String, Map<String, String>> customMap = null;
        if (customizations != null) {
            customMap = new HashMap<>();
            for (Map.Entry<String, Map<String, String>> e : customizations.entrySet()) {
                if (StringUtils.hasText(e.getKey()) && e.getValue() != null) {
                    Map<String, String> vv = e.getValue().entrySet().stream()
                            .filter(ee -> StringUtils.hasText(ee.getValue()))
                            .collect(Collectors.toMap(ee -> ee.getKey(), ee -> ee.getValue()));

                    if (!vv.isEmpty()) {
                        customMap.put(e.getKey(), vv);
                    }
                }
            }
        }
        r.setCustomizations(customMap);
        r.setOAuthConfigurationMap(oauthConfigurationMap);
        r = realmRepository.save(r);

        return toRealm(r);

    }

    public void deleteRealm(String slug) {
        if (SystemKeys.REALM_GLOBAL.equals(slug) || SystemKeys.REALM_SYSTEM.equals(slug)) {
            throw new IllegalArgumentException("system realms are immutable");
        }

        RealmEntity r = realmRepository.findBySlug(slug);
        if (r != null) {
            realmRepository.delete(r);
        }
    }

    @Transactional(readOnly = true)
    public List<Realm> listRealms() {
        List<RealmEntity> realms = realmRepository.findAll();
        return realms.stream().map(r -> toRealm(r)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Realm> listRealms(boolean isPublic) {
        List<RealmEntity> realms = realmRepository.findByIsPublic(isPublic);
        return realms.stream().map(r -> toRealm(r)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Realm> searchRealms(String keywords) {
        Set<RealmEntity> realms = new HashSet<>();
        realms.addAll(realmRepository.findBySlugContainingIgnoreCase(keywords));
        realms.addAll(realmRepository.findByNameContainingIgnoreCase(keywords));

        return realms.stream().map(r -> toRealm(r)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<Realm> searchRealms(String keywords, Pageable pageRequest) {
        Page<RealmEntity> page = StringUtils.hasText(keywords) ? realmRepository.findByKeywords(keywords, pageRequest)
                : realmRepository.findAll(pageRequest);
        return PageableExecutionUtils.getPage(
                page.getContent().stream().map(r -> toRealm(r)).collect(Collectors.toList()),
                pageRequest,
                () -> page.getTotalElements());
    }

    /*
     * Helpers
     */
    private Realm toRealm(RealmEntity re) {
        Realm r = new Realm(re.getSlug(), re.getName());
        r.setEditable(re.isEditable());
        r.setPublic(re.isPublic());

        OAuth2ConfigurationMap oauth2ConfigMap = new OAuth2ConfigurationMap();
        if (re.getOAuthConfigurationMap() != null) {
            oauth2ConfigMap.setConfiguration(re.getOAuthConfigurationMap());
        }
        r.setOAuthConfiguration(oauth2ConfigMap);

        // build a template customization map
        Map<String, CustomizationBean> map = new HashMap<>();
        r.setCustomization(Collections.emptyList());

        if (re.getCustomizations() != null) {
            re.getCustomizations().entrySet().stream().forEach(m -> {
                if (m.getValue() != null) {
                    String k = m.getKey();
                    if (!map.containsKey(k)) {
                        map.put(k, new CustomizationBean(k));
                    }

                    map.get(k).setResources(m.getValue());
                }
            });
        }

        r.setCustomization(map.values().stream().collect(Collectors.toList()));

        return r;
    }
}
