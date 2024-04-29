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

package it.smartcommunitylab.aac.core.auth.handler;

import it.smartcommunitylab.aac.core.auth.LoginUrlRequestConverter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.Assert;

public class ExtendedLoginUrlAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {

    private List<LoginUrlRequestConverter> converters;

    private String defaultLoginUrl;

    public ExtendedLoginUrlAuthenticationEntryPoint(String defaultLoginUrl, LoginUrlRequestConverter... converters) {
        this(defaultLoginUrl, Arrays.asList(converters));
    }

    public ExtendedLoginUrlAuthenticationEntryPoint(
        String defaultLoginUrl,
        Collection<LoginUrlRequestConverter> converters
    ) {
        super(defaultLoginUrl);
        Assert.hasText(defaultLoginUrl, "default login url can not be null or empty");
        this.defaultLoginUrl = defaultLoginUrl;
        setConverters(converters);
    }

    public void setConverters(Collection<LoginUrlRequestConverter> converters) {
        this.converters = new ArrayList<>(converters);
    }

    protected String determineUrlToUseForThisRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) {
        String loginUrl = defaultLoginUrl;

        // let converters try in order
        for (LoginUrlRequestConverter converter : converters) {
            String url = converter.convert(request, response, exception);
            if (url != null) {
                loginUrl = url;
                break;
            }
        }

        return loginUrl;
    }
}
