package it.smartcommunitylab.aac.core.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.smartcommunitylab.aac.core.persistence.UserEntity;
import it.smartcommunitylab.aac.core.persistence.UserEntityRepository;

@Service
public class UserEntityService {

    @Autowired
    private UserEntityRepository userRepository;

    public UserEntity createUser() {

        // generate random
        // TODO ensure unique on multi node deploy
        // (given that UUID is derived from timestamp we consider this safe enough)
        String uuid = UUID.randomUUID().toString();
        UserEntity u = new UserEntity(uuid);

        return u;
    }

    public UserEntity addUser(UserEntity u) {
        u = userRepository.save(u);
        return u;
    }

    public UserEntity addUser(String uuid, String username) {
        UserEntity u = new UserEntity(uuid);
        u.setUsername(username);
        u = userRepository.save(u);
        return u;
    }

    public UserEntity getUser(String uuid) {
        return userRepository.findByUuid(uuid);
    }

    public UserEntity updateUser(String uuid, String username) {
        UserEntity u = userRepository.findByUuid(uuid);

        u.setUsername(username);
        u = userRepository.saveAndFlush(u);
        return u;

    }

    public UserEntity deleteUser(String uuid) {
        UserEntity u = userRepository.findByUuid(uuid);
        if (u != null) {
            userRepository.delete(u);
        }

        return u;
    }

}
