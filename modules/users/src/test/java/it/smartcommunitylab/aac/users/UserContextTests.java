/**
 * Copyright 2023 Fondazione Bruno Kessler
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

package it.smartcommunitylab.aac.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.smartcommunitylab.aac.accounts.model.UserAccount;
import it.smartcommunitylab.aac.accounts.model.UserAccountsResourceContext;
import it.smartcommunitylab.aac.identity.model.UserIdentitiesResourceContext;
import it.smartcommunitylab.aac.model.User;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;

// @SpringJUnitWebConfig
public class UserContextTests {

    private static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void userContextTest() throws Exception {
        User user = new User("user-id", "test");

        assertThat(user.getResources()).isNotNull();
    }

    // @Test
    // public void accountContextTest() throws Exception {
    //     User user = new User("user-id", "test");
    //     InternalUserAccount account = new InternalUserAccount("internal", "internal", "test", "account-id");
    //     account.setUserId("user-id");

    //     assertThat(user.getAccounts()).isEmpty();
    //     user.setAccounts(Collections.singletonList(account));

    //     assertThat(user.getAccounts()).isNotEmpty();
    //     assertThat(user.getAccounts()).contains(account);

    //     String value = mapper.writeValueAsString(user);
    //     System.out.println(value);
    // }

    @Test
    public void identitiesContextTest() throws Exception {
        User user = new User("user-id", "test");
        TestUserAccount account = TestUserAccount.builder().username("test").uuid("123123lkj123").build();
        TestUserAccount account2 = TestUserAccount.builder().username("test2").uuid("123123lkj123").build();

        user.setResources("account", List.of(account, account2));

        assertThat(user.getResources("account")).isNotEmpty();
        assertThat(user.getResources("account")).contains(account);

        String value = mapper.writeValueAsString(user);
        System.out.println(value);

        UserAccountsResourceContext ctx = UserAccountsResourceContext.from(user);
        assertThat(ctx.getAccounts()).isNotEmpty();
        assertThat(ctx.getAccounts()).contains(account);
        System.out.println(ctx.getAccounts());

        value = mapper.writeValueAsString(ctx);
        System.out.println(value);

        UserAccountsResourceContext wtx = UserAccountsResourceContext.with(user);
        wtx.setAccounts(null);

        assertThat(ctx.getAccounts()).isEmpty();
        assertThat(user.getResources("account")).isEmpty();
    }
}
