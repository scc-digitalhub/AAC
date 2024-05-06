///*
// * Copyright 2024 the original author or authors
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package it.smartcommunitylab.aac.spid.auth;
//
//import it.smartcommunitylab.aac.spid.model.SpidError;
//import org.opensaml.saml.saml2.core.Assertion;
//import org.opensaml.saml.saml2.core.Issuer;
//import org.opensaml.saml.saml2.core.NameIDType;
//import org.opensaml.saml.saml2.core.Response;
//import org.opensaml.saml.saml2.core.Status;
//import org.opensaml.saml.saml2.core.StatusCode;
//import org.opensaml.saml.saml2.core.Subject;
//import org.springframework.security.saml2.core.OpenSamlInitializationService;
//import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
//import org.springframework.util.CollectionUtils;
//
//public class SpidResponseValidator {
//    static {
//        OpenSamlInitializationService.initialize();
//    }
//
//    public SpidResponseValidator() {}
//
//    public void validateResponse(OpenSaml4AuthenticationProvider.ResponseToken responseToken)
//        throws SpidAuthenticationException {
//        Response response = responseToken.getResponse();
//        //        Saml2AuthenticationToken token = responseToken.getToken();
//        //        String saml2Response = token.getSaml2Response();
//
//        // evaluate status
//        Status status = response.getStatus();
//        if (status == null) {
//            throw new SpidAuthenticationException(SpidError.SYSTEM_ERROR);
//        }
//        String statusCode = status.getStatusCode().getValue();
//
//        //        if (StatusCode.REQUESTER.equals(status.getStatusCode().getValue())) {
//        //            throw new SpidResponseException(SpidError.MALFORMED_RESPONSE_DATA);
//        //        }
//        if (StatusCode.VERSION_MISMATCH.equals(statusCode)) {
//            throw new SpidAuthenticationException(SpidError.UNKNOWN_RESPONSE_CLASS);
//        }
//
//        // check assertion
//        Assertion assertion = CollectionUtils.firstElement(response.getAssertions());
//        if (assertion == null) {
//            throw new SpidAuthenticationException(SpidError.MALFORMED_RESPONSE_DATA);
//        }
//
//        // check issuer
//        Issuer issuer = assertion.getIssuer();
//        if (issuer == null) {
//            throw new SpidAuthenticationException(SpidError.INVALID_ISSUER);
//        }
//
//        // check name format
//        Subject subject = assertion.getSubject();
//        if (subject == null) {
//            throw new SpidAuthenticationException(SpidError.MALFORMED_RESPONSE_DATA);
//        }
//
//        if (!NameIDType.TRANSIENT.equals(assertion.getSubject().getNameID().getFormat())) {
//            throw new SpidAuthenticationException(SpidError.INVALID_NAMEFORMAT);
//        }
//
//        // finally fail on no success
//        if (!StatusCode.SUCCESS.equals(statusCode)) {
//            throw new SpidAuthenticationException(SpidError.SYSTEM_ERROR);
//        }
//    }
//}
