package it.smartcommunitylab.aac.webauthn.persistence.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.yubico.webauthn.data.AuthenticatorTransport;

@Converter(autoApply = true)
public class AuthenticatoTransportConverter implements AttributeConverter<AuthenticatorTransport, String> {

    @Override
    public String convertToDatabaseColumn(AuthenticatorTransport category) {
        if (category == AuthenticatorTransport.USB) {
            return "USB";
        } else if (category == AuthenticatorTransport.BLE) {
            return "BLE";
        } else if (category == AuthenticatorTransport.NFC) {
            return "NFC";
        } else if (category == AuthenticatorTransport.INTERNAL) {
            return "INTERNAL";
        } else {
            throw new IllegalArgumentException("Transport not found: " + category);
        }
    }

    @Override
    public AuthenticatorTransport convertToEntityAttribute(String code) {
        if (code == null) {
            throw new IllegalArgumentException("convertToEntityAttribute called with a null transport code");
        }
        return AuthenticatorTransport.valueOf(code);
    }
}
