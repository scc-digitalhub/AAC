package it.smartcommunitylab.aac.oauth;

import org.springframework.util.Assert;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.dto.RealmConfig;

public class OAuth2ConfigUtils {

    public static OAuth2TestConfig with(RealmConfig rc) {
        Assert.notNull(rc, "config can not be null");
        return new OAuth2TestConfig(rc);
    }

    public static OAuth2TestConfig with(BootstrapConfig config) {
        Assert.notNull(config, "config can not be null");
        RealmConfig rc = config.getRealms().iterator().next();
        if (rc == null) {
            throw new IllegalArgumentException("missing config");
        }

        return with(rc);
    }

}
