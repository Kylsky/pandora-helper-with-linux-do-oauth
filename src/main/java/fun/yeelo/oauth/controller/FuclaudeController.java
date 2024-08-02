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
            share.setComment("unassigned");
            shareService.save(share);
            final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            final String jwt = jwtTokenUtil.generateToken(userDetails);
            share.setToken(jwt);
            return HttpResult.error("用户未激活,请联系管理员");
        }
        ShareClaudeConfig claudeShare = claudeConfigService.getByShareId(user.getId());
        if (claudeShare == null || claudeShare.getOauthToken() == null) {
            return HttpResult.error("用户未激活,请联系管理员");
        }
        return HttpResult.success(claudeShare.getOauthToken());
    }

    @PostMapping("/login")
    public HttpEntity<String> login(@RequestBody LoginDTO resetDTO) {
        String username = resetDTO.getUsername();
        String password = resetDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return new ResponseEntity<>("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return new ResponseEntity<>("用户不存在，请重试", HttpStatus.BAD_REQUEST);
        }
        ShareClaudeConfig claudeShare = claudeConfigService.getByShareId(user.getId());
        if (claudeShare==null || !StringUtils.hasText(claudeShare.getOauthToken())) {
            return new ResponseEntity<>("用户未激活", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(password,user.getPassword())){
            return new ResponseEntity<>("密码错误，请重试", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(claudeShare.getOauthToken(), HttpStatus.OK);

    }
}
