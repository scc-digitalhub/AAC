package it.smartcommunitylab.aac.openid;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Constants;
import it.smartcommunitylab.aac.core.base.BaseAccount;

@Entity
@Table(name = "oidc_users", uniqueConstraints = @UniqueConstraint(columnNames = { "realm", "user_id" }))
public class OIDCUserAccount extends BaseAccount {

    @Id
    @GeneratedValue
    private Long id;

    // entity
    @NotNull
    private String subject;

    private String realm;

    // provider id
    @NotNull
    @Column(name = "user_id")
    private String userId;
    @NotNull
    @Column(name = "provider_id")
    private String providerId;
    @NotNull
    private String issuer;

    // attributes
    private String email;
    @Column(name = "email_verified")
    private Boolean emailVerified;

    private String name;
    @Column(name = "given_name")
    private String givenName;
    @Column(name = "family_name")
    private String familyName;

    @Column(name = "profile_uri")
    private String profileUri;
    @Column(name = "picture_uri")
    private String pictureUri;

    // audit
    @CreatedDate
    @Column(name = "created_date")
    private Date createDate;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date modifiedDate;

    @Override
    public String getAuthority() {
        return Constants.AUTHORITY_OIDC;
    }

    @Override
    public String getProvider() {
        // we add realm to identify where user belongs
        return Constants.AUTHORITY_OIDC + "|" + realm + "|" + providerId;
    }

    @Override
    public String getUsername() {
        // we use email as username, fallback to userId
        return StringUtils.hasText(email) ? email : userId;
    }

    /*
     * Fields
     */

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getProfileUri() {
        return profileUri;
    }

    public void setProfileUri(String profileUri) {
        this.profileUri = profileUri;
    }

    public String getPictureUri() {
        return pictureUri;
    }

    public void setPictureUri(String pictureUri) {
        this.pictureUri = pictureUri;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

}
