package it.smartcommunitylab.aac.bootstrap;

import java.nio.charset.Charset;
import java.util.Base64;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.model.ClientAppBasic;

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
    private boolean isTrusted;

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
            bc.redirectUris = client.getRedirectUris().toArray(new String[0]);
        }

        if (client.getUniqueSpaces() != null) {
            bc.uniqueSpaces = client.getUniqueSpaces().toArray(new String[0]);
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

        return bc;
    }
}