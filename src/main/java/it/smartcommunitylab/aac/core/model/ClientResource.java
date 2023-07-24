package it.smartcommunitylab.aac.core.model;

/*
 * A realm scoped client resource, provided by an authority via a specific provider
 */

public interface ClientResource extends Resource {
    public String getClientId();

    default String getProvider() {
        return null;
    }
}
