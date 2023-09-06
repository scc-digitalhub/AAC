/*
 * Copyright 2023 the original author or authors
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

package it.smartcommunitylab.aac.utils;

public class SecurityUtils {
    //    /*
    //     * Extract clientId from OAuth token
    //     */
    //    public static String getOAuthClientId(Authentication auth) {
    //        return getOAuthClientId(auth, false);
    //    }
    //
    //    public static String getOAuthClientId(Authentication auth, boolean rejectIfUserAuth) {
    //        if (auth == null) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //
    //        if (!auth.isAuthenticated()) {
    //            throw new InsufficientAuthenticationException("The client is not authenticated.");
    //        }
    //        if (auth instanceof OAuth2Authentication) {
    //            // Might be a client and user combined authentication
    //            if (rejectIfUserAuth && ((OAuth2Authentication) auth).getUserAuthentication() != null) {
    //                throw new InsufficientAuthenticationException("Invalid token");
    //            }
    //
    //            return ((OAuth2Authentication) auth).getOAuth2Request().getClientId();
    //        }
    //        if (auth instanceof AACAuthenticationToken) {
    //            // we can not extract the clientId
    //        }
    //        throw new InsufficientAuthenticationException("Invalid authentication");
    //    }
    //
    //    /*
    //     * Extract clientId from either OAuth token or basic auth
    //     */
    //
    //    public static String getOAuthOrBasicClientId(Authentication auth, boolean rejectIfUserAuth) {
    //        if (auth == null) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //
    //        if (!auth.isAuthenticated()) {
    //            throw new InsufficientAuthenticationException("The client is not authenticated.");
    //        }
    //        if (auth instanceof OAuth2Authentication) {
    //            // Might be a client and user combined authentication
    //            if (rejectIfUserAuth && ((OAuth2Authentication) auth).getUserAuthentication() != null) {
    //                throw new InsufficientAuthenticationException("Invalid token");
    //            }
    //
    //            return ((OAuth2Authentication) auth).getOAuth2Request().getClientId();
    //        }
    //        if (auth instanceof AACAuthenticationToken) {
    //            // we can not extract the clientId
    //        }
    //        if (auth instanceof UsernamePasswordAuthenticationToken) {
    //            return auth.getName();
    //        }
    //
    //        throw new InsufficientAuthenticationException("Invalid authentication");
    //    }

    //    /*
    //     * Extract userId from OAuth token
    //     */
    //    public static String getOAuthUserId(Authentication auth) {
    //        return getOAuthUserId(auth, false);
    //    }
    //
    //    public static String getOAuthUserId(Authentication auth, boolean rejectIfClient) {
    //        if (auth == null) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //
    //        if (!auth.isAuthenticated()) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //        if (auth instanceof OAuth2Authentication) {
    //            String user = ((OAuth2Authentication) auth).getUserAuthentication().getName();
    //            String clientId = ((OAuth2Authentication) auth).getOAuth2Request().getClientId();
    //
    //            if (rejectIfClient && user.equals(clientId)) {
    //                throw new InsufficientAuthenticationException("Invalid token");
    //            }
    //
    //            return user;
    //        }
    //
    //        throw new InsufficientAuthenticationException("Invalid authentication");
    //    }
    //
    //    /*
    //     * Extract scopes from OAuth token
    //     */
    //    public static Collection<String> getOAuthScopes(Authentication auth) {
    //        if (auth == null) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //
    //        if (!auth.isAuthenticated()) {
    //            throw new InsufficientAuthenticationException("The user is not authenticated.");
    //        }
    //        if (auth instanceof OAuth2Authentication) {
    //            return ((OAuth2Authentication) auth).getOAuth2Request().getScope();
    //        }
    //
    //        return Collections.emptyList();
    //    }

}
