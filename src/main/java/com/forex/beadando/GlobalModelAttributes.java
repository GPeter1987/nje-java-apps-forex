package com.forex.beadando;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

// Minden controller modelljébe bekerül a "team" attribútum, ezt a közös lábléc (fragments.html) jeleníti meg.
@ControllerAdvice
public class GlobalModelAttributes {
    private final String team;

    public GlobalModelAttributes(@Value("${app.team:Projektcsapat}") String team) {
        this.team = team;
    }

    @ModelAttribute("team")
    public String team() {
        return team;
    }
}
