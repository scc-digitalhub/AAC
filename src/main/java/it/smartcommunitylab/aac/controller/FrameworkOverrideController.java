package it.smartcommunitylab.aac.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import springfox.documentation.annotations.ApiIgnore;

@ApiIgnore
@Controller
public class FrameworkOverrideController {

	@RequestMapping("/oauth/check_token")
	public ResponseEntity<String> disableCheckToken() {
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequestMapping("/oauth/token_key")
	public ResponseEntity<String> disableTokenKey() {
		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}
}
