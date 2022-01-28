package it.smartcommunitylab.aac.webauthn.controller;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UsernameValidator {

    /**
     * Checks if the provided object can be used as a valid username
     */
    public static boolean isValidUsername(Object username) {
        if (!(username instanceof String)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^\\w{3,30}$");
        Matcher matcher = pattern.matcher((String) username);
        return matcher.find();
    }

    /**
     * Checks if the provided object can be used as a valid display name
     */
    public static boolean isValidDisplayName(Object candidate) {
        if (!(candidate instanceof String)) {
            return false;
        }
        Pattern pattern = Pattern.compile("^[\\w ]{3,30}$");
        Matcher matcher = pattern.matcher((String) candidate);
        return matcher.find();
    }

}