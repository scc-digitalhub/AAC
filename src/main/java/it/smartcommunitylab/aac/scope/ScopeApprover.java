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

package it.smartcommunitylab.aac.scope;

import it.smartcommunitylab.aac.common.InvalidDefinitionException;
import it.smartcommunitylab.aac.common.SystemException;
import it.smartcommunitylab.aac.core.ClientDetails;
import it.smartcommunitylab.aac.users.model.User;
import java.util.Collection;
import org.springframework.security.oauth2.provider.approval.Approval;

/*
 * A scope approver can decide if the requested scope can be obtained by applying any reasoning.
 *
 * When the approver can make a decision, the result will contain the response (approve/deny).
 * An approve will authorize the release of the scope, while a deny will make the request fail.
 *
 * Returning <null> will indicate that the approver can't decide, or is willing to let the request continue without this scope.
 * This happens because scopes without approval will be dropped from the request.
 *
 * Do note that we don't make assumptions on the actual implementation, the registry won't keep a copy of the approver
 * but ask the providers for every request.
 */

public interface ScopeApprover {
    public String getRealm();

    public Approval approveUserScope(String scope, User user, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException;

    public Approval approveClientScope(String scope, ClientDetails client, Collection<String> scopes)
        throws InvalidDefinitionException, SystemException;
}
