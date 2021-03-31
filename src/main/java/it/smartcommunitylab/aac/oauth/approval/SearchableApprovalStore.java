package it.smartcommunitylab.aac.oauth.approval;

import java.util.Collection;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;

public interface SearchableApprovalStore extends ApprovalStore {

    public Collection<Approval> findUserApprovals(String userId);

    public Collection<Approval> findClientApprovals(String clientId);

}
