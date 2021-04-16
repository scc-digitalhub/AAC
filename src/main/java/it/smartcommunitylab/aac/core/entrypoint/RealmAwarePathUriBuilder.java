package it.smartcommunitylab.aac.core.entrypoint;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class RealmAwarePathUriBuilder implements RealmAwareUriBuilder {
    public static final String REALM_URI_PATH_SEPARATOR = "-";

    private final String baseUrl;

    public RealmAwarePathUriBuilder(String baseUrl) {
        Assert.notNull(baseUrl, "base url is required");
        this.baseUrl = baseUrl;
    }

    public UriComponents buildUri(HttpServletRequest request, String realm, String path) {
        UriComponentsBuilder template = UriComponentsBuilder.fromUriString(request.getRequestURL().toString());
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

        URI requestUri = template.build().toUri();
        builder.scheme(requestUri.getScheme()).host(requestUri.getHost()).port(requestUri.getPort());
        builder.path(request.getContextPath());

        if (StringUtils.hasText(realm) && isRealmPath(path)) {
            builder.pathSegment(REALM_URI_PATH_SEPARATOR, realm);
        }

        builder.path(path);

        return builder.build();
    }

    public UriComponents buildUri(String realm, String path) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (StringUtils.hasText(realm) && isRealmPath(path)) {
            builder.pathSegment(REALM_URI_PATH_SEPARATOR, realm);
        }

        builder.path(path);

        return builder.build();
    }

    public String buildUrl(String realm, String path) {
        return buildUri(realm, path).toUriString();
    }

    public boolean isRealmPath(String path) {
        return true;
    }

}
