package it.smartcommunitylab.aac.internal.service;

import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

public interface InternalUserConfirmKeyService {

    public InternalUserAccount findAccountByConfirmationKey(String repository, String key);

}
