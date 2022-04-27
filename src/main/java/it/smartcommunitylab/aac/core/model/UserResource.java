package it.smartcommunitylab.aac.core.model;

/*
 * A realm scoped user resource, provided by an authority via a specific provider
 */

public interface UserResource extends Resource {

    public String getUserId();

}
