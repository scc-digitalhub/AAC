package it.smartcommunitylab.aac.repository;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.Repository;

import it.smartcommunitylab.aac.internal.persistence.InternalUserAccount;

//@NoRepositoryBean
public interface DetachableJpaRepository<T> {

    <S extends T> S detach(S e);
}
