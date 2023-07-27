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

package it.smartcommunitylab.aac.oauth;

import it.smartcommunitylab.aac.bootstrap.BootstrapConfig;
import it.smartcommunitylab.aac.dto.RealmConfig;
import org.springframework.util.Assert;

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
