package it.smartcommunitylab.aac.spid.auth;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/*
 * Validation for time formats in SAML responses.
 * SAML asses that time values "MUST be expressed in UTC form, with no time zone component".
 * In particular, official SPID specifications uses as format "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
 * while SPID validator uses "yyyy-MM-dd'T'HH:mm:ssz".
 */
public class SpidInstantValidationUtils {

    /*
     * SPID requirements are not well defined, official specifications uses as format "yyyy-MM-dd'T'HH:mm:ss.SSSz", while spid validator uses "yyyy-MM-dd'T'HH:mm:ssz"
     */
    public static boolean isInstantFormatValid(String instant) {
        return (isInstantUTCFormatWithMilliseconds(instant) || isInstantUTCFormatWithSeconds(instant));
    }

    public static boolean isInstantUTCFormatWithSeconds(String instant) {
        try {
            LocalDateTime.parse(instant, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz"));
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }

    public static boolean isInstantUTCFormatWithMilliseconds(String instant) {
        try {
            LocalDateTime.parse(instant, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz"));
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }
}
