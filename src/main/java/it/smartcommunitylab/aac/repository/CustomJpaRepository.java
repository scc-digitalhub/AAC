package it.smartcommunitylab.aac.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CustomJpaRepository<T, ID> extends JpaRepository<T, ID> {

    // add findOne method as workaround
    // TODO update all services/methods

    default T findOne(ID id) {
        return (T) findById(id).orElse(null);
    }
}
