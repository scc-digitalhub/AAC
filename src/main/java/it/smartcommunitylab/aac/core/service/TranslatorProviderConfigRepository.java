package it.smartcommunitylab.aac.core.service;

import it.smartcommunitylab.aac.core.base.AbstractProviderConfig;
import it.smartcommunitylab.aac.core.provider.ProviderConfigRepository;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;

public class TranslatorProviderConfigRepository<
    U extends AbstractProviderConfig<?, ?>, C extends AbstractProviderConfig<?, ?>
>
    implements ProviderConfigRepository<C> {

    private final ProviderConfigRepository<U> externalRepository;

    // converter function does nothing by default
    private Converter<U, C> converter = source -> (null);

    public TranslatorProviderConfigRepository(ProviderConfigRepository<U> externalRepository) {
        Assert.notNull(externalRepository, "source repository can not be null");

        this.externalRepository = externalRepository;
    }

    public void setConverter(Converter<U, C> converter) {
        this.converter = converter;
    }

    protected Converter<U, C> getConverter() {
        return converter;
    }

    @Override
    public C findByProviderId(String providerId) {
        Assert.hasText(providerId, "providerId cannot be empty");
        U ext = externalRepository.findByProviderId(providerId);
        if (ext == null) {
            return null;
        }

        return getConverter().convert(ext);
    }

    @Override
    public Collection<C> findAll() {
        return externalRepository
            .findAll()
            .stream()
            .map(e -> getConverter().convert(e))
            .filter(e -> e != null)
            .collect(Collectors.toList());
    }

    @Override
    public Collection<C> findByRealm(String realm) {
        return externalRepository
            .findByRealm(realm)
            .stream()
            .map(e -> getConverter().convert(e))
            .filter(e -> e != null)
            .collect(Collectors.toList());
    }

    @Override
    public void addRegistration(C registration) {
        // nothing to do
    }

    @Override
    public void removeRegistration(String providerId) {
        // nothing to do
    }

    @Override
    public void removeRegistration(C registration) {
        // nothing to do
    }
}
