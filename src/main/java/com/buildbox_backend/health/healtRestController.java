package com.buildbox_backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class healtRestController {
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
