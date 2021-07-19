package it.smartcommunitylab.aac.profiles.model;

import java.time.LocalDate;
import java.util.Date;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import it.smartcommunitylab.aac.SystemKeys;

@JsonInclude(Include.NON_EMPTY)
public class OpenIdProfile extends AbstractProfile {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public static final String IDENTIFIER = "openid";

    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("nickname")
    private String nickName;

    @JsonProperty("preferred_username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("email_verified")
    private Boolean emailVerified;

    @JsonProperty("phone_number")
    private String phone;

    @JsonProperty("phone_number_verified")
    private Boolean phoneVerified;

    @JsonProperty("profile")
    private String profileUrl;

    @JsonProperty("picture")
    private String pictureUrl;

    @JsonProperty("website")
    private String websiteUrl;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("birthdate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    private String zoneinfo;
    private String locale;

    // TODO implement
    private Object address;

    @JsonProperty("updated_at")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Date updatedAt;

    public OpenIdProfile() {

    }

    public OpenIdProfile(BasicProfile basic) {
        this.name = basic.getName();
        this.givenName = basic.getName();
        this.familyName = basic.getSurname();
        this.username = basic.getUsername();
        this.email = basic.getEmail();
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /*
     * Narrow down profile
     */

    public OpenIdProfile toDefaultProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        profile.name = this.name;
        profile.givenName = this.givenName;
        profile.familyName = this.familyName;
        profile.middleName = this.middleName;
        profile.nickName = this.nickName;
        profile.username = this.username;
        profile.profileUrl = this.profileUrl;
        profile.pictureUrl = this.pictureUrl;
        profile.websiteUrl = this.websiteUrl;
        profile.gender = this.gender;
        profile.birthdate = this.birthdate;
        profile.zoneinfo = this.zoneinfo;
        profile.locale = this.locale;
        profile.updatedAt = this.updatedAt;

        return profile;
    }

    public OpenIdProfile toEmailProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        if (StringUtils.hasText(this.email)) {
            profile.email = this.email;
            profile.emailVerified = this.emailVerified;
        }
        return profile;
    }

    public OpenIdProfile toAddressProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        profile.address = this.address;

        return profile;
    }

    public OpenIdProfile toPhoneProfile() {
        OpenIdProfile profile = new OpenIdProfile();
        if (StringUtils.hasText(this.phone)) {
            profile.phone = this.phone;
            profile.phoneVerified = this.phoneVerified;
        }
        return profile;
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

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(Boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public String getZoneinfo() {
        return zoneinfo;
    }

    public void setZoneinfo(String zoneinfo) {
        this.zoneinfo = zoneinfo;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Object getAddress() {
        return address;
    }

    public void setAddress(Object address) {
        this.address = address;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

}
