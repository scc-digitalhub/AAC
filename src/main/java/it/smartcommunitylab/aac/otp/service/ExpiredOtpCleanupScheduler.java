package it.smartcommunitylab.aac.otp.service;

import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExpiredOtpCleanupScheduler {

    private final InternalUserOtpEntityRepository repository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public ExpiredOtpCleanupScheduler(InternalUserOtpEntityRepository repository) {
        this.repository = repository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void ExpiredCleanup() {
        long buffer = System.currentTimeMillis() - 3600000;

        logger.debug("Cleaning up expired OTP tokens");
        repository.deleteExpired(buffer);
        logger.debug("Finished cleanup");
    }
}
