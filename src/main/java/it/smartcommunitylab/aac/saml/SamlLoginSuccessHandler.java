package it.smartcommunitylab.aac.saml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.util.StringUtils;

public class SamlLoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    protected final Log logger = LogFactory.getLog(this.getClass());

    public SamlLoginSuccessHandler(String defaultTargetUrl) {
        super(defaultTargetUrl);
    }

    protected void handle(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        logger.trace(authentication.toString());
        logger.trace(authentication.getClass().getName());
        logger.trace(authentication.getPrincipal().getClass().getName());
        
        //hardcoded redirect
        //TODO replace
        request.getSession().setAttribute("redirect", "/account");
        
        
        ExpiringUsernameAuthenticationToken auth = (ExpiringUsernameAuthenticationToken) authentication;
        SAMLCredential saml = (SAMLCredential) auth.getCredentials();

        Map<String, String> details = preprocess(saml.getAttributes());
        try {
            URIBuilder builder = new URIBuilder(getDefaultTargetUrl());
            for (String key : details.keySet()) {
                builder.addParameter(key, details.get(key));
                request.setAttribute(key, details.get(key));
            }
            request.getRequestDispatcher(builder.build().toString()).forward(request, response);
//          response.sendRedirect("forward:"+builder.build().toString());
//          getRedirectStrategy().sendRedirect(request, response, builder.build().toString());
        } catch (URISyntaxException e) {
            throw new ServletException(e.getMessage());
        }
    }

    /**
     * @param details
     * @return
     */
    protected Map<String, String> preprocess(List<Attribute> attributes) {
        Map<String, String> result = new HashMap<>();
        // flatten all attributes
        for (Attribute attr : attributes) {
            String key = attr.getName();
            String friendlyName = attr.getFriendlyName();
            String value = getAttributeAsString(attr);

            if (StringUtils.hasText(friendlyName)) {
                key = friendlyName;
            }

            result.put(key, value);

        }
        return result;
    }

    private String getAttributeAsString(Attribute attribute) {
        if (attribute == null) {
            return null;
        }
        List<XMLObject> attributeValues = attribute.getAttributeValues();
        if (attributeValues == null || attributeValues.size() == 0) {
            return null;
        }
        XMLObject xmlValue = attributeValues.iterator().next();
        return getString(xmlValue);
    }

    private String getString(XMLObject xmlValue) {
        if (xmlValue instanceof XSString) {
            return ((XSString) xmlValue).getValue();
        } else if (xmlValue instanceof XSAny) {
            return ((XSAny) xmlValue).getTextContent();
        } else {
            return null;
        }
    }
}
