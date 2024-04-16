package it.smartcommunitylab.aac.spid.registry;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class SpidDataModel {

    @JsonProperty("data")
    private List<SpidIdpModel> dataModel;

    public SpidDataModel() {}

    public SpidDataModel(List<SpidIdpModel> dataModel) {
        this.dataModel = dataModel;
    }

    public List<SpidIdpModel> getDataModel() {
        return dataModel;
    }

    @Override
    public String toString() {
        return "SpidDataModel{" + "dataModel=" + dataModel + '}';
    }
}
