package it.smartcommunitylab.aac.webauthn.persistence.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.yubico.webauthn.data.ByteArray;

@Converter(autoApply = true)
public class ByteArrayToStringConverter implements AttributeConverter<ByteArray, String> {

    @Override
    public String convertToDatabaseColumn(ByteArray barr) {
        return barr.getBase64();
    }

    @Override
    public ByteArray convertToEntityAttribute(String arg0) {
        return ByteArray.fromBase64(arg0);
    }
}
