package it.smartcommunitylab.aac.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import it.smartcommunitylab.aac.common.NoSuchUserException;
import it.smartcommunitylab.aac.common.RegistrationException;

@ControllerAdvice(basePackages = "it.smartcommunitylab.aac.api")
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({
            NoSuchServiceException.class,
            NoSuchUserException.class,
            NoSuchRealmException.class })

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

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

        if (HttpStatus.INTERNAL_SERVER_ERROR.equals(status)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, WebRequest.SCOPE_REQUEST);
        }

        Map<String, Object> response = buildResponse(request, ex, status, body);

        return new ResponseEntity<>(response, headers, status);
    }

    private Map<String, Object> buildResponse(WebRequest request, Exception ex, HttpStatus status, Object body) {
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

        return response;

    }

}