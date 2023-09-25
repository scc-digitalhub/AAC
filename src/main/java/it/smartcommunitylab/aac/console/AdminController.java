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

package it.smartcommunitylab.aac.console;

import static org.mockito.Answers.values;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.micrometer.core.instrument.Measurement;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.SystemKeys;
import it.smartcommunitylab.aac.common.NoSuchRealmException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.config.ApplicationProperties;
import it.smartcommunitylab.aac.model.Realm;
import it.smartcommunitylab.aac.realms.RealmManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.ListNamesResponse;
import org.springframework.boot.actuate.metrics.MetricsEndpoint.MetricResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@PreAuthorize("hasAuthority('" + Config.R_ADMIN + "')")
@RequestMapping("/console/admin")
public class AdminController {

    private static final String[] SORT_WHITELIST = { "slug", "name" };

    // @formatter:off
    private static final String[] METRICS_KEYS = { 
        "jvm.memory.used", "jvm.memory.max",
        "jvm.threads.live", "jvm.threads.daemon", "jvm.threads.peak",
        "hikaricp.connections", "hikaricp.connections.max", "hikaricp.connections.min", "hikaricp.connections.usage",
        "http.server.requests",
        "process.cpu.usage","process.uptime","process.files.open","process.files.max",
        "system.cpu.count", "system.cpu.usage", "system.load.average.1m",
        "tomcat.sessions.active.current", "tomcat.sessions.active.max", "tomcat.sessions.alive.max", "tomcat.sessions.created",
        "tomcat.sessions.expired", "tomcat.sessions.rejected"
    };

    // @formatter:on

    @Autowired
    private MetricsEndpoint metrics;

    @Autowired
    private ApplicationProperties appProps;

    @Autowired
    private RealmManager realmManager;

    @GetMapping("/props")
    public ResponseEntity<ApplicationProperties> appProps() {
        return ResponseEntity.ok(appProps);
    }

    @GetMapping("/metrics")
    public ResponseEntity<List<SystemMetric>> metrics() {
        List<SystemMetric> values = Arrays
            .asList(METRICS_KEYS)
            .stream()
            .map(k -> new SystemMetric(k, metrics.metric(k, null)))
            .collect(Collectors.toList());
        return ResponseEntity.ok(values);
    }

    @GetMapping("/metrics/{id}")
    public ResponseEntity<SystemMetric> metric(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String id
    ) {
        MetricResponse metric = metrics.metric(id, null);
        return ResponseEntity.ok(new SystemMetric(id, metric));
    }

    /*
     * Realms
     */

    @GetMapping("/realms")
    public Page<Realm> getRealms(@RequestParam(required = false) String q, Pageable pageRequest) {
        // fix pageable unsupported sort via whitelist
        // also sort with slug by default
        List<Order> orders = pageRequest
            .getSort()
            .filter(o -> Arrays.asList(SORT_WHITELIST).contains(o.getProperty()))
            .toList();
        Sort sort = orders.isEmpty() ? Sort.by("slug") : Sort.by(orders);
        Pageable pg = PageRequest.of(pageRequest.getPageNumber(), pageRequest.getPageSize(), sort);

        return realmManager.searchRealms(q, pg);
    }

    @GetMapping("/realms/{slug}")
    public Realm getRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
        throws NoSuchRealmException {
        Realm realm = realmManager.getRealm(slug);
        // make sure config is clear
        realm.setOAuthConfiguration(null);

        return realm;
    }

    @PostMapping("/realms")
    public Realm addRealm(@RequestBody @Valid @NotNull Realm reg) throws RegistrationException {
        return realmManager.addRealm(reg);
    }

    @PutMapping("/realms/{slug}")
    public Realm updateRealm(
        @PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug,
        @RequestBody @Valid @NotNull Realm reg
    ) throws NoSuchRealmException, RegistrationException {
        Realm realm = realmManager.updateRealm(slug, reg);
        // make sure config is clear
        realm.setOAuthConfiguration(null);

        return realm;
    }

    @DeleteMapping("/realms/{slug}")
    public void deleteRealm(@PathVariable @Valid @NotNull @Pattern(regexp = SystemKeys.SLUG_PATTERN) String slug)
        throws NoSuchRealmException {
        realmManager.deleteRealm(slug, true);
    }

    public static class SystemMetric {

        private final String id;

        @JsonUnwrapped
        private final MetricResponse metric;

        public SystemMetric(String id, MetricResponse metric) {
            this.id = id;
            this.metric = metric;
        }

        public MetricResponse getMetric() {
            return metric;
        }

        public String getId() {
            return id;
        }

        public MetricsEndpoint.Sample getSample() {
            return metric != null && metric.getMeasurements() != null
                ? metric.getMeasurements().stream().findFirst().orElse(null)
                : null;
        }
    }
}
