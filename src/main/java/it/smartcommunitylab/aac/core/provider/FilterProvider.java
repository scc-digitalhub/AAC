package it.smartcommunitylab.aac.core.provider;

import java.util.Collection;

import javax.servlet.Filter;

public interface FilterProvider {
    public String getAuthorityId();

    public Collection<Filter> getFilters();

}
