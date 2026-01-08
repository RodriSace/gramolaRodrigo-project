package com.example.gramolaRodrigo.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    // Evita el error 404 cuando Selenium entra en /login
    @GetMapping(value = {"/login", "/payment", "/search"})
    public String forward() {
        return "forward:/index.html";
    }
}
