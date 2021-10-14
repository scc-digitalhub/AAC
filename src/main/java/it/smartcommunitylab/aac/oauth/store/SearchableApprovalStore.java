package it.smartcommunitylab.aac.oauth.store;

import java.util.Collection;

import org.springframework.security.oauth2.provider.approval.Approval;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;

public interface SearchableApprovalStore extends ApprovalStore {

    public Approval findApproval(String userId, String clientId, String scope);

    public Collection<Approval> findUserApprovals(String userId);

    public Collection<Approval> findClientApprovals(String clientId);

    public Collection<Approval> findScopeApprovals(String scope);
    
    public Collection<Approval> findUserScopeApprovals(String userId, String scope);


}
