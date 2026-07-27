package it.smartcommunitylab.aac.otp.persistence;

import java.util.List;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

public interface InternalUserOtpEntityRepository
    extends CustomJpaRepository<InternalUserOtpEntity, String>, DetachableJpaRepository<InternalUserOtpEntity> {
    List<InternalUserOtpEntity> findByRepositoryId(String repositoryId);

    List<InternalUserOtpEntity> findByRealm(String realm);

    List<InternalUserOtpEntity> findByUserId(String userId);

    InternalUserOtpEntity findByTokenAndRealm(String token, String realm);

    InternalUserOtpEntity findByTokenAndProviderId(String token, String providerId);

    List<InternalUserOtpEntity> findByRepositoryIdAndUserId(String repositoryId, String userId);

    List<InternalUserOtpEntity> findByRepositoryIdAndUserIdOrderByExpiryTimestampDesc(
        String repositoryId,
        String userId
    );

    List<InternalUserOtpEntity> findByRepositoryIdAndConsumed(String repositoryId, boolean consumed);

    List<InternalUserOtpEntity> findByConsumed(boolean consumed);

    List<InternalUserOtpEntity> findByAttemptsGreaterThanEqual(int attempts);

    List<InternalUserOtpEntity> findByExpiryTimestampLessThan(Long timestamp);
}
