package it.smartcommunitylab.aac.claims;

public interface ResourceClaimsExtractorProvider {

    public String getResourceId();

    public ResourceClaimsExtractor getExtractor();

}
