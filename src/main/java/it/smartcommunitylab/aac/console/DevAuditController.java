package it.smartcommunitylab.aac.console;

import io.swagger.v3.oas.annotations.Hidden;
import it.smartcommunitylab.aac.audit.BaseAuditController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Hidden
@RequestMapping("/console/dev")
public class DevAuditController extends BaseAuditController {}
