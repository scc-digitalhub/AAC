package it.smartcommunitylab.aac.bootstrap;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.model.ClientAppBasic;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;

public class BootstrapClient {
    private String id;
    private String name;
    private String secret;
    private String developer;
    private String[] grantTypes;
    private String[] scopes;
    private String[] redirectUris;
    private String[] uniqueSpaces;
    private String claimMappingFunction;
    private String afterApprovalWebhook;
    private String[] rolePrefixes;
    private boolean isTrusted;
    private String jwtSignAlgorithm;
//    private String jwtSignKey;
    private String jwtEncAlgorithm;
    private String jwtEncMethod;
//    private String jwtEncKey;
    private String jwks;
    private String jwksUri;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String[] getGrantTypes() {
        return grantTypes;
    }

    public void setGrantTypes(String[] grantTypes) {
        this.grantTypes = grantTypes;
    }

    public String[] getScopes() {
        return scopes;
    }

    public void setScopes(String[] scopes) {
        this.scopes = scopes;
    }

    public String[] getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String[] redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String[] getUniqueSpaces() {
        return uniqueSpaces;
    }

    public void setUniqueSpaces(String[] uniqueSpaces) {
        this.uniqueSpaces = uniqueSpaces;
    }

    public String getClaimMappingFunction() {
        return claimMappingFunction;
    }

    public void setClaimMappingFunction(String claimMappingFunction) {
        this.claimMappingFunction = claimMappingFunction;
    }

    public String getAfterApprovalWebhook() {
        return afterApprovalWebhook;
    }

    public void setAfterApprovalWebhook(String afterApprovalWebhook) {
        this.afterApprovalWebhook = afterApprovalWebhook;
    }

    public boolean isTrusted() {
        return isTrusted;
    }

    public void setTrusted(boolean isTrusted) {
        this.isTrusted = isTrusted;
    }

    public String[] getRolePrefixes() {
        return rolePrefixes;
    }

    public void setRolePrefixes(String[] rolePrefixes) {
        this.rolePrefixes = rolePrefixes;
    }

    public String getJwtSignAlgorithm() {
        return jwtSignAlgorithm;
    }

    public void setJwtSignAlgorithm(String jwtSignAlgorithm) {
        this.jwtSignAlgorithm = jwtSignAlgorithm;
    }

    public String getJwtEncAlgorithm() {
        return jwtEncAlgorithm;
    }

    public void setJwtEncAlgorithm(String jwtEncAlgorithm) {
        this.jwtEncAlgorithm = jwtEncAlgorithm;
    }

    public String getJwtEncMethod() {
        return jwtEncMethod;
    }

    public void setJwtEncMethod(String jwtEncMethod) {
        this.jwtEncMethod = jwtEncMethod;
    }

    public String getJwks() {
        return jwks;
    }

    public void setJwks(String jwks) {
        this.jwks = jwks;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    /*
     * Builders
     */
    public static BootstrapClient fromClientApp(ClientAppBasic client) {
        BootstrapClient bc = new BootstrapClient();

        bc.id = client.getClientId();
        bc.name = client.getName();
        bc.developer = client.getUserName();
        bc.secret = client.getClientSecret();
        bc.scopes = new String[0];
        bc.grantTypes = new String[0];
        bc.redirectUris = new String[0];
//        bc.uniqueSpaces = new String[0];
//        bc.afterApprovalWebhook = "";

        // TODO extract authority from client for ROLE_TRUSTED check
        bc.isTrusted = false;

        if (client.getScope() != null) {
            bc.scopes = client.getScope().toArray(new String[0]);
        }

        if (client.getGrantedTypes() != null) {
            bc.grantTypes = client.getGrantedTypes().toArray(new String[0]);
        }

        if (client.getRedirectUris() != null) {
            bc.redirectUris = client.getRedirectUris().stream().filter(u -> StringUtils.hasText(u))
                    .collect(Collectors.toList())
                    .toArray(new String[0]);
        }

        if (client.getUniqueSpaces() != null) {
            bc.uniqueSpaces = client.getUniqueSpaces().toArray(new String[0]);
        }
        if (client.getRolePrefixes() != null) {
            bc.rolePrefixes = client.getRolePrefixes().toArray(new String[0]);
        }

        if (StringUtils.hasText(client.getOnAfterApprovalWebhook())) {
            bc.afterApprovalWebhook = client.getOnAfterApprovalWebhook();
        }

        if (StringUtils.hasText(client.getClaimMapping())) {
            // transform code base64encoded
            byte[] bytes = client.getClaimMapping().getBytes(Charset.forName("UTF-8"));
            String claimMappingCode = new String(Base64.getEncoder().encode(bytes));
            bc.setClaimMappingFunction(claimMappingCode);
        }

        // jwt
        if (StringUtils.hasText(client.getJwtSignAlgorithm())) {
            bc.setJwtSignAlgorithm(client.getJwtSignAlgorithm());
        }

        if (StringUtils.hasText(client.getJwtEncAlgorithm()) && StringUtils.hasText(client.getJwtEncMethod())) {
            bc.setJwtEncAlgorithm(client.getJwtEncAlgorithm());
            bc.setJwtEncMethod(client.getJwtEncMethod());

        }

        if (StringUtils.hasText(client.getJwksUri())) {
            bc.setJwksUri(client.getJwksUri());
        }

        if (StringUtils.hasText(client.getJwks())) {
            // check if valid json
            try {
                // TODO replace with sane org.json, minidev could break data..
//                JSONObject json = new JSONParser(JSONParser.MODE_PERMISSIVE).parse(client.getJwks(), JSONObject.class);

                // transform code base64encoded
                byte[] bytes = client.getJwks().getBytes(Charset.forName("UTF-8"));
                String claimMappingCode = new String(Base64.getEncoder().encode(bytes));
                bc.setJwks(claimMappingCode);
            } catch (Exception jex) {
                // invalid, skip
            }
        }

        return bc;
    }
}