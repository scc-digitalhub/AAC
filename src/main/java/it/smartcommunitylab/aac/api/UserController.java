package it.smartcommunitylab.aac.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.smartcommunitylab.aac.core.UserManager;

@RestController
@RequestMapping("api/users")
public class UserController {

    @Autowired
    private UserManager userManager;

}
