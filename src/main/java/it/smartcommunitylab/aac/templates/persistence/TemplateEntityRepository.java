package it.smartcommunitylab.aac.templates.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;
import it.smartcommunitylab.aac.repository.DetachableJpaRepository;

@Repository
public interface TemplateEntityRepository extends CustomJpaRepository<TemplateEntity, String>,
        DetachableJpaRepository<TemplateEntity> {

    TemplateEntity findByAuthorityAndRealmAndTemplateAndLanguage(String authority, String realm, String template,
            String language);

    List<TemplateEntity> findByRealm(String realm);

    List<TemplateEntity> findByAuthority(String authority);

    List<TemplateEntity> findByAuthorityAndRealm(String authority, String realm);

    List<TemplateEntity> findByAuthorityAndRealmAndTemplate(String authority, String realm, String template);

    Page<TemplateEntity> findByRealm(String realm, Pageable page);

    Page<TemplateEntity> findByRealmAndAuthorityContainingIgnoreCaseOrRealmAndTemplateContainingIgnoreCase(
            String realma,
            String authority, String realmt, String template, Pageable page);

}
