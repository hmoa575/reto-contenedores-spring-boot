package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index() {
        return "Aplicación Spring Boot funcionando en un contenedor Docker";
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
