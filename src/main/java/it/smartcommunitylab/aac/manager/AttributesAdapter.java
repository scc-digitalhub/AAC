/**
 *    Copyright 2015-2019 Smart Community Lab, FBK
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
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.smartcommunitylab.aac.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import it.smartcommunitylab.aac.Config;
import it.smartcommunitylab.aac.authority.AuthorityHandler;
import it.smartcommunitylab.aac.authority.AuthorityHandlerContainer;
import it.smartcommunitylab.aac.jaxbmodel.Authorities;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMapping;
import it.smartcommunitylab.aac.jaxbmodel.AuthorityMatching;
import it.smartcommunitylab.aac.jaxbmodel.Match;
import it.smartcommunitylab.aac.model.Attribute;
import it.smartcommunitylab.aac.model.Authority;
import it.smartcommunitylab.aac.repository.AuthorityRepository;
import it.smartcommunitylab.aac.utils.Utils;

/**
 * This class manages all operations on attributes.
 * 
 */
@Component
public class AttributesAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private AuthorityRepository authorityRepository;
	private Map<String, AuthorityMapping> authorities;
	private Map<String, Set<String>> identityAttributes;
	private Multimap<String,AuthorityMatching> authorityMatches;

	@Autowired
	private AuthorityHandlerContainer authorityHandlerManager;
	@Autowired
