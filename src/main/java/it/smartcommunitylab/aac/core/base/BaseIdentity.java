package it.smartcommunitylab.aac.core.base;

import java.util.AbstractMap;
import java.util.Collection;

public abstract class BaseIdentity extends BaseAccount {

    public abstract Collection<AbstractMap.SimpleEntry<String, String>> getAttributes();
}
