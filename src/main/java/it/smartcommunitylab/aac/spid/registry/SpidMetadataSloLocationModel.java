package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SpidMetadataSloLocationModel {

    @JsonProperty("Binding")
    private String Binding;

    @JsonProperty("Location")
    private String Location;

    public SpidMetadataSloLocationModel() {}

    public SpidMetadataSloLocationModel(String binding, String location) {
        Binding = binding;
        Location = location;
    }

    public String getBinding() {
        return Binding;
    }

    public String getLocation() {
        return Location;
    }

    @Override
    public String toString() {
        return "SpidSloLocationModel{" + "Binding='" + Binding + '\'' + ", Location='" + Location + '\'' + '}';
    }
}
