/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
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
 ******************************************************************************/

package it.smartcommunitylab.aac.crypto;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author raman
 *
 */
public class InternalPasswordEncoder implements PasswordEncoder {

    private final PasswordHash hasher;

    public InternalPasswordEncoder() {
        this.hasher = new PasswordHash();
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return rawPassword.toString();
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        try {
            return hasher.validatePassword(rawPassword.toString(), encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
