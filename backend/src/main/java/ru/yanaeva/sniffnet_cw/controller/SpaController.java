package ru.yanaeva.sniffnet_cw.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({
        "/",
        "/auth",
        "/dashboard",
        "/experiments",
        "/experiments/{id}",
        "/classification",
        "/history",
        "/profile",
        "/admin/users"
    })
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
