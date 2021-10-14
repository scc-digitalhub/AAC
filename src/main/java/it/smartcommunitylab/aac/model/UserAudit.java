package it.smartcommunitylab.aac.model;

import java.util.Date;
import java.util.List;

/*
 * A model used to audit user activity
 */
public class UserAudit {

    private String subjectId;

    private String realm;

    private List<String> realms;

    private Date createDate;

    private Date modifiedDate;

    private Date loginDate;

    private String loginIp;

    private String loginProvider;

    // TODO add 2FA/MFA, tokens, status, approvals etc

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<String> getRealms() {
        return realms;
    }

    public void setRealms(List<String> realms) {
        this.realms = realms;
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

    public Date getLoginDate() {
        return loginDate;
    }

    public void setLoginDate(Date loginDate) {
        this.loginDate = loginDate;
    }

    public String getLoginIp() {
        return loginIp;
    }

    public void setLoginIp(String loginIp) {
        this.loginIp = loginIp;
    }

    public String getLoginProvider() {
        return loginProvider;
    }

    public void setLoginProvider(String loginProvider) {
        this.loginProvider = loginProvider;
    }

}
