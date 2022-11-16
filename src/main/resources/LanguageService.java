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
