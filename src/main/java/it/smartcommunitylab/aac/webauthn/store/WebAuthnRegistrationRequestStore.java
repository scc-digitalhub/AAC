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

package it.smartcommunitylab.aac.webauthn.store;

import it.smartcommunitylab.aac.webauthn.model.WebAuthnRegistrationRequest;
import java.util.Collection;

public interface WebAuthnRegistrationRequestStore {
    public WebAuthnRegistrationRequest find(String key);

    public WebAuthnRegistrationRequest consume(String key);

    public Collection<WebAuthnRegistrationRequest> findAll();

    public String store(WebAuthnRegistrationRequest request);

    public void store(WebAuthnRegistrationRequest request, String key);

    public void remove(String key);
}
