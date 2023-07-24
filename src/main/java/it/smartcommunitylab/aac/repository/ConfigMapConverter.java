package it.smartcommunitylab.aac.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.core.model.ConfigMap;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import org.springframework.core.convert.converter.Converter;

public class ConfigMapConverter<T extends ConfigMap> implements Converter<Map<String, Serializable>, T> {

    protected static final ObjectMapper mapper = new ObjectMapper();
    //    private final TypeReference<T> classRef;
    private final JavaType type;

    //    public ConfigMapConverter(TypeReference<T> classRef) {
    //        this.classRef = classRef;
    //    }
    //

    protected ConfigMapConverter() {
        // resolve generics type via subclass trick
        Type t = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.type = mapper.getTypeFactory().constructSimpleType((Class<?>) t, null);
    }

    public ConfigMapConverter(Class<T> c) {
        this.type = mapper.getTypeFactory().constructSimpleType(c, null);
    }

    @Override
    public T convert(Map<String, Serializable> source) {
        // use mapper
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        return mapper.convertValue(source, type);
    }
}
