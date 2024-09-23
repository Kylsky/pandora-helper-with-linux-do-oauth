package fun.yeelo.oauth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.ClaudeConfigService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/fuclaude")
public class FuclaudeController {
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private static final Logger log = LoggerFactory.getLogger(FuclaudeController.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ClaudeConfigService claudeConfigService;

    @Value("${linux-do.oaifree.auth-api}")
    private String authUrl;

    private final static String DEFAULT_AUTH_URL = "https://new.oaifree.com";

    @Value("${linux-do.oaifree.token-api}")
    private String tokenUrl;

    @Autowired
    private ShareService shareService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${linux-do.fuclaude}")
    private String fuclaudeUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/checkUser")
    public HttpResult<String> checkLinuxDoUser(@RequestParam String username, @RequestParam String jmc, HttpServletRequest request) {
        String jmcFromSession = request.getSession().getAttribute("jmc") == null ? "" : request.getSession().getAttribute("jmc").toString();
        if (!StringUtils.hasText(jmc) || !jmc.equals(jmcFromSession)) {
            return HttpResult.error("请遵守登录规范！");
        }
        Share user = shareService.getByUserName(username);
        if (Objects.isNull(user)) {
            // 新建默认share
            ShareVO share = new ShareVO();
            share.setUniqueName(username);
            share.setIsShared(false);
            share.setPassword(passwordEncoder.encode("123456"));
            share.setComment("");
            shareService.save(share);
            return HttpResult.error("用户未激活,请联系管理员");
        }
        ShareClaudeConfig claudeShare = claudeConfigService.getByShareId(user.getId());
        if (claudeShare == null) {
            return HttpResult.error("权限未激活,请联系管理员");
        }
        Account account = accountService.getById(claudeShare.getAccountId());
        String token = claudeConfigService.generateAutoToken(account,user,null);
        return HttpResult.success(token);
    }

    @PostMapping("/login")
    public HttpResult<String> login(@RequestBody LoginDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空");
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        ShareClaudeConfig claudeShare = claudeConfigService.getByShareId(user.getId());
        if (claudeShare==null) {
            return HttpResult.error("当前用户未激活Claude");
        }
        Account account = accountService.getById(claudeShare.getAccountId());
        String token = claudeConfigService.generateAutoToken(account,user,null);
        if (token==null) {
            return HttpResult.error("生成OAUTH_TOKEN异常，请联系管理员");
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return HttpResult.error("密码错误，请重试");
        }
        return HttpResult.success(token);

    }
}
