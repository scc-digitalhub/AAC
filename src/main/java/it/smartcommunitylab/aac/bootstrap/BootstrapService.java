package it.smartcommunitylab.aac.bootstrap;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.Config.AUTHORITY;
import it.smartcommunitylab.aac.dto.ServiceDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceClaimDTO;
import it.smartcommunitylab.aac.dto.ServiceDTO.ServiceScopeDTO;

public class BootstrapService {

    private String serviceId;
    private String name;
    private String description;
    private String namespace;
    private String context;
    private String claimMapping;
    private BootstrapServiceScope[] scopes;
    private BootstrapServiceClaim[] claims;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getClaimMapping() {
        return claimMapping;
    }

    public void setClaimMapping(String claimMapping) {
        this.claimMapping = claimMapping;
    }

    public BootstrapServiceScope[] getScopes() {
        return scopes;
    }

    public void setScopes(BootstrapServiceScope[] scopes) {
        this.scopes = scopes;
    }

    public BootstrapServiceClaim[] getClaims() {
        return claims;
    }

    public void setClaims(BootstrapServiceClaim[] claims) {
        this.claims = claims;
    }

    /*
     * Builder
     */
    public static BootstrapService fromDTO(ServiceDTO dto) {
        BootstrapService service = new BootstrapService();
        service.serviceId = dto.getServiceId();
        service.name = dto.getName();
        service.description = dto.getDescription();
        service.namespace = dto.getNamespace();
        service.context = dto.getContext();
        service.claimMapping = "";
        service.scopes = new BootstrapServiceScope[0];
        service.claims = new BootstrapServiceClaim[0];

        if (dto.getScopes() != null) {
            service.scopes = dto.getScopes().stream()
                    .map(d -> BootstrapServiceScope.fromDTO(d))
                    .collect(Collectors.toList()).toArray(new BootstrapServiceScope[0]);
        }

        if (dto.getClaims() != null) {
            service.claims = dto.getClaims().stream()
                    .map(d -> BootstrapServiceClaim.fromDTO(d))
                    .collect(Collectors.toList()).toArray(new BootstrapServiceClaim[0]);
        }

        if (StringUtils.hasText(dto.getClaimMapping())) {
            // transform code base64encoded
            byte[] bytes = dto.getClaimMapping().getBytes(Charset.forName("UTF-8"));
            String claimMappingCode = new String(Base64.getEncoder().encode(bytes));
            service.claimMapping = claimMappingCode;
        }

        return service;
    }

    public static ServiceDTO toDTO(BootstrapService service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setServiceId(service.getServiceId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        dto.setNamespace(service.getNamespace());
        dto.setContext(service.getContext());
        dto.setClaimMapping("");
        dto.setScopes(Collections.emptyList());
        dto.setClaims(Collections.emptyList());

        if (service.getScopes() != null) {
            dto.setScopes(Arrays.stream(service.getScopes())
                    .map(d -> BootstrapServiceScope.toDTO(d))
                    .collect(Collectors.toList()));
        }

        if (service.getClaims() != null) {
            dto.setClaims(Arrays.stream(service.getClaims())
                    .map(d -> BootstrapServiceClaim.toDTO(d))
                    .collect(Collectors.toList()));
        }

        if (StringUtils.hasText(service.getClaimMapping())) {
            // transform code base64encoded
            byte[] bytes = service.getClaimMapping().getBytes(Charset.forName("UTF-8"));
            String claimMappingCode = new String(Base64.getDecoder().decode(bytes));
            dto.setClaimMapping(claimMappingCode);
        }

        return dto;
    }

    public static class BootstrapServiceScope {
        private String scope;
        private String serviceId;
        private String name;
        private String description;
        private String[] claims;
        private String[] roles;
        private String authority;
        private boolean approvalRequired = false;

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String[] getClaims() {
            return claims;
        }

        public void setClaims(String[] claims) {
            this.claims = claims;
        }

        public String[] getRoles() {
            return roles;
        }

        public void setRoles(String[] roles) {
            this.roles = roles;
        }

        public String getAuthority() {
            return authority;
        }

        public void setAuthority(String authority) {
            this.authority = authority;
        }

        public boolean isApprovalRequired() {
            return approvalRequired;
        }

        public void setApprovalRequired(boolean approvalRequired) {
            this.approvalRequired = approvalRequired;
        }

        /*
         * Builder
         */
        public static BootstrapServiceScope fromDTO(ServiceScopeDTO dto) {
            BootstrapServiceScope serviceScope = new BootstrapServiceScope();
            serviceScope.scope = dto.getScope();
            serviceScope.serviceId = dto.getServiceId();
            serviceScope.name = dto.getName();
            serviceScope.description = dto.getDescription();
            serviceScope.approvalRequired = dto.isApprovalRequired();

            serviceScope.claims = new String[0];
            serviceScope.roles = new String[0];
            serviceScope.authority = "";

            if (dto.getClaims() != null) {
                serviceScope.claims = dto.getClaims().toArray(new String[0]);
            }
            if (dto.getRoles() != null) {
                serviceScope.roles = dto.getRoles().toArray(new String[0]);
            }
            if (dto.getAuthority() != null) {
                serviceScope.authority = dto.getAuthority().name();
            }

            return serviceScope;
        }

        public static ServiceScopeDTO toDTO(BootstrapServiceScope serviceScope) {
            ServiceScopeDTO dto = new ServiceScopeDTO();
            dto.setScope(serviceScope.getScope());
            dto.setServiceId(serviceScope.getServiceId());
            dto.setName(serviceScope.getName());
            dto.setDescription(serviceScope.getDescription());
            dto.setApprovalRequired(serviceScope.isApprovalRequired());
            if(StringUtils.hasText(serviceScope.getAuthority())) {
                dto.setAuthority(AUTHORITY.valueOf(serviceScope.getAuthority()));
            } else {
                //set USER as default
                dto.setAuthority(AUTHORITY.ROLE_USER);
            }
            dto.setClaims(Collections.emptyList());
            dto.setRoles(Collections.emptyList());

            if (serviceScope.getClaims() != null) {
                dto.setClaims(Arrays.asList(serviceScope.getClaims()));
            }
            if (serviceScope.getRoles() != null) {
                dto.setRoles(Arrays.asList(serviceScope.getRoles()));
            }

            return dto;
        }

    }

    public static class BootstrapServiceClaim {
        private String claim;
        private String serviceId;
        private String name;
        private boolean multiple;
        private String type;

        public String getClaim() {
            return claim;
        }

        public void setClaim(String claim) {
            this.claim = claim;
        }

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isMultiple() {
            return multiple;
        }

        public void setMultiple(boolean multiple) {
            this.multiple = multiple;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        /*
         * Builder
         */
        public static BootstrapServiceClaim fromDTO(ServiceClaimDTO dto) {
            BootstrapServiceClaim serviceClaim = new BootstrapServiceClaim();
            serviceClaim.claim = dto.getClaim();
            serviceClaim.serviceId = dto.getServiceId();
            serviceClaim.name = dto.getName();
            serviceClaim.multiple = dto.isMultiple();
            serviceClaim.type = dto.getType();

            return serviceClaim;
        }

        public static ServiceClaimDTO toDTO(BootstrapServiceClaim serviceClaim) {
            ServiceClaimDTO dto = new ServiceClaimDTO();
            dto.setClaim(serviceClaim.getClaim());
            dto.setServiceId(serviceClaim.getServiceId());
            dto.setName(serviceClaim.getName());
            dto.setMultiple(serviceClaim.isMultiple());
            dto.setType(serviceClaim.getType());

            return dto;

        }
    }

}
