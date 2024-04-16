package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SpidMetadataExtensionsModel {

    @JsonProperty("supported_acr")
    private List<String> supportedAcr;

    @JsonProperty("supported_purpose")
    private List<String> supportedPurpose;

    public SpidMetadataExtensionsModel() {}

    public SpidMetadataExtensionsModel(List<String> supportedAcr, List<String> supportedPurpose) {
        this.supportedAcr = supportedAcr;
        this.supportedPurpose = supportedPurpose;
    }

    public List<String> getSupportedAcr() {
        return supportedAcr;
    }

    public List<String> getSupportedPurpose() {
        return supportedPurpose;
    }

    @Override
    public String toString() {
        return "SpidExtensionsModel{" + "supportedAcr=" + supportedAcr + ", supportedPurpose=" + supportedPurpose + '}';
    }
}
