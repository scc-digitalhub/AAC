package it.smartcommunitylab.aac.otp.service;

import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntity;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OtpCleanupScheduler {

    private final InternalUserOtpEntityRepository repository;

    public OtpCleanupScheduler(InternalUserOtpEntityRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanup() {
        // avoid deletion of recent tokens, we keep a buffer of 1 hour
        Long buffer = System.currentTimeMillis() - 3600000;

        List<InternalUserOtpEntity> expired = repository.findByExpiryTimestampLessThan(buffer);
        repository.deleteAll(expired);

        // remove consumed or failed tokens
        List<InternalUserOtpEntity> consumed = repository.findByConsumed(true);
        repository.deleteAll(consumed);

        List<InternalUserOtpEntity> highAttempts = repository.findByAttemptsGreaterThanEqual(3);
        repository.deleteAll(highAttempts);
    }
}
