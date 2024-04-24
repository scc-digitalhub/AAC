package it.smartcommunitylab.aac.spid.auth;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SpidInstantValidationUtils {

    /*
     * SPID requirements are not well defined, official specifications uses as format "yyyy-MM-dd'T'HH:mm:ss.SSSz", while spid validator uses "yyyy-MM-dd'T'HH:mm:ssz"
     */
    public static boolean isInstantFormatValid(String instant) {
        return (isInstantIsoFormatWithMilliseconds(instant) || isInstantIsoFormat(instant));
    }

    public static boolean isInstantIsoFormat(String instant) {
        try {
            LocalDateTime.parse(instant, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz"));
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }

    public static boolean isInstantIsoFormatWithMilliseconds(String instant) {
        try {
            LocalDateTime.parse(instant, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSz"));
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }
}
