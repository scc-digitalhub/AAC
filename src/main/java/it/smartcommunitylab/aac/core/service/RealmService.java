package it.smartcommunitylab.aac.core.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.AlreadyRegisteredException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.core.persistence.RealmEntity;
import it.smartcommunitylab.aac.core.persistence.RealmEntityRepository;
import it.smartcommunitylab.aac.model.Realm;

@Service
public class RealmService {

    private final RealmEntityRepository realmRepository;

    public RealmService(RealmEntityRepository realmRepository) {
        Assert.notNull(realmRepository, "realm repository is mandatory");
        this.realmRepository = realmRepository;
    }

    public Realm addRealm(String slug, String name) throws AlreadyRegisteredException {
        RealmEntity r = realmRepository.findBySlug(slug);
        if (r != null) {
            throw new AlreadyRegisteredException("slug already exists");
        }

        r = new RealmEntity();
        r.setSlug(slug);
        r.setName(name);

        r = realmRepository.save(r);

        return toRealm(r);

    }

    public Realm findRealm(String slug) {
        RealmEntity r = realmRepository.findBySlug(slug);
        if (r == null) {
            return null;
        }

        return toRealm(r);
    }

    public Realm getRealm(String slug) throws NoSuchRealmException {
        RealmEntity r = realmRepository.findBySlug(slug);
        if (r == null) {
            throw new NoSuchRealmException();
        }

        return toRealm(r);
    }

    public Realm updateRealm(String slug, String name) throws NoSuchRealmException {
        RealmEntity r = realmRepository.findBySlug(slug);
        if (r == null) {
            throw new NoSuchRealmException();
        }

        r.setName(name);

        r = realmRepository.save(r);

        return toRealm(r);

    }

    public void deleteRealm(String slug) {
        RealmEntity r = realmRepository.findBySlug(slug);
        if (r != null) {
            realmRepository.delete(r);
        }
    }

    public List<Realm> listRealms() {
        List<RealmEntity> realms = realmRepository.findAll();
        return realms.stream().map(r -> toRealm(r)).collect(Collectors.toList());
    }

    public List<Realm> searchRealms(String keywords) {
        Set<RealmEntity> realms = new HashSet<>();
        realms.addAll(realmRepository.findBySlugContainingIgnoreCase(keywords));
        realms.addAll(realmRepository.findByNameContainingIgnoreCase(keywords));

        return realms.stream().map(r -> toRealm(r)).collect(Collectors.toList());
    }
    public Page<Realm> searchRealms(String keywords, Pageable pageRequest) {
    	Page<RealmEntity> page = StringUtils.hasText(keywords) ? realmRepository.findByKeywords(keywords, pageRequest) : realmRepository.findAll(pageRequest);
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
        return r;
    }
}