//	private NativeAuthorityHandlerContainer nativeAuthorityHandlerManager;
	
    @Value("${authorities.enabled}")
    private String[] enabledAuthoritiesArray;

	/**
	 * Load attributes from the XML descriptor.
	 * @throws JAXBException
	 */
	@PostConstruct
	public void init() throws JAXBException {
		JAXBContext jaxb = JAXBContext.newInstance(AuthorityMapping.class,
				Authorities.class);
		Unmarshaller unm = jaxb.createUnmarshaller();
		JAXBElement<Authorities> element = (JAXBElement<Authorities>) unm
				.unmarshal(
						new StreamSource(getClass().getResourceAsStream(
								"authorities.xml")), Authorities.class);
		Authorities auths = element.getValue();
		authorities = new HashMap<String, AuthorityMapping>();
		authorityMatches = ArrayListMultimap.create();//new HashMap<String, AuthorityMatching>();
		identityAttributes = new HashMap<String, Set<String>>();
		Set<String> enabledAuthorities = new HashSet<>();
		for (String a : enabledAuthoritiesArray) enabledAuthorities.add(a.trim());
		
		for (AuthorityMapping mapping : auths.getAuthorityMapping()) {
		    //check if enabled in config
		    if(enabledAuthorities.contains(mapping.getName())) {
    			Authority auth = authorityRepository.findOne(mapping.getName());
    			if (auth == null) {
    				auth = new Authority();
    				auth.setName(mapping.getName());
    				auth.setRedirectUrl(mapping.getUrl());
    				authorityRepository.saveAndFlush(auth);
    //				authorityRepository.save(auth);
    //				authorityRepository.flush();
    			}
    			authorities.put(mapping.getName(), mapping);
    			Set<String> identities = new HashSet<String>();
    			if (mapping.getIdentifyingAttributes() != null) {
    				identities.addAll(mapping.getIdentifyingAttributes());
    			}
    			identityAttributes.put(auth.getName(), identities);
		    }
		}
        logger.debug("active authorities "+authorities.keySet().toString());

		if (auths.getAuthorityMatching() != null) {
			for (AuthorityMatching am : auths.getAuthorityMatching()) {
				for (Match match : am.getAuthority()) {
					authorityMatches.put(match.getName(), am);
				}
			}
		}
	}

	/**
	 * Retrieve from http request the attribute values of a specified authority
	 * 
	 * @param authority
	 *            the authority specified
	 * @param map 
	 * 			  the original request parameters in addition to the current request ones	
	 * @param request
	 *            the http request to process
	 * @return a map of user attributes
	 */
	public Map<String, String> getAttributes(String authority,
			Map<String, String> map, HttpServletRequest request) {
		AuthorityMapping mapping = authorities.get(authority);
		if (mapping == null) {
			throw new IllegalArgumentException("Unsupported authority: "
					+ authority);
		}
		
		AuthorityHandler handler = authorityHandlerManager.getAuthorityHandler(authority);
//		if (!handler.extractAttributes(request)) throw new SecurityException("Invalid attributes for authority "+authority);
		
		Map<String, String> attrs = handler.extractAttributes(request, map, mapping);
		if (attrs.get(Config.USER_ATTR_USERNAME) == null) {
			attrs.put(Config.USER_ATTR_USERNAME, handler.extractUsername(attrs));
		}
		return attrs;
	}

	/**
	 * Returns the list of the of the identity attributes of an authority as declared in configuration.
	 * 
	 * @param authority
	 *            the authority
	 * @return the list of identifying attributes for the given authority
	 */
	private List<String> getIdentifyingAttributes(String authority) {
		AuthorityMapping mapping = authorities.get(authority);
		if (mapping == null) {
			throw new IllegalArgumentException("Unsupported authority: "
					+ authority);
		}
		return mapping.getIdentifyingAttributes();
	}

	/**
	 * Returns the authorities available
	 * 
	 * @return the map of authorities, the key is authority name and the value
	 *         is its url
	 */

	public Map<String, String> getAuthorityUrls() {
		Map<String, String> map = new HashMap<String, String>();
		for (AuthorityMapping mapping : authorities.values()) {
			map.put(mapping.getName(), mapping.getUrl());
		}
		return map;
	}

	/**
	 * @return the authorities to show on any web-based login
	 */
	public Map<String, String> getWebAuthorityUrls() {
		Map<String, String> map = new HashMap<String, String>();
		for (AuthorityMapping mapping : authorities.values()) {
			if (mapping.isWeb()) {
				map.put(mapping.getName(), Utils.filterRedirectURL(mapping.getName()));
			}
		}
		return map;
	}
	
	/**
	 * @param a
	 * @return true if the specified attribute is the identity attribute for the authority
	 */
	public boolean isIdentityAttr(Attribute a) {
		return identityAttributes.containsKey(a.getAuthority().getName()) && 
				identityAttributes.get(a.getAuthority().getName()).contains(a.getKey());
	}

	/**
	 * @param key
	 * @return
	 */
	public AuthorityMapping getAuthority(String key) {
		return authorities.get(key);
	}

	/**
	 * Return the list that contains an attribute for each authority matching the specified one.
	 * @param auth
	 * @param attributes
	 * @return
	 */
	public List<Attribute> findAllIdentityAttributes(Authority auth, Map<String, String> attributes, boolean all) {
		Collection<AuthorityMatching> matchings = authorityMatches.get(auth.getName());
		List<Attribute> list = new ArrayList<Attribute>();
		
		if (all && matchings != null && !matchings.isEmpty()) {
			for (AuthorityMatching matching : matchings) {
				String value = null;
				for (Match match : matching.getAuthority()) {
					if (auth.getName().equals(match.getName())) {
						value = attributes.get(match.getAttribute());
					}
				}
				if (value != null) {
					for (Match match : matching.getAuthority()) {
						if (match.getName().equals(auth.getName())) continue;
						if (!authorities.containsKey(match.getName())) continue;
						
						Attribute a = new Attribute();
						a.setAuthority(authorityRepository.findOne(match.getName()));
						a.setKey(match.getAttribute());
						a.setValue(value);
						list.add(a);
					}
				}
			}
		}
		List<String> ids = getIdentifyingAttributes(auth.getName());
		for (String key : ids) {
			if (!attributes.containsKey(key)) {
				throw new IllegalArgumentException("The required attribute is missing: " + key);
			}
			Attribute a = new Attribute();
			a.setAuthority(auth);
			a.setKey(key);
			a.setValue(attributes.get(key));
			list.add(a);
		}
		
//		if (matching == null) {
//			List<String> ids = getIdentifyingAttributes(auth.getName());
//			for (String key : ids) {
//				if (!attributes.containsKey(key)) {
//					throw new IllegalArgumentException("The required attribute is missing: " + key);
//				}
//				Attribute a = new Attribute();
//				a.setAuthority(auth);
//				a.setKey(key);
//				a.setValue(attributes.get(key));
//				list.add(a);
//			}
//		} else {,
//			String value = null;
//			for (Match match : matching.getAuthority()) {
//				if (auth.getName().equals(match.getName())) {
//					if (!attributes.containsKey(match.getAttribute())) {
//						throw new IllegalArgumentException("The required match attribute is missing: " + match.getAttribute());
//					}
//					value = attributes.get(match.getAttribute());
//				}
//			}
//			if (value == null) {
//				throw new IllegalArgumentException("The required match attribute value is null (authority "+auth.getName()+")");
//			}
//			for (Match match : matching.getAuthority()) {
//				Attribute a = new Attribute();
//				a.setAuthority(authorityRepository.findOne(match.getName()));
//				a.setKey(match.getAttribute());
//				a.setValue(value);
//				list.add(a);
//			}
//		}
		return list;
	}

//	/**
//	 * @param name
//	 * @param token
//	 * @param params
//	 * @return
//	 */
//	public Map<String, String> getNativeAttributes(String authority, String token, Map<String, String> params) {
//		NativeAuthorityHandler authorityHandler = nativeAuthorityHandlerManager.getAuthorityHandler(authority);
//		if (authorityHandler == null) {
//			throw new InvalidGrantException("Invalid authority: "+ authority);
//		}
//		AuthorityMapping mapping = authorities.get(authority);
//		if (mapping == null) {
//			throw new InvalidGrantException("Unsupported authority: " + authority);
//		}
//		Map<String, String> extracted = authorityHandler.extractAttributes(token, params, mapping);
//		if (extracted.get(Config.USER_ATTR_USERNAME) == null) {
//			extracted.put(Config.USER_ATTR_USERNAME, authorityHandler.extractUsername(extracted));
//		}
//		return extracted;
//	}

}
