/*
 * Copyright 2024 the original author or authors
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

package it.smartcommunitylab.aac.spid.service;

import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import java.util.Collection;

/*
 * SpidRegistry is a registry of SPID certified identity providers, such as Infocert, Lepida, Poste, etc.
 * For an up to date list of certified providers, see the page
 *      https://registry.spid.gov.it/identity-providers
 * For more on a possible remote SPID registry, see
 *      https://www.agid.gov.it/sites/default/files/repository_files/spid-avviso-n42-spid_bottone.pdf
 */
public interface SpidRegistry {
    public Collection<SpidRegistration> getIdentityProviders();

    public SpidRegistration getIdentityProvider(String entityId);
}
