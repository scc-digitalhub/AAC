package it.smartcommunitylab.aac.core.auth;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.Assert;

public class DelegatingAuthenticationConverter implements AuthenticationConverter {

    private List<AuthenticationConverter> converters;

    public DelegatingAuthenticationConverter(AuthenticationConverter... converters) {
        this(Arrays.asList(converters));
    }

    public DelegatingAuthenticationConverter(List<AuthenticationConverter> converters) {
        Assert.notNull(converters, "authentication converters are required");
        setConverters(converters);
    }

    public void setConverters(List<AuthenticationConverter> converters) {
        this.converters = converters;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        // get first valid response
        for (AuthenticationConverter converter : converters) {
            Authentication auth = converter.convert(request);
            if (auth != null) {
                return auth;
            }
        }

        return null;
    }

}
