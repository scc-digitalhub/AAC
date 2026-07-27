package it.smartcommunitylab.aac.otp.service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import it.smartcommunitylab.aac.common.DuplicatedDataException;
import it.smartcommunitylab.aac.common.NoSuchCredentialException;
import it.smartcommunitylab.aac.common.RegistrationException;
import it.smartcommunitylab.aac.credentials.persistence.UserCredentialsService;
import it.smartcommunitylab.aac.otp.model.InternalUserOtp;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntity;
import it.smartcommunitylab.aac.otp.persistence.InternalUserOtpEntityRepository;

@Service("otpUserCredentialsService")
@Validated
@Transactional
public class OtpUserCredentialsService implements UserCredentialsService<InternalUserOtp> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final InternalUserOtpEntityRepository otpRepository;

    public OtpUserCredentialsService(InternalUserOtpEntityRepository otpRepository) {
        Assert.notNull(otpRepository, "otp repository is mandatory");
        this.otpRepository = otpRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentials(@NotNull String repositoryId) {
        logger.debug("find credentials for repository {}", String.valueOf(repositoryId));

        List<InternalUserOtpEntity> credentials = otpRepository.findByRepositoryId(repositoryId);
        return credentials.stream().map(this::to).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentialsByRealm(@NotNull String realm) {
        logger.debug("find credentials for realm {}", String.valueOf(realm));

        List<InternalUserOtpEntity> credentials = otpRepository.findByRealm(realm);
        return credentials.stream().map(this::to).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserOtp findCredentialsById(@NotNull String repository, @NotNull String id) {
        logger.debug("find credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        InternalUserOtpEntity credential = otpRepository.findOne(id);
        if (credential == null || !repository.equals(credential.getRepositoryId())) {
            return null;
        }

        return to(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserOtp findCredentialsByUuid(@NotNull String uuid) {
        logger.debug("find credentials with uuid {}", String.valueOf(uuid));

        InternalUserOtpEntity credential = otpRepository.findOne(uuid);
        if (credential == null) {
            return null;
        }

        return to(credential);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        logger.debug(
            "find credentials for user {} in repository {}",
            String.valueOf(userId),
            String.valueOf(repository)
        );

        List<InternalUserOtpEntity> credentials = otpRepository.findByRepositoryIdAndUserId(repository, userId);
        return credentials.stream().map(this::to).collect(Collectors.toList());
    }

    @Override
    public InternalUserOtp addCredentials(@NotNull String repository, @NotNull String id, @NotNull InternalUserOtp reg)
        throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        try {
            InternalUserOtpEntity existing = otpRepository.findOne(id);
            if (existing != null) {
                throw new DuplicatedDataException("id");
            }

            InternalUserOtpEntity credential = from(reg, id, repository);

            System.out.println("ID: " + credential.getId());
            System.out.println("RepositoryId: " + credential.getRepositoryId());
            System.out.println("UserId: " + credential.getUserId());
            System.out.println("Realm: " + credential.getRealm());
            System.out.println("ProviderId: " + credential.getProviderId()); // <- Guarda se questo è NULL!
            System.out.println("Token: " + credential.getToken());
            System.out.println("ExpiryTimestamp: " + credential.getExpiryTimestamp());

            credential = otpRepository.saveAndFlush(credential);

            InternalUserOtp result = to(credential);
            result.setAuthority(reg.getAuthority());
            result.setProvider(reg.getProvider());

            return result;
        } catch (Exception e) {
            e.printStackTrace(); // Utile per vedere se l'eccezione originale viene mascherata
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public InternalUserOtp updateCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull InternalUserOtp reg
    ) throws NoSuchCredentialException, RegistrationException {
        logger.debug("update credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) {
            throw new RegistrationException();
        }

        InternalUserOtpEntity credential = otpRepository.findOne(id);
        if (credential == null) {
            throw new NoSuchCredentialException();
        }

        try {
            credential.setRepositoryId(repository);
            credential.setUserId(reg.getUserId());
            credential.setRealm(reg.getRealm());
            credential.setToken(reg.getToken());

            Long expiry = reg.getExpiryTimestamp();
            if (expiry == null) {
                expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
            }
            credential.setExpiryTimestamp(expiry);

            credential.setAttempts(reg.getAttempts());
            credential.setConsumed(reg.isConsumed());

            if (reg.getProvider() != null) {
                credential.setProviderId(reg.getProvider());
            }

            credential = otpRepository.saveAndFlush(credential);

            InternalUserOtp result = to(credential);
            result.setAuthority(reg.getAuthority());
            result.setProvider(reg.getProvider());

            return result;
        } catch (Exception e) {
            throw new RegistrationException(e.getMessage());
        }
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        InternalUserOtpEntity credential = otpRepository.findOne(id);
        if (credential != null && repository.equals(credential.getRepositoryId())) {
            otpRepository.delete(credential);
        }
    }

    @Override
    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> ids) {
        for (String id : ids) {
            deleteCredentials(repository, id);
        }
    }

    @Override
    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        List<InternalUserOtpEntity> credentials = otpRepository.findByRepositoryIdAndUserId(repository, userId);
        if (!credentials.isEmpty()) {
            otpRepository.deleteAll(credentials);
        }
    }

    private InternalUserOtpEntity from(InternalUserOtp reg, String id, String repository) {
        InternalUserOtpEntity credential = new InternalUserOtpEntity();
        credential.setId(id != null ? id : UUID.randomUUID().toString());
        credential.setRepositoryId(repository);
        credential.setUserId(reg.getUserId());
        credential.setRealm(reg.getRealm());
        credential.setToken(reg.getToken());

        Long expiry = reg.getExpiryTimestamp();
        if (expiry == null) {
            expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        }
        credential.setExpiryTimestamp(expiry);

        credential.setAttempts(reg.getAttempts());
        credential.setConsumed(reg.isConsumed());

        // Fissa providerId: se null imposta un valore di default o usa repositoryId per evitare il NotNull
        String providerId = reg.getProvider() != null ? reg.getProvider() : repository;
        credential.setProviderId(providerId);

        return credential;
    }

    private InternalUserOtp to(InternalUserOtpEntity credential) {
        InternalUserOtp reg = new InternalUserOtp(credential.getRealm(), credential.getId());
        reg.setRepositoryId(credential.getRepositoryId());
        reg.setUserId(credential.getUserId());
        reg.setToken(credential.getToken());
        reg.setExpiryTimestamp(credential.getExpiryTimestamp());
        reg.setAttempts(credential.getAttempts());
        reg.setConsumed(credential.isConsumed());
        reg.setProvider(credential.getProviderId());
        return reg;
    }
}
