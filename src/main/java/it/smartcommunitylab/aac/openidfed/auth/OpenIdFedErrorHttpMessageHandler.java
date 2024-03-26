/**
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

package it.smartcommunitylab.aac.openidfed.auth;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;

public class OpenIdFedErrorHttpMessageHandler {

    private final OAuth2ErrorHttpMessageConverter messageConverter = new OAuth2ErrorHttpMessageConverter();

    public void handleError(OAuth2Error error, HttpServletResponse response)
        throws HttpMessageNotWritableException, IOException {
        //build a response message
        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        //unwrap and write
        if ("not_found".equals(error.getErrorCode())) {
            status = HttpStatus.NOT_FOUND;
        }
        if (OAuth2ErrorCodes.INVALID_REQUEST.equals(error.getErrorCode())) {
            status = HttpStatus.BAD_REQUEST;
        }
        if (OAuth2ErrorCodes.INVALID_CLIENT.equals(error.getErrorCode())) {
            status = HttpStatus.UNAUTHORIZED;
        }
        if (OAuth2ErrorCodes.SERVER_ERROR.equals(error.getErrorCode())) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (OAuth2ErrorCodes.TEMPORARILY_UNAVAILABLE.equals(error.getErrorCode())) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        if ("unsupported_parameter".equals(error.getErrorCode())) {
            status = HttpStatus.BAD_REQUEST;
        }

        httpResponse.setStatusCode(status);
        messageConverter.write(error, MediaType.APPLICATION_JSON, httpResponse);
    }
}
