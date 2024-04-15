/**
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

package it.smartcommunitylab.aac.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import it.smartcommunitylab.aac.audit.store.AuditApplicationEventMixIns;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ExitCodeEvent;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.context.ApplicationEvent;

public class AuditStoreTests {

    //use CBOR by default as mapper
    private final ObjectMapper mapper = new CBORMapper()
        .registerModule(new JavaTimeModule())
        //include only non-null fields
        .setSerializationInclusion(Include.NON_NULL)
        //add mixin for including typeInfo in events
        .addMixIn(ApplicationEvent.class, AuditApplicationEventMixIns.class);
    private final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

    @BeforeEach
    public void setUp() {}

    @Test
    void mapperTests() throws Exception {
        String test = "test";
        ApplicationEvent event = new ExitCodeEvent(test, 0);
        AuditEvent auditEvent = new AuditEvent(test, test, Collections.singletonMap("event", event));
        Map<String, Object> data = mapper.convertValue(auditEvent, typeRef);
        System.out.println(data.toString());
    }
}
