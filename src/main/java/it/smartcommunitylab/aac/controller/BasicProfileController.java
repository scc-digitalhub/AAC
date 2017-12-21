/**
 *    Copyright 2012-2013 Trento RISE
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

package it.smartcommunitylab.aac.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import it.smartcommunitylab.aac.dto.AccountProfile;
import it.smartcommunitylab.aac.dto.AccountProfiles;
import it.smartcommunitylab.aac.dto.BasicProfile;
import it.smartcommunitylab.aac.dto.BasicProfiles;
import it.smartcommunitylab.aac.manager.BasicProfileManager;
import it.smartcommunitylab.aac.manager.UserManager;
import it.smartcommunitylab.aac.model.ErrorInfo;

/**
 * @author raman
 *
 */
@Controller
@Api(tags = { "AAC User profile" })
public class BasicProfileController {

	private Log logger = LogFactory.getLog(getClass());
	@Autowired
	private UserManager userManager;

	@Autowired
	private BasicProfileManager profileManager;

	@ApiOperation(value="Get basic profile of a user")
	@RequestMapping(method = RequestMethod.GET, value = "/basicprofile/all/{userId}")
	public @ResponseBody BasicProfile getUser(HttpServletResponse response, @PathVariable("userId") String userId)
			throws IOException {
		return profileManager.getBasicProfileById(userId);
	}

	@ApiOperation(value="Get basic profile of all users")
	@RequestMapping(method = RequestMethod.GET, value = "/basicprofile/all")
	public @ResponseBody BasicProfiles searchUsers(HttpServletResponse response,
			@RequestParam(value = "filter", required = false) String fullNameFilter) throws IOException {
		List<BasicProfile> list;
		if (fullNameFilter != null && !fullNameFilter.isEmpty()) {
			list = profileManager.getUsers(fullNameFilter);

		} else {
			list = profileManager.getUsers();
		}

		BasicProfiles profiles = new BasicProfiles();
		profiles.setProfiles(list);
		return profiles;
	}

	@ApiOperation(value="Get basic profile of a current user")
	@RequestMapping(method = RequestMethod.GET, value = "/basicprofile/me")
	public @ResponseBody BasicProfile findProfile(HttpServletResponse response) throws IOException {
		Long user = userManager.getUserId();
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		return profileManager.getBasicProfileById(user.toString());
	}

	@ApiOperation(value="Get basic profile of specified users")
	@RequestMapping(method = RequestMethod.GET, value = "/basicprofile/profiles")
	public @ResponseBody BasicProfiles findProfiles(HttpServletResponse response, @RequestParam List<String> userIds) {
		BasicProfiles profiles = new BasicProfiles();
		profiles.setProfiles(profileManager.getUsers(userIds));
		return profiles;
	}

	@ApiOperation(value="Get account data of a current user")
	@RequestMapping(method = RequestMethod.GET, value = "/accountprofile/me")
	public @ResponseBody AccountProfile findAccountProfile(HttpServletResponse response) throws IOException {
		Long user = userManager.getUserId();
		if (user == null) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return null;
		}
		return profileManager.getAccountProfileById(user.toString());
	}

	@ApiOperation(value="Get account profiles of specified users")
	@RequestMapping(method = RequestMethod.GET, value = "/accountprofile/profiles")
	public @ResponseBody AccountProfiles findAccountProfiles(HttpServletResponse response,
			@RequestParam List<String> userIds) throws IOException {
		AccountProfiles profiles = new AccountProfiles();
		profiles.setProfiles(profileManager.getAccountProfilesById(userIds));
		return profiles;
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(Exception.class)
	@ResponseBody
	ErrorInfo handleBadRequest(HttpServletRequest req, Exception ex) {
		StackTraceElement ste = ex.getStackTrace()[0];
		return new ErrorInfo(req.getRequestURL().toString(), ex.getClass().getTypeName(), ste.getClassName(),
				ste.getLineNumber());
	}

}
