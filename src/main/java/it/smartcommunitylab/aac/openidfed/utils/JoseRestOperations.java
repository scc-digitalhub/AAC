/**
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

package it.smartcommunitylab.aac.openidfed.utils;

import com.nimbusds.jose.jwk.JWK;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

public class JoseRestOperations extends RestTemplate {

    public JoseRestOperations(String jwksUri) {
        super();
        //build jwt converter
        HttpMessageConverter<?> jwtHttpMessageConverter = new JwtJacksonHttpMessageConverter(jwksUri);

        //add to default converters
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.addAll(getMessageConverters());
        converters.add(jwtHttpMessageConverter);

        setMessageConverters(converters);
    }

    public JoseRestOperations(String jwksUri, JWK jweKey, String jweAlgorithm, String jweMethod) {
        super();
        //build jwt converter
        HttpMessageConverter<?> jwtHttpMessageConverter = new JwtJacksonHttpMessageConverter(
            jwksUri,
            jweKey,
            jweAlgorithm,
            jweMethod
        );

        //add to default converters
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        converters.addAll(getMessageConverters());
        converters.add(jwtHttpMessageConverter);

        setMessageConverters(converters);
    }
}
