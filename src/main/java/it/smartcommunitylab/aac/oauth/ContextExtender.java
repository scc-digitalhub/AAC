/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

package it.smartcommunitylab.aac.oauth;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;

/**
 * Context extension to customize the OAuth2 authorization endpoint
 * 
 * @author raman
 *
 */
public class ContextExtender implements ApplicationContextAware {

    private final ExtRedirectResolver redirectResolver;

    public ContextExtender(String applicationURL, boolean configMatchPorts, boolean configMatchSubDomains) {
        super();

        this.redirectResolver = new ExtRedirectResolver(applicationURL, configMatchPorts, configMatchSubDomains);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
//		ctx.getBean(AuthorizationEndpoint.class).setRedirectResolver(new ExtRedirectResolver(ctx.getBean(ServletContext.class)));
        ctx.getBean(AuthorizationEndpoint.class).setRedirectResolver(redirectResolver);
    }

}
