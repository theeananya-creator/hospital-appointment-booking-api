package org.hospital.appointment.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendController {

    @GetMapping({"/login", "/register", "/doctors", "/doctors/{id}",
            "/appointments", "/profile", "/doctor", "/admin"})
    public String forwardToFrontend() {
        return "forward:/index.html";
    }
}
