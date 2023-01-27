package it.smartcommunitylab.aac.services.provider;

import org.springframework.util.StringUtils;

import it.smartcommunitylab.aac.claims.ScriptExecutionService;
import it.smartcommunitylab.aac.claims.base.AbstractResourceClaimsSetExtractor;
import it.smartcommunitylab.aac.claims.base.DefaultClaimsParser;
import it.smartcommunitylab.aac.claims.extractors.ScriptClaimsExtractor;
import it.smartcommunitylab.aac.services.model.ApiService;

public class ApiServiceClaimsSetExtractor extends AbstractResourceClaimsSetExtractor<ApiService> {

    public ApiServiceClaimsSetExtractor(ApiService res, ScriptExecutionService executionService) {
        super(res);

        if (res.getClaims() != null) {
            // set extractor
            // TODO support webhook
            if (StringUtils.hasText(res.getClientClaimsExtractor())
                    || StringUtils.hasText(res.getUserClaimsExtractor())) {

                ScriptClaimsExtractor ext = new ScriptClaimsExtractor();
                ext.setExecutionService(executionService);
                ext.setClaimsParser(new DefaultClaimsParser(res.getClaims()));
                ext.setUserClaimsFunction(res.getUserClaimsExtractor());
                ext.setClientClaimsFunction(res.getClientClaimsExtractor());

                this.claimsExtractor = ext;
            }
        }
    }

}
