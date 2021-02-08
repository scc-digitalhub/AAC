/*******************************************************************************
 * Copyright 2015 Fondazione Bruno Kessler
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 ******************************************************************************/

package it.smartcommunitylab.aac.openid.endpoint;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.jwt.ClientKeyCacheService;
import it.smartcommunitylab.aac.jwt.JWTService;
import it.smartcommunitylab.aac.manager.RoleManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ClientDetailsEntity;
import it.smartcommunitylab.aac.model.User;
import it.smartcommunitylab.aac.openid.view.HttpCodeView;
import it.smartcommunitylab.aac.openid.view.UserInfoJWTView;
import it.smartcommunitylab.aac.openid.view.UserInfoView;
import it.smartcommunitylab.aac.repository.ClientDetailsRepository;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "OpenID Connect Core" })
public class UserInfoEndpoint {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String USERINFO_URL = "/userinfo";

    @Autowired
    private UserManager userManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private ClientDetailsRepository clientRepo;

    /**
     * Get information about the user as specified in the accessToken included in
     * this request
     */
//	@PreAuthorize("hasRole('ROLE_USER') and #oauth2.hasScope('" + Config.OPENID_SCOPE + "')")
    @ApiOperation(value = "Get info about the authenticated End-User")
    @RequestMapping(value = USERINFO_URL, method = { RequestMethod.GET, RequestMethod.POST }, produces = {
            MediaType.APPLICATION_JSON_VALUE, UserInfoJWTView.JOSE_MEDIA_TYPE_VALUE })
    public String getInfo(
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader,
            OAuth2Authentication auth, Model model) {

        if (auth == null) {
            logger.error("getInfo failed; no principal. Requester is not authorized.");
            model.addAttribute(HttpCodeView.CODE, HttpStatus.FORBIDDEN);
            return HttpCodeView.VIEWNAME;
        }

        User user = userManager.getUser();
        if (user == null) {
            logger.error("getInfo failed; user not found: " + user);
            model.addAttribute(HttpCodeView.CODE, HttpStatus.NOT_FOUND);
            return HttpCodeView.VIEWNAME;
        }

        // refresh authorities since authentication could be stale (stored in db)
        List<GrantedAuthority> userAuthorities = roleManager.buildAuthorities(user);

        model.addAttribute(UserInfoView.SCOPE, auth.getOAuth2Request().getScope());


        model.addAttribute(UserInfoView.USER_INFO, user);

        model.addAttribute(UserInfoView.SELECTED_AUTHORITIES, userAuthorities);
        
        //TODO extract requested claims as per openid spec - disabled now
//        model.addAttribute(UserInfoView.AUTHORIZED_CLAIMS, auth.getOAuth2Request().getExtensions().get("claims"));     
        model.addAttribute(UserInfoView.AUTHORIZED_CLAIMS, null);        
        model.addAttribute(UserInfoView.REQUESTED_CLAIMS, null);
        
        // content negotiation

        // start off by seeing if the client has registered for a signed/encrypted JWT
        // from here
        ClientDetailsEntity client = clientRepo.findByClientId(auth.getOAuth2Request().getClientId());
        model.addAttribute(UserInfoJWTView.CLIENT, client);

        String signedResponseAlg = jwtService.getSigningAlgorithm(client);
        String encResponseAlg = jwtService.getEncryptAlgorithm(client);
        String encResponseEnc = jwtService.getEncryptMethod(client);

        List<MediaType> mediaTypes = MediaType.parseMediaTypes(acceptHeader);
        MediaType.sortBySpecificityAndQuality(mediaTypes);

        if (StringUtils.hasText(signedResponseAlg)
                || (StringUtils.hasText(encResponseAlg)) && StringUtils.hasText(encResponseEnc)) {

            // client has a preference, see if they ask for plain JSON specifically on this
            // request
            for (MediaType m : mediaTypes) {
                if (!m.isWildcardType() && m.isCompatibleWith(UserInfoJWTView.JOSE_MEDIA_TYPE)) {
                    return UserInfoJWTView.VIEWNAME;
                } else if (!m.isWildcardType() && m.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    return UserInfoView.VIEWNAME;
                }
            }

            // otherwise return JWT
            return UserInfoJWTView.VIEWNAME;
        } else {
            // client has no preference, see if they asked for JWT specifically on this
            // request
            for (MediaType m : mediaTypes) {
                if (!m.isWildcardType() && m.isCompatibleWith(MediaType.APPLICATION_JSON)) {
                    return UserInfoView.VIEWNAME;
                } else if (!m.isWildcardType() && m.isCompatibleWith(UserInfoJWTView.JOSE_MEDIA_TYPE)) {
                    return UserInfoJWTView.VIEWNAME;
                }
            }

            // otherwise return JSON
            return UserInfoView.VIEWNAME;
        }

    }

}