package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.LoginDTO;
import fun.yeelo.oauth.service.GrokConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/grok")
public class GrokController {
    @Autowired
    private GrokConfigService grokConfigService;

    @GetMapping("/checkUser")
    public HttpResult<String> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        return grokConfigService.checkLinuxDoUser(username,jmc,request);
    }

    @PostMapping("/login")
    public HttpResult<String> login(@RequestBody LoginDTO resetDTO) {
        return grokConfigService.login(resetDTO);

    }
}
