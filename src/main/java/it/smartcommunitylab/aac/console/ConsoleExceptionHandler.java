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

package it.smartcommunitylab.aac.console;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.NoSuchAttributeException;
import it.smartcommunitylab.aac.common.NoSuchAttributeSetException;
import it.smartcommunitylab.aac.common.NoSuchClaimException;
import it.smartcommunitylab.aac.common.NoSuchClientException;
import it.smartcommunitylab.aac.common.NoSuchProviderException;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchResourceException;
import it.smartcommunitylab.aac.common.NoSuchScopeException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.common.SystemException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

@ControllerAdvice(basePackages = "it.smartcommunitylab.aac.console")
public class ConsoleExceptionHandler extends ResponseEntityExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(value = { AccessDeniedException.class })
    public final ResponseEntity<Object> handleAccessDenied(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.FORBIDDEN;
        return handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    @ExceptionHandler(
        {
            NoSuchAttributeException.class,
            NoSuchAttributeSetException.class,
            NoSuchClaimException.class,
            NoSuchClientException.class,
            NoSuchProviderException.class,
            NoSuchRealmException.class,
            NoSuchScopeException.class,
            NoSuchServiceException.class,
            NoSuchUserException.class,
            NoSuchResourceException.class,
        }
    )
    public final ResponseEntity<Object> handleNoSuchException(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.NOT_FOUND;
        return handleExceptionInternal(ex, ex.getMessage(), headers, status, request);
    }

    @ExceptionHandler(RegistrationException.class)
    public final ResponseEntity<Object> handleRegistrationException(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    @ExceptionHandler({ InvalidDefinitionException.class, IllegalArgumentException.class })
    public final ResponseEntity<Object> handleBadRequestException(Exception ex, WebRequest request) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return handleExceptionInternal(ex, null, headers, status, request);
    }

    @ExceptionHandler({ SystemException.class, RuntimeException.class, IOException.class })
    public ResponseEntity<Object> handleSystemException(Exception ex, WebRequest request) {
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        return handleExceptionInternal(ex, null, headers, status, request);
    }

    // @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception ex,
        @Nullable Object body,
        HttpHeaders headers,
        HttpStatus status,
        WebRequest request
    ) {
        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }

        Map<String, Object> response = buildResponse(request, ex, status, body);

        return new ResponseEntity<>(response, headers, status);
    }

    private Map<String, Object> buildResponse(WebRequest request, Exception ex, HttpStatus status, Object body) {
        logger.debug("build response for error: " + ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        //        response.put("timestamp", new Date())
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", ex.getMessage());
        if (request instanceof ServletWebRequest) {
            String path = ((ServletWebRequest) request).getRequest().getRequestURI();
            response.put("path", path);
        }
        if (body != null) {
            response.put("description", body);
        }

        logger.error("error processing request: " + ex.getMessage());
        if (logger.isTraceEnabled()) {
            ex.printStackTrace();
        }

        return response;
    }
}
