package fun.yeelo.oauth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {

    @GetMapping("/")
    public String index(Model model) {
        // 将 API URL 传递到 Thymeleaf 模板
        return "index";
    }


    //
    //@GetMapping("/loading")
    //public String index2(Model model, HttpServletRequest request) {
    //    // 将 API URL 传递到 Thymeleaf 模板
    //    model.addAttribute("apiUrl", apiUrl);
    //    return "forward:/";
    //}
    //
    //@GetMapping("/navi")
    //public String navi(Model model) {
    //    // 将 API URL 传递到 Thymeleaf 模板
    //    model.addAttribute("apiUrl", apiUrl);
    //    return "index";
    //}
}
