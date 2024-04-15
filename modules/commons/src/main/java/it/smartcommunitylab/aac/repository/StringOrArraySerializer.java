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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.smartcommunitylab.aac.SystemKeys;
import java.io.IOException;
import java.util.Set;

public class StringOrArraySerializer extends StdSerializer<Set<String>> {

    private static final long serialVersionUID = SystemKeys.AAC_COMMON_SERIAL_VERSION;

    public StringOrArraySerializer() {
        super(Set.class, false);
    }

    @Override
    public void serialize(Set<String> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value != null) {
            if (value.size() == 1) {
                gen.writeString(value.iterator().next());
            } else {
                gen.writeStartArray();
                for (String s : value) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
        }
    }
}
