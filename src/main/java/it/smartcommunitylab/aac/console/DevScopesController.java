package it.smartcommunitylab.aac.console;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.controller.BaseScopesController;

@RestController
@Hidden
@RequestMapping("/console/dev")public class DevScopesController extends BaseScopesController {

}