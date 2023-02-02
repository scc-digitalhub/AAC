package it.smartcommunitylab.aac.core.base;

import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.core.model.Resource;
import it.smartcommunitylab.aac.core.provider.ResourceProvider;

public abstract class AbstractProvider<R extends Resource> implements ResourceProvider<R> {

    private final String authority;
    private final String realm;
    private final String provider;

    protected AbstractProvider(String authority, String provider, String realm) {
        this.authority = authority;
        this.realm = realm;
        this.provider = provider;
    }

    @Override
    public String getAuthority() {
        return authority;
    }

    @Override
    public String getProvider() {
        return provider;
    }

    @Override
    public String getRealm() {
        return realm;
    }

//    /*
//     * Resource id logic according to URN
//     * 
//     * authority|provider|id
//     * 
//     * subclasses can override
//     */
//    protected String parseAuthority(String resourceId) throws IllegalArgumentException {
//        String[] s = _parseResourceId(resourceId);
//
//        if (s.length != 3) {
//            throw new IllegalArgumentException("invalid resource id");
//        }
//
//        if (!StringUtils.hasText(s[0])) {
//            throw new IllegalArgumentException("empty authority id");
//        }
//
//        return s[0];
//    }
//
//    protected String parseProvider(String resourceId) throws IllegalArgumentException {
//        String[] s = _parseResourceId(resourceId);
//
//        if (s.length != 3) {
//            throw new IllegalArgumentException("invalid resource id");
//        }
//
//        if (!StringUtils.hasText(s[1])) {
//            throw new IllegalArgumentException("empty provider id");
//        }
//
//        return s[1];
//    }
//
//    protected String parseId(String resourceId) throws IllegalArgumentException {
//        String[] s = _parseResourceId(resourceId);
//
//        if (!StringUtils.hasText(s[2])) {
//            throw new IllegalArgumentException("empty resource id");
//        }
//
//        return s[2];
//    }
//
//    protected String parseType(String urn) throws IllegalArgumentException {
//        String[] s = _parseUrn(urn);
//
//        if (!StringUtils.hasText(s[1])) {
//            throw new IllegalArgumentException("empty type id");
//        }
//
//        return s[1];
//    }
//
//    protected String parseResourceId(String urn) throws IllegalArgumentException {
//        String[] s = _parseUrn(urn);
//
//        if (!StringUtils.hasText(s[2])) {
//            throw new IllegalArgumentException("empty resource id");
//        }
//
//        return s[2];
//    }
//
//    private String[] _parseUrn(String urn) throws IllegalArgumentException {
//        if (!StringUtils.hasText(urn)) {
//            throw new IllegalArgumentException("empty or null urn");
//        }
//
//        StringBuilder sb = new StringBuilder();
//        sb.append(SystemKeys.URN_PROTOCOL).append(SystemKeys.URN_SEPARATOR);
//        String prefix = sb.toString();
//
//        if (!urn.startsWith(prefix)) {
//            throw new IllegalArgumentException("invalid resource urn");
//        }
//
//        String[] s = urn.split(Pattern.quote(SystemKeys.URN_SEPARATOR));
//
//        if (s.length < 2) {
//            throw new IllegalArgumentException("invalid resource urn");
//        }
//
//        return s;
//    }
//
//    private String[] _parseResourceId(String resourceId) throws IllegalArgumentException {
//        if (!StringUtils.hasText(resourceId)) {
//            throw new IllegalArgumentException("empty or null id");
//        }
//
//        String[] s = resourceId.split(Pattern.quote(SystemKeys.ID_SEPARATOR));
//
//        if (s.length != 3) {
//            throw new IllegalArgumentException("invalid resource id");
//        }
//
//        return s;
//    }

}
