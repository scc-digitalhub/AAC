/*
 * Copyright 2024 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package it.smartcommunitylab.aac.oauth.service;

import java.time.Instant;
import java.util.Date;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import it.smartcommunitylab.aac.oauth.store.ExtTokenStore;

@Component
public class OAuth2Scheduler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final int DEFAULT_DELAY = 24 * 60 * 60 * 1000; //every day
    public static final int INITIAL_DELAY = 6 * 1000; //wait 60s for start


    @Value("${oauth2.accesstoken.cleanup}")
    private int accessTokenCleanupInterval;

    @Value("${oauth2.refreshtoken.cleanup}")
    private int refreshTokenCleanupInterval;

    @Autowired
    private ExtTokenStore tokenStore;

    @Scheduled(fixedDelay = DEFAULT_DELAY, initialDelay = INITIAL_DELAY)
    @Transactional
    public void deleteExpiredTokens() {
        try {
            Date now = Date.from(Instant.now());

            logger.info("remove access tokens expired before {} - {}", now, accessTokenCleanupInterval);
            tokenStore.deleteExpiredAccessTokens(accessTokenCleanupInterval);

            logger.info("remove refresh tokens expired before {} - {}", now, refreshTokenCleanupInterval);
            tokenStore.deleteExpiredRefreshTokens(refreshTokenCleanupInterval);            
        } catch (RuntimeException e) {
            logger.error("error removing expired tokens", e);
        }
    }
}
