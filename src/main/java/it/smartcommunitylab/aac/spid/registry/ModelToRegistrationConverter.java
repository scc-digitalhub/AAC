package it.smartcommunitylab.aac.spid.registry;

import it.smartcommunitylab.aac.spid.model.SpidRegistration;
import java.util.LinkedList;
import java.util.List;

public class ModelToRegistrationConverter {

    public static List<SpidRegistration> Convert(SpidDataModel model) {
        List<SpidRegistration> regs = new LinkedList<>();
        for (SpidIdpModel m : model.getDataModel()) {
            SpidRegistration reg = Convert(m, new IconStore());
            regs.add(reg);
        }
        return regs;
    }

    public static SpidRegistration Convert(SpidIdpModel model, IconStore icons) {
        SpidRegistration reg = new SpidRegistration();
        reg.setMetadataUrl(model.getMetadataUrl());
        reg.setEntityId(model.getEntityId());
        reg.setEntityName(model.getEntityName());
        String iconUrl = icons.GetIcon(model.getEntityId());
        if (iconUrl != null) {
            reg.setIconUrl(iconUrl);
        } else {
            // TODO
        }
        return reg;
    }
}
