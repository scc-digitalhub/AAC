package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpidMetadataSsoLocationModel {

    @JsonProperty("Binding")
    private String binding;

    @JsonProperty("Location")
    private String location;

    public SpidMetadataSsoLocationModel() {}

    public SpidMetadataSsoLocationModel(String binding, String location) {
        this.binding = binding;
        this.location = location;
    }

    public String getBinding() {
        return binding;
    }

    public String getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "SpidSsoLocationModel{" + "binding='" + binding + '\'' + ", location='" + location + '\'' + '}';
    }
}
