package it.smartcommunitylab.aac.openid.session;

import java.util.Collection;

public interface SessionStateStore {
    public String find(String state);

    public Collection<String> findAll();

    public String store(String state);

    public void remove(String state);
}
