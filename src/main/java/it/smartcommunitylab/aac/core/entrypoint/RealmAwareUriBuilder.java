package it.smartcommunitylab.aac.core.entrypoint;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponents;

public interface RealmAwareUriBuilder {
    public UriComponents buildUri(HttpServletRequest request, String realm, String path);

    public UriComponents buildUri(String realm, String path);

    public String buildUrl(String realm, String path);
}
