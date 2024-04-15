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

package it.smartcommunitylab.aac.claims;

import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;

/*
 * A claim describes an assertion about a principal made by an actor.
 *
 * The assertion needs to have a valid type and a non empty value to be used.
 */

public interface Claim {
    // the claim key should be unique under the given namespace
    // when multiple are provided, the result will be a collection
    public String getKey();

    public AttributeType getType();

    // a claim can be namespaced. When empty claims will be merged top level
    // TODO remove, claimsSet are namespaced, single claim need to belong to a
    // claimSet to be exposed
    public String getNamespace();

    // a humane readable name for this claim
    // TODO evaluate removal
    public String getName();

    public String getDescription();

    //    // a flag signaling if this claim is single or multiple use
    //    // TODO evaluate removal from here, should be left to definitions
    //    public boolean isMultiple();

    // the value in a serializable format, ready for store or translation
    public Serializable getValue();
}
