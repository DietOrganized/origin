package com.example.calorie.controller;

import com.example.calorie.context.UserContext;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 页面控制器（JSP 视图）。
 * 接口前缀是 /api/** 的走 REST，其它路径交给这里返回 JSP 页面。
 */
@Hidden
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "redirect:/jsp/overview";
    }

    @GetMapping({"/jsp", "/jsp/"})
    public String jspHome() {
        return "redirect:/jsp/overview";
    }

    @GetMapping("/jsp/overview")
    public String overview() { return "overview"; }

    @GetMapping("/jsp/meals")
    public String meals() { return "meals"; }

    @GetMapping("/jsp/stats")
    public String stats() { return "stats"; }

    @GetMapping("/jsp/recipes")
    public String recipes() { return "recipes"; }

    @GetMapping("/jsp/advice")
    public String advice() { return "advice"; }

    @GetMapping("/jsp/profile")
    public String profile() { return "profile"; }

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/jsp/hello")
    public String hello(Model model) {
        model.addAttribute("time", LocalDateTime.now().format(FORMAT));
        model.addAttribute("username", Optional.ofNullable(UserContext.get())
                .map(UserContext.LoginUser::username)
                .orElse("未登录"));
        return "hello";
    }
}
