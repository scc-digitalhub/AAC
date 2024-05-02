/*
 * Copyright 2023 the original author or authors
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

package it.smartcommunitylab.aac.attributes.mapper;

import it.smartcommunitylab.aac.attributes.OpenIdAttributesSet;
import it.smartcommunitylab.aac.attributes.model.Attribute;
import it.smartcommunitylab.aac.attributes.types.BooleanAttribute;
import it.smartcommunitylab.aac.attributes.types.DateAttribute;
import it.smartcommunitylab.aac.attributes.types.StringAttribute;
import java.util.ArrayList;
import java.util.List;

public class OpenIdAttributesMapper extends DefaultAttributesMapper {

    private static final OpenIdAttributesSet set;

    public OpenIdAttributesMapper() {
        super(set);
    }

    @Override
    public String getIdentifier() {
        return OpenIdAttributesSet.IDENTIFIER;
    }

    static {
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new StringAttribute(OpenIdAttributesSet.NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.GIVEN_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.FAMILY_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.MIDDLE_NAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.NICKNAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PREFERRED_USERNAME));
        attributes.add(new StringAttribute(OpenIdAttributesSet.EMAIL));
        attributes.add(new BooleanAttribute(OpenIdAttributesSet.EMAIL_VERIFIED));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PHONE_NUMBER));
        attributes.add(new BooleanAttribute(OpenIdAttributesSet.PHONE_NUMBER_VERIFIED));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PROFILE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.PICTURE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.WEBSITE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.GENDER));
        attributes.add(new DateAttribute(OpenIdAttributesSet.BIRTHDATE));
        attributes.add(new StringAttribute(OpenIdAttributesSet.ZONEINFO));
        attributes.add(new StringAttribute(OpenIdAttributesSet.LOCALE));

        set = new OpenIdAttributesSet(attributes);
    }
}
