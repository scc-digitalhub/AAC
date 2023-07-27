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

package it.smartcommunitylab.aac.services;

import it.smartcommunitylab.aac.claims.ResourceClaimsExtractor;
import it.smartcommunitylab.aac.claims.ResourceClaimsExtractorProvider;
import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.common.NoSuchServiceException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class ServiceResourceClaimsExtractorProvider implements ResourceClaimsExtractorProvider {

    private final ServicesService servicesService;
    private ScriptExecutionService executionService;

    public ServiceResourceClaimsExtractorProvider(ServicesService servicesService) {
        Assert.notNull(servicesService, "services service is required");
        this.servicesService = servicesService;
    }

    public void setExecutionService(ScriptExecutionService executionService) {
        this.executionService = executionService;
    }

    @Override
    public Collection<String> getResourceIds() {
        // service namespace is resourceId
        return servicesService.listNamespaces();
    }

    @Override
    public Collection<ResourceClaimsExtractor> getExtractors() {
        List<String> namespaces = servicesService.listNamespaces();
        Set<ResourceClaimsExtractor> extractors = new HashSet<>();

        for (String namespace : namespaces) {
            try {
                Service service = servicesService.getServiceByNamespace(namespace);
                extractors.add(buildExtractor(service));
            } catch (NoSuchServiceException e) {}
        }

        return extractors;
    }

    @Override
    public ResourceClaimsExtractor getExtractor(String resourceId) {
        try {
            Service service = servicesService.getServiceByNamespace(resourceId);
            return buildExtractor(service);
        } catch (NoSuchServiceException e) {
            return null;
        }
    }

    private ResourceClaimsExtractor buildExtractor(Service service) {
        // TODO implement multiple types of extractors

        if (
            StringUtils.hasText(service.getUserClaimMapping()) || StringUtils.hasText(service.getClientClaimMapping())
        ) {
            ScriptServiceClaimExtractor e = new ScriptServiceClaimExtractor(service);
            e.setExecutionService(executionService);

            return e;
        }

        return null;
    }
}
