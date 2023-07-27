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

package it.smartcommunitylab.aac.repository;

import java.util.Base64;
import javax.persistence.AttributeConverter;

public class StringBase64Converter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String input) {
        String result = null;
        if (input != null) {
            result = Base64.getEncoder().withoutPadding().encodeToString(input.getBytes());
        }
        return result;
    }

    @Override
    public String convertToEntityAttribute(String input) {
        String result = null;
        if (input != null) {
            result = new String(Base64.getDecoder().decode(input.getBytes()));
        }
        return result;
    }
}
