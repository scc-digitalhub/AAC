/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
