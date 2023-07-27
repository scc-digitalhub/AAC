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

package it.smartcommunitylab.aac.templates.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Service;

@Service
public class LanguageService {

    // TODO evaluate discovering available languages by inspecting resource bundles
    public static final String[] LANGUAGES = { "en", "it", "es", "lv", "de" };
    private static final Set<String> languages;

    static {
        TreeSet<String> set = new TreeSet<>(Arrays.asList(LANGUAGES));
        languages = Collections.unmodifiableSortedSet(set);
    }

    public Set<String> getLanguages() {
        return languages;
    }
}
