package it.smartcommunitylab.aac.utils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

public class YamlUtils {

    public static Yaml getInstance(boolean skipNull, Class<?>... classes) {
        DumperOptions options = getDefaultOptions();

        if (skipNull) {
            return new Yaml(getNotNullRepresenter(classes), options);
        } else {
            return new Yaml(getRepresenter(classes), options);
        }
    }

    public static DumperOptions getDefaultOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setCanonical(false);
        options.setExplicitStart(false);

        return options;
    }

    public static Representer getRepresenter(Class<?>... classes) {
        Representer representer = new Representer();

        // disable bean declaration for classes by treating as map
        for (Class<?> c : classes) {
            representer.addClassTag(c, Tag.MAP);
        }

        return representer;
    }

    /*
     * Return a representer which skips null values
     */
    public static Representer getNotNullRepresenter(Class<?>... classes) {
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(
                Object javaBean,
                Property property,
                Object propertyValue,
                Tag customTag
            ) {
                // if value of property is null, ignore it.
                if (propertyValue == null) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };

        // disable bean declaration for classes by treating as map
        for (Class<?> c : classes) {
            representer.addClassTag(c, Tag.MAP);
        }

        return representer;
    }
}
