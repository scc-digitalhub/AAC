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

package it.smartcommunitylab.aac.webauthn.model;

import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import java.io.Serializable;

public class CredentialCreationInfo implements Serializable {

    // private ByteArray userHandle;
    private String userHandle;

    // TODO handle serializable, this is NOT serializable by itself
    // private PublicKeyCredentialCreationOptions options;
    private String options;

    // public ByteArray getUserHandle() {
    //     return userHandle;
    // }

    // public void setUserHandle(ByteArray userHandle) {
    //     this.userHandle = userHandle;
    // }

    // public PublicKeyCredentialCreationOptions getOptions() {
    //     return options;
    // }

    // public void setOptions(PublicKeyCredentialCreationOptions options) {
    //     this.options = options;
    // }

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return "CredentialCreationInfo [userHandle=" + userHandle + ", options=" + options + "]";
    }
}
