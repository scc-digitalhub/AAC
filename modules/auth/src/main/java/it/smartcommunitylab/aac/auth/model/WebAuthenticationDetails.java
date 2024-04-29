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

package it.smartcommunitylab.aac.auth.model;

import it.smartcommunitylab.aac.SystemKeys;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;

public class WebAuthenticationDetails implements Serializable {

    private static final long serialVersionUID = SystemKeys.AAC_CORE_SERIAL_VERSION;

    private final String remoteAddress;
    private final String sessionId;
    private final long timestamp;
    private final String scheme;
    private final String protocol;
    private final Locale locale;
    private final String userAgent;

    // TODO evaluate new "client hints" as per google proposal
    // TODO evaluate fallback to spring mobile for device detect

    public WebAuthenticationDetails(HttpServletRequest request) {
        this.timestamp = Calendar.getInstance().getTimeInMillis();
        this.remoteAddress = request.getRemoteAddr();
        this.scheme = request.getScheme();
        this.locale = request.getLocale();
        this.protocol = request.getProtocol();
        this.userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        HttpSession session = request.getSession(false);
        this.sessionId = (session != null) ? session.getId() : null;
    }

    public String getRemoteAddress() {
        return this.remoteAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getScheme() {
        return scheme;
    }

    public String getProtocol() {
        return protocol;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return (locale != null ? locale.getLanguage() : null);
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getSessionId() {
        return this.sessionId;
    }
}
