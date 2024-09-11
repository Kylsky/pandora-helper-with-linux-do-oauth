package fun.yeelo.oauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @Value("${linux-do.oauth2.client.registration.redirect-uri}")
    private String apiUrl;

    @GetMapping("/")
    public String index(Model model) {
        // 将 API URL 传递到 Thymeleaf 模板
        model.addAttribute("apiUrl", apiUrl);
        return "index";
    }
}
