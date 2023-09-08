/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.core.entrypoint;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
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
