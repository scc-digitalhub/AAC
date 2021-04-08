package it.smartcommunitylab.aac.core.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.core.persistence.ScopeEntity;
import it.smartcommunitylab.aac.core.persistence.ScopeEntityRepository;
import it.smartcommunitylab.aac.model.AttributeType;
import it.smartcommunitylab.aac.model.ScopeType;
import it.smartcommunitylab.aac.scope.Scope;
import it.smartcommunitylab.aac.scope.ScopeProvider;

@Service
public class ScopeService implements ScopeProvider {

    private final ScopeEntityRepository scopeRepository;

    public ScopeService(ScopeEntityRepository scopeRepository) {
        Assert.notNull(scopeRepository, "scope repository is required");
        this.scopeRepository = scopeRepository;
    }

    public Scope getScope(String scope) throws NoSuchScopeException {
        ScopeEntity s = scopeRepository.findByScope(scope);
        if (s == null) {
            throw new NoSuchScopeException();
        }

        return toScope(s);
    }

    public Scope findScope(String scope) throws NoSuchScopeException {
        ScopeEntity s = scopeRepository.findByScope(scope);
        if (s == null) {
            return null;
        }

        return toScope(s);
    }

    public List<Scope> findScopes(String resourceId) {
        List<ScopeEntity> list = scopeRepository.findByResourceId(resourceId);

        return list.stream().map(se -> toScope(se)).collect(Collectors.toList());

    }

    public List<Scope> findScopes(String resourceId, String type) {
        List<ScopeEntity> list = scopeRepository.findByResourceIdAndType(resourceId, type);

        return list.stream().map(se -> toScope(se)).collect(Collectors.toList());

    }

    public Scope addScope(
            String scope, String resourceId,
            String name, String description,
            ScopeType type) {

        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        ScopeEntity se = new ScopeEntity();
        se.setScope(scope);
        se.setResourceId(resourceId);
        se.setName(name);
        se.setDescription(description);
        se.setType(type.getValue());

        se = scopeRepository.save(se);

        return toScope(se);

    }

    public Scope updateScope(
            String scope,
            String name, String description,
            ScopeType type) throws NoSuchScopeException {

        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("invalid scope");
        }

        ScopeEntity se = scopeRepository.findByScope(scope);
        if (se == null) {
            throw new NoSuchScopeException();
        }

        se.setName(name);
        se.setDescription(description);
        se.setType(type.getValue());

        se = scopeRepository.save(se);

        return toScope(se);

    }

    public void deleteScope(String scope) {
        ScopeEntity se = scopeRepository.findByScope(scope);
        if (se != null) {
            scopeRepository.delete(se);
        }

    }

    /*
     * 
     */
    private Scope toScope(ScopeEntity se) {
        Scope s = new Scope(se.getScope());
        s.setResourceId(se.getResourceId());
        s.setName(se.getName());
        s.setDescription(se.getDescription());
        s.setType(ScopeType.parse(se.getType()));

        return s;
    }

    /*
     * Scope provider: expose scopes defined in db to the local registry
     */
    @Override
    public String getResourceId() {
        // not applicable
        return null;
    }

    @Override
    public Collection<Scope> getScopes() {
        // translate each scope entity to scope and expose
        List<ScopeEntity> entities = scopeRepository.findAll();
        return entities.stream().map(se -> toScope(se)).collect(Collectors.toList());
    }

}
