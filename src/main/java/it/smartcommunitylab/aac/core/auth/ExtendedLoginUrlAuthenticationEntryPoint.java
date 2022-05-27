package it.smartcommunitylab.aac.core.auth;

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

    public ExtendedLoginUrlAuthenticationEntryPoint(String defaultLoginUrl,
            Collection<LoginUrlRequestConverter> converters) {
        super(defaultLoginUrl);
        Assert.hasText(defaultLoginUrl, "default login url can not be null or empty");
        this.defaultLoginUrl = defaultLoginUrl;
        setConverters(converters);
    }

    public void setConverters(Collection<LoginUrlRequestConverter> converters) {
        this.converters = new ArrayList<>(converters);
    }

    protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) {

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
