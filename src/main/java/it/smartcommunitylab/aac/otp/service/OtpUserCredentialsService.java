package it.smartcommunitylab.aac.otp.service;

import java.util.Collection;

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

@Service
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
    public InternalUserOtp addCredentials(@NotNull String repository, @NotNull String id, @NotNull InternalUserOtp reg)
        throws RegistrationException {
        logger.debug("add credentials with id {} in repository {}", String.valueOf(id), String.valueOf(repository));

        if (reg == null) throw new RegistrationException();

        try {
            InternalUserOtpEntity existing = otpRepository.findOne(id);

            if (existing != null) throw new DuplicatedDataException("id");

            InternalUserOtpEntity credential = from(reg, id, repository);

            credential = otpRepository.saveAndFlush(credential);

            InternalUserOtp result = to(credential);
            result.setAuthority(reg.getAuthority());
            result.setProvider(reg.getProvider());

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RegistrationException(e.getMessage());
        }
    }

    private InternalUserOtpEntity from(InternalUserOtp reg, String id, String repository) {
        InternalUserOtpEntity credential = new InternalUserOtpEntity();
        credential.setId(id);
        credential.setRepositoryId(repository);
        credential.setUserId(reg.getUserId());
        credential.setRealm(reg.getRealm());
        credential.setToken(reg.getToken());
        credential.setExpiryTimestamp(reg.getExpiryTimestamp());
        credential.setProviderId(reg.getProvider());

        return credential;
    }

    private InternalUserOtp to(InternalUserOtpEntity credential) {
        InternalUserOtp reg = new InternalUserOtp(credential.getRealm(), credential.getId());
        reg.setRepositoryId(credential.getRepositoryId());
        reg.setUserId(credential.getUserId());
        reg.setToken(credential.getToken());
        reg.setExpiryTimestamp(credential.getExpiryTimestamp());
        reg.setProvider(credential.getProviderId());

        return reg;
    }

    // Unused methods
    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentials(@NotNull String repositoryId) {
        throw new UnsupportedOperationException("Unimplemented method 'findCredentials'");
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentialsByRealm(@NotNull String realm) {
        throw new UnsupportedOperationException("Unimplemented method 'findCredentialsByRealm'");
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserOtp findCredentialsById(@NotNull String repository, @NotNull String id) {
        throw new UnsupportedOperationException("Unimplemented method 'findCredentialsById'");
    }

    @Override
    @Transactional(readOnly = true)
    public InternalUserOtp findCredentialsByUuid(@NotNull String uuid) {
        throw new UnsupportedOperationException("Unimplemented method 'findCredentialsByUuid'");
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<InternalUserOtp> findCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'findCredentialsByUser'");
    }

    @Override
    public InternalUserOtp updateCredentials(
        @NotNull String repository,
        @NotNull String id,
        @NotNull InternalUserOtp reg
    ) throws NoSuchCredentialException {
        throw new UnsupportedOperationException("Unimplemented method 'updateCredentials'");
    }

    @Override
    public void deleteCredentials(@NotNull String repository, @NotNull String id) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteCredentials'");
    }

    @Override
    public void deleteAllCredentials(@NotNull String repository, @NotNull Collection<String> ids) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllCredentials'");
    }

    @Override
    public void deleteAllCredentialsByUser(@NotNull String repository, @NotNull String userId) {
        throw new UnsupportedOperationException("Unimplemented method 'deleteAllCredentialsByUser'");
    }
}
