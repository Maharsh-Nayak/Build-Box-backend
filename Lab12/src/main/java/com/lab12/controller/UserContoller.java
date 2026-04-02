package com.lab12.controller;

import com.lab12.entity.User;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@SessionAttributes("user")
public class UserContoller {

    @RequestMapping("/form")
    public String form(Model model) {

        model.addAttribute("user", new User());

        return "form";
    }

    @PostMapping("/saveUser")
    public String saveUser(@Valid @ModelAttribute("user") User user,
                           BindingResult result,
                           Model model) {

        if(result.hasErrors()) {
            return "form";
        }

        model.addAttribute("user", user);

        return "userDetails";
    }

    @RequestMapping("/editUser")
    public String editUser(@ModelAttribute("user") User user) {

        return "form";
    }
}