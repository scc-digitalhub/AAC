package it.smartcommunitylab.aac.claims.base;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.claims.model.Claim;

@Valid
public abstract class AbstractClaim implements Claim {

    @Pattern(regexp = SystemKeys.KEY_PATTERN)
    @NotBlank
    protected String key;

    protected String id;

//    protected Boolean isMultiple;

    // TODO i18n
//    protected String name;
//    protected String description;

    @Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

//    @Override
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    @Override
//    public String getDescription() {
//        return description;
//    }
//
//    public void setDescription(String description) {
//        this.description = description;
//    }
//
//    public Boolean getIsMultiple() {
//        return isMultiple;
//    }
//
//    public void setIsMultiple(Boolean isMultiple) {
//        this.isMultiple = isMultiple;
//    }

    @Override
    public String getClaimId() {
        return id != null ? id : key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

//    @Override
//    public boolean isMultiple() {
//        return isMultiple != null ? isMultiple.booleanValue() : false;
//    }

    public Serializable exportValue() {
        // by default use current value as-is
        return getValue();
    }
}
