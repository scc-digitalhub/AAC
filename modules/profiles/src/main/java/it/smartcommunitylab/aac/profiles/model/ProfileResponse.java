/*
 * Copyright 2023 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.smartcommunitylab.aac.profiles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Collection;
import org.springframework.util.Assert;

@JsonInclude(Include.NON_EMPTY)
public class ProfileResponse {

    private String subject;
    private String realm;

    @JsonUnwrapped
    private AbstractProfile profile;

    @JsonUnwrapped
    private Collection<AbstractProfile> profiles;

    public ProfileResponse(String subject) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
    }

    public ProfileResponse(String subject, AbstractProfile profile) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        this.profile = profile;
    }

    public ProfileResponse(String subject, Collection<AbstractProfile> profiles) {
        Assert.notNull(subject, "subject can not be null");
        this.subject = subject;
        if (profiles != null) {
            if (profiles.size() == 1) {
                this.profile = profiles.iterator().next();
            } else {
                this.profiles = profiles;
            }
        }
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

    public AbstractProfile getProfile() {
        return profile;
    }

    public void setProfile(AbstractProfile profile) {
        this.profile = profile;
    }

    public Collection<AbstractProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(Collection<AbstractProfile> profiles) {
        if (profiles != null && profiles.size() == 1) {
            this.profile = profiles.iterator().next();
            this.profiles = null;
        } else {
            this.profiles = profiles;
        }
    }
}
