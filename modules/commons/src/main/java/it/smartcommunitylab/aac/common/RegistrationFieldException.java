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

package it.smartcommunitylab.aac.common;

import it.smartcommunitylab.aac.SystemKeys;

/**
 * @author raman
 *
 */
public class RegistrationFieldException extends RegistrationException {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    private final String field;

    public RegistrationFieldException(String field) {
        super("invalid_field");
        this.field = field;
    }

    public RegistrationFieldException(String field, String message) {
        super(message);
        this.field = field;
    }

    public RegistrationFieldException(String error, String field, String message) {
        super(error, message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
