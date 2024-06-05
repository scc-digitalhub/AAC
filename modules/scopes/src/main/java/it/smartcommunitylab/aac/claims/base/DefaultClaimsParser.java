package it.smartcommunitylab.aac.claims.base;

import it.smartcommunitylab.aac.attributes.model.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.model.DateAttribute;
import it.smartcommunitylab.aac.attributes.model.DateTimeAttribute;
import it.smartcommunitylab.aac.attributes.model.InstantAttribute;
import it.smartcommunitylab.aac.attributes.model.NumberAttribute;
import it.smartcommunitylab.aac.attributes.model.StringAttribute;
import it.smartcommunitylab.aac.attributes.model.TimeAttribute;
import it.smartcommunitylab.aac.claims.model.BooleanClaim;
import it.smartcommunitylab.aac.claims.model.DateClaim;
import it.smartcommunitylab.aac.claims.model.DateTimeClaim;
import it.smartcommunitylab.aac.claims.model.InstantClaim;
import it.smartcommunitylab.aac.claims.model.NumberClaim;
import it.smartcommunitylab.aac.claims.model.SerializableClaim;
import it.smartcommunitylab.aac.claims.model.StringClaim;
import it.smartcommunitylab.aac.claims.model.TimeClaim;
import it.smartcommunitylab.aac.model.AttributeType;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;

public class DefaultClaimsParser implements Converter<MultiValueMap<String, Serializable>, List<AbstractClaim>> {

    private List<? extends AbstractClaimDefinition> definitions;

    public DefaultClaimsParser() {}

    public DefaultClaimsParser(AbstractClaimDefinition... definitions) {
        this(Arrays.asList(definitions));
    }

    public DefaultClaimsParser(Collection<? extends AbstractClaimDefinition> definitions) {
        Assert.notNull(definitions, "definitions can not be null");
        setDefinitions(definitions);
    }

    public void setDefinitions(Collection<? extends AbstractClaimDefinition> definitions) {
        if (definitions == null) {
            definitions = Collections.emptyList();
        }

        // sort by key
        ArrayList<? extends AbstractClaimDefinition> list = new ArrayList<>(definitions);
        Collections.sort(list);
        this.definitions = Collections.unmodifiableList(list);
    }

    @Override
    public List<AbstractClaim> convert(MultiValueMap<String, Serializable> source) {
        if (source == null || definitions == null) {
            return null;
        }

        List<AbstractClaim> claims = new ArrayList<>();

        // convert based on definitions, skip everything else
        definitions.forEach(def -> {
            List<Serializable> values = source.get(def.getKey());
            if (values != null) {
                values.forEach(value -> {
                    try {
                        AbstractClaim claim = buildClaim(def.getKey(), def.getType(), value);
                        claims.add(claim);
                    } catch (ParseException e) {}
                });
            }
        });

        return claims;
    }

    private AbstractClaim buildClaim(String key, AttributeType type, Serializable value) throws ParseException {
        if (AttributeType.STRING == type) {
            return new StringClaim(key, StringAttribute.parseValue(value));
        }
        if (AttributeType.NUMBER == type) {
            return new NumberClaim(key, NumberAttribute.parseValue(value));
        }
        if (AttributeType.BOOLEAN == type) {
            return new BooleanClaim(key, BooleanAttribute.parseValue(value));
        }
        if (AttributeType.DATE == type) {
            return new DateClaim(key, DateAttribute.parseValue(value));
        }
        if (AttributeType.DATETIME == type) {
            return new DateTimeClaim(key, DateTimeAttribute.parseValue(value));
        }
        if (AttributeType.TIME == type) {
            return new TimeClaim(key, TimeAttribute.parseValue(value));
        }
        if (AttributeType.INSTANT == type) {
            return new InstantClaim(key, InstantAttribute.parseValue(value));
        }
        if (AttributeType.OBJECT == type) {
            return new SerializableClaim(key, value);
        }

        throw new ParseException("unsupported", 0);
    }
}
