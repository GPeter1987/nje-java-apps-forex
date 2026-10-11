package com.forex.beadando;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class HomeController {
    private final String team;

    public HomeController(@Value("${app.team:Projektcsapat}") String team) {
        this.team = team;
    }

    @ModelAttribute("team")
    public String team() {
        return team;
    }

    @GetMapping("/")
    public String home() {
        return "index";
    }
}
