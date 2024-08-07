package fun.yeelo.oauth.controller;

import fun.yeelo.oauth.config.HttpResult;
import fun.yeelo.oauth.domain.*;
import fun.yeelo.oauth.service.AccountService;
import fun.yeelo.oauth.service.GptConfigService;
import fun.yeelo.oauth.service.ShareService;
import fun.yeelo.oauth.utils.JwtTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

@RestController
@RequestMapping("/user")
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ShareService shareService;
    @Autowired
    private AccountService accountService;
    @Value("${admin-name:admin}")
    private String adminName;

    @PostConstruct
    public void initiate() {
        List<Share> list = shareService.list();
        if (CollectionUtils.isEmpty(list)){
            Share user = new Share();
            user.setId(1);
            user.setUniqueName(adminName);
            user.setParentId(1);
            user.setPassword(passwordEncoder.encode("123456"));
            user.setComment("admin");
            shareService.save(user);
            log.info("初始化成功");
        }
    }

    @PostMapping("/login")
    public HttpResult<String> panelLogin(@RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return HttpResult.error("用户名或密码不能为空", HttpStatus.BAD_REQUEST);
        }
        Share user = shareService.getByUserName(username);
        if (user == null) {
            return HttpResult.error("用户不存在，请重试");
        }
        //ShareGptConfig gptConfig = gptConfigService.getByShareId(user.getId());
        //if (gptConfig == null || !StringUtils.hasText(gptConfig.getShareToken())) {
        //    return HttpResult.error("用户未激活");
        //}
        if (!passwordEncoder.matches(password,user.getPassword())){
            return HttpResult.error("密码错误,请重试");
        }
        final UserDetails userDetails = userDetailsService.loadUserByUsername(loginDTO.getUsername());

        final String jwt = jwtTokenUtil.generateToken(userDetails);

        return HttpResult.success(jwt);
    }

    @GetMapping("/checkToken")
    public HttpResult<Boolean> checkToken(HttpServletRequest request){
        final String authorizationHeader = request.getHeader("Authorization");

        String username;
        String jwt;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            username = jwtTokenUtil.extractUsername(jwt);
        }else {
            return HttpResult.success(false,"用户未登录");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() != null) {
            return HttpResult.success(true);
        } else {
            return HttpResult.success(false,"登录状态已失效，请重新登录");
        }
    }

}
