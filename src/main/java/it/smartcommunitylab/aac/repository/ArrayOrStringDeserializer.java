package it.smartcommunitylab.aac.repository;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.type.SimpleType;


public class ArrayOrStringDeserializer extends StdDeserializer<Set<String>> {

	public ArrayOrStringDeserializer() {
		super(Set.class);
	}

	@Override
	public Set<String> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
		JsonToken token = jp.getCurrentToken();
		if (token.isScalarValue()) {
			//use regex and comma separated to handle incorrectly formatted space separated
			String list = jp.getText().replaceAll("\\s+", ",");
			return StringUtils.commaDelimitedListToSet(list);
		} else {
			return jp.readValueAs(new TypeReference<Set<String>>() {});
		}
	}

	@Override
	public JavaType getValueType() {
		return SimpleType.construct(String.class);
	}
}