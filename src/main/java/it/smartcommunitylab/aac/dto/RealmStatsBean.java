/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.dto;

import java.util.List;

import it.smartcommunitylab.aac.audit.RealmAuditEvent;
import it.smartcommunitylab.aac.model.Realm;

/**
 * @author raman
 *
 */
public class RealmStatsBean {

    private Realm realm;
    private Long users;
    private Integer apps;
    private Integer providers;
    private Integer providersActive;
    private Integer services;
    private Long events;

    private Long loginCount;
    private List<RealmAuditEvent> loginEvents;

    private Long registrationCount;
    private List<RealmAuditEvent> registrationEvents;

    public Realm getRealm() {
        return realm;
    }

    public void setRealm(Realm realm) {
        this.realm = realm;
    }

    public Long getUsers() {
        return users;
    }

    public void setUsers(Long users) {
        this.users = users;
    }

    public Integer getApps() {
        return apps;
    }

    public void setApps(Integer apps) {
        this.apps = apps;
    }

    public Integer getProviders() {
        return providers;
    }

    public void setProviders(Integer providers) {
        this.providers = providers;
    }

    public Integer getProvidersActive() {
        return providersActive;
    }

    public void setProvidersActive(Integer providersActive) {
        this.providersActive = providersActive;
    }

    public Integer getServices() {
        return services;
    }

    public void setServices(Integer services) {
        this.services = services;
    }

    public Long getEvents() {
        return events;
    }

    public void setEvents(Long events) {
        this.events = events;
    }

    public Long getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Long loginCount) {
        this.loginCount = loginCount;
    }

    public List<RealmAuditEvent> getLoginEvents() {
        return loginEvents;
    }

    public void setLoginEvents(List<RealmAuditEvent> loginEvents) {
        this.loginEvents = loginEvents;
    }

    public Long getRegistrationCount() {
        return registrationCount;
    }

    public void setRegistrationCount(Long registrationCount) {
        this.registrationCount = registrationCount;
    }

    public List<RealmAuditEvent> getRegistrationEvents() {
        return registrationEvents;
    }

    public void setRegistrationEvents(List<RealmAuditEvent> registrationEvents) {
        this.registrationEvents = registrationEvents;
    }

}
