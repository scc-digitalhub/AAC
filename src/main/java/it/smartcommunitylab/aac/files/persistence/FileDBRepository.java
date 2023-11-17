package it.smartcommunitylab.aac.files.persistence;

import org.springframework.stereotype.Repository;

import it.smartcommunitylab.aac.repository.CustomJpaRepository;

@Repository
public interface FileDBRepository extends CustomJpaRepository<FileDB, String> {

}
