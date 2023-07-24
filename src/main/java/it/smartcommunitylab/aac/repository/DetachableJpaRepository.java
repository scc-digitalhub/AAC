package it.smartcommunitylab.aac.repository;

//@NoRepositoryBean
public interface DetachableJpaRepository<T> {
    <S extends T> S detach(S e);
}
