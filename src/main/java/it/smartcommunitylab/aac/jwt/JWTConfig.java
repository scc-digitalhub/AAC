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

package it.smartcommunitylab.aac.jwt;

import it.smartcommunitylab.aac.oauth.model.EncryptionMethod;
import it.smartcommunitylab.aac.oauth.model.JWEAlgorithm;
import it.smartcommunitylab.aac.oauth.model.JWSAlgorithm;

public class JWTConfig {

    private JWSAlgorithm signAlgorithm;
    private JWEAlgorithm encAlgorithm;
    private EncryptionMethod encMethod;

    public JWSAlgorithm getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(JWSAlgorithm signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public JWEAlgorithm getEncAlgorithm() {
        return encAlgorithm;
    }

    public void setEncAlgorithm(JWEAlgorithm encAlgorithm) {
        this.encAlgorithm = encAlgorithm;
    }

    public EncryptionMethod getEncMethod() {
        return encMethod;
    }

    public void setEncMethod(EncryptionMethod encMethod) {
        this.encMethod = encMethod;
    }
}
