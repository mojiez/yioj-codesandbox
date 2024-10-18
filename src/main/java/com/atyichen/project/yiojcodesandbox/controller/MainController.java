package com.atyichen.project.yiojcodesandbox.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class MainController {
    @GetMapping("/health")
    public String checkHealth() {
        return "ok";
    }
}
